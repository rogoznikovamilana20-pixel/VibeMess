package com.vibe.ui.feature.auth

import android.os.Handler
import android.os.Looper
import com.vibe.common.logging.VibeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.BuildVars
import org.telegram.messenger.ContactsController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import kotlin.coroutines.resume

/**
 * Реальный вход в Telegram через официальный API (auth.sendCode / auth.signIn).
 * Работает на аккаунте 0 (UserConfig.selectedAccount) — том же, что использует мост.
 */
object TelegramLoginManager {

    private const val TAG = "TelegramLogin"
    private const val ACCOUNT = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    private var phoneNumber: String? = null
    private var phoneCodeHash: String? = null

    /** Результат отправки кода. */
    data class SentCodeInfo(
        val timeoutSeconds: Int = 30,
        val viaCall: Boolean = false
    )

    sealed class VerifyResult {
        data class Success(val userId: Long) : VerifyResult()
        data class Failure(val message: String) : VerifyResult()
        object SignUpRequired : VerifyResult()
        object PasswordRequired : VerifyResult()
    }

    fun isSessionActive(): Boolean {
        return runCatching {
            UserConfig.getInstance(ACCOUNT).isClientActivated()
        }.getOrDefault(false)
    }

    /**
     * Отправляет код подтверждения на номер.
     */
    suspend fun sendCode(phone: String): Result<SentCodeInfo> {
        val normalized = phone.filter { it.isDigit() }
        if (normalized.length < 5) {
            return Result.failure(IllegalArgumentException("Введите корректный номер телефона"))
        }
        phoneNumber = normalized

        val settings = TLRPC.TL_codeSettings().apply {
            allow_flashcall = false
            current_number = false
            unknown_number = false
            allow_missed_call = true
            allow_app_hash = true
            allow_firebase = false
            app_sandbox = false
        }

        val req = TLRPC.TL_auth_sendCode().apply {
            api_hash = BuildVars.APP_HASH
            api_id = BuildVars.APP_ID
            phone_number = normalized
            this.settings = settings
        }

        val cm = ConnectionsManager.getInstance(ACCOUNT)
        cm.cleanup(false)

        return request(
            req, cm,
            ConnectionsManager.RequestFlagFailOnServerErrors or
                ConnectionsManager.RequestFlagWithoutLogin or
                ConnectionsManager.RequestFlagTryDifferentDc or
                ConnectionsManager.RequestFlagEnableUnauthorized
        ) { response, error ->
            if (error != null) {
                Result.failure(AuthError(messageFromError(error)))
            } else {
                when (response) {
                    is TLRPC.TL_auth_sentCodeSuccess -> {
                        val auth = response.authorization
                        if (auth is TLRPC.TL_auth_authorization) {
                            finalizeLogin(auth.user)
                            Result.success(SentCodeInfo(30, false))
                        } else {
                            Result.failure(AuthError("Не удалось войти (код уже подтверждён)"))
                        }
                    }
                    is TLRPC.TL_auth_sentCode -> {
                        phoneCodeHash = response.phone_code_hash
                        val viaCall = response.type is TLRPC.TL_auth_sentCodeTypeCall
                        Result.success(SentCodeInfo(response.timeout.coerceIn(20, 300), viaCall))
                    }
                    else -> Result.failure(AuthError("Неожиданный ответ сервера"))
                }
            }
        }
    }

    /**
     * Подтверждает код и завершает вход.
     */
    suspend fun verifyCode(code: String): VerifyResult {
        val phone = phoneNumber ?: return VerifyResult.Failure("Сначала запросите код")
        val hash = phoneCodeHash ?: return VerifyResult.Failure("Код устарел, запросите новый")

        val req = TLRPC.TL_auth_signIn().apply {
            phone_number = phone
            phone_code_hash = hash
            phone_code = code
            flags = 1
        }

        val cm = ConnectionsManager.getInstance(ACCOUNT)
        return request(req, cm, ConnectionsManager.RequestFlagFailOnServerErrors) { response, error ->
            if (error != null) {
                if (error.text.contains("SESSION_PASSWORD_NEEDED")) {
                    VerifyResult.PasswordRequired
                } else if (error.text.contains("PHONE_CODE_INVALID") || error.text.contains("PHONE_CODE_EMPTY")) {
                    VerifyResult.Failure("Неверный код подтверждения")
                } else if (error.text.contains("PHONE_CODE_EXPIRED")) {
                    phoneCodeHash = null
                    VerifyResult.Failure("Код истёк. Запросите новый код")
                } else if (error.text.contains("FLOOD_WAIT")) {
                    VerifyResult.Failure("Слишком много попыток. Подождите и попробуйте снова")
                } else {
                    VerifyResult.Failure(messageFromError(error))
                }
            } else {
                when (response) {
                    is TLRPC.TL_auth_authorization -> {
                        finalizeLogin(response.user)
                        VerifyResult.Success(response.user.id)
                    }
                    is TLRPC.TL_auth_authorizationSignUpRequired ->
                        VerifyResult.SignUpRequired
                    else -> VerifyResult.Failure("Неожиданный ответ сервера")
                }
            }
        }
    }

    /**
     * Вход с облачным паролем (2FA) — auth.checkPassword с SRP.
     */
    suspend fun checkPassword(password: String): VerifyResult {
        if (password.isBlank()) return VerifyResult.Failure("Введите облачный пароль")

        val cm = ConnectionsManager.getInstance(ACCOUNT)
        val pwd = request(
            org.telegram.tgnet.tl.TL_account.getPassword(),
            cm,
            ConnectionsManager.RequestFlagWithoutLogin
        ) { response, error ->
            if (error != null) null else response as? org.telegram.tgnet.tl.TL_account.Password
        } ?: return VerifyResult.Failure("Не удалось получить параметры пароля")

        if (!pwd.has_password) return VerifyResult.Failure("Облачный пароль не установлен")

        val algo = pwd.current_algo
        if (algo !is TLRPC.TL_passwordKdfAlgoSHA256SHA256PBKDF2HMACSHA512iter100000SHA256ModPow) {
            return VerifyResult.Failure("Не поддерживаемый алгоритм пароля")
        }

        val input = withContext(Dispatchers.Default) {
            val x = org.telegram.messenger.SRPHelper.getX(
                org.telegram.messenger.AndroidUtilities.getStringBytes(password),
                algo
            )
            org.telegram.messenger.SRPHelper.startCheck(x, pwd.srp_id, pwd.srp_B, algo)
        } ?: return VerifyResult.Failure("Не удалось обработать пароль")

        val req = TLRPC.TL_auth_checkPassword().apply { this.password = input }
        return request(
            req, cm,
            ConnectionsManager.RequestFlagFailOnServerErrors or
                ConnectionsManager.RequestFlagWithoutLogin
        ) { response, error ->
            if (error != null) {
                when {
                    error.text.contains("PASSWORD_HASH_INVALID") -> VerifyResult.Failure("Неверный облачный пароль")
                    error.text.contains("SRP_ID_INVALID") -> VerifyResult.Failure("Пароль устарел, попробуйте ещё раз")
                    error.text.contains("FLOOD_WAIT") -> VerifyResult.Failure("Слишком много попыток. Подождите")
                    else -> VerifyResult.Failure(messageFromError(error))
                }
            } else {
                when (response) {
                    is TLRPC.TL_auth_authorization -> {
                        finalizeLogin(response.user)
                        VerifyResult.Success(response.user.id)
                    }
                    else -> VerifyResult.Failure("Неожиданный ответ сервера")
                }
            }
        }
    }

    /**
     * Регистрация нового аккаунта (когда номер не зарегистрирован) — auth.signUp.
     */
    suspend fun signUp(firstName: String, lastName: String): VerifyResult {
        if (firstName.isBlank()) return VerifyResult.Failure("Введите имя")
        val phone = phoneNumber ?: return VerifyResult.Failure("Сначала запросите код")
        val hash = phoneCodeHash ?: return VerifyResult.Failure("Код устарел, запросите новый")

        val req = TLRPC.TL_auth_signUp().apply {
            phone_number = phone
            phone_code_hash = hash
            first_name = firstName.trim()
            last_name = lastName.trim()
        }

        val cm = ConnectionsManager.getInstance(ACCOUNT)
        return request(req, cm, ConnectionsManager.RequestFlagFailOnServerErrors) { response, error ->
            if (error != null) {
                when {
                    error.text.contains("PHONE_CODE_EMPTY") || error.text.contains("PHONE_CODE_INVALID") ->
                        VerifyResult.Failure("Неверный код подтверждения")
                    error.text.contains("PHONE_CODE_EXPIRED") -> {
                        phoneCodeHash = null
                        VerifyResult.Failure("Код истёк. Запросите новый код")
                    }
                    error.text.contains("FIRSTNAME_INVALID") -> VerifyResult.Failure("Некорректное имя")
                    error.text.contains("LASTNAME_INVALID") -> VerifyResult.Failure("Некорректная фамилия")
                    else -> VerifyResult.Failure(messageFromError(error))
                }
            } else {
                when (response) {
                    is TLRPC.TL_auth_authorization -> {
                        finalizeLogin(response.user)
                        VerifyResult.Success(response.user.id)
                    }
                    else -> VerifyResult.Failure("Неожиданный ответ сервера")
                }
            }
        }
    }

    /**
     * Отправляет код повторно.
     */
    suspend fun resendCode(phone: String): Result<SentCodeInfo> {
        return sendCode(phone)
    }

    /**
     * Завершение логина — по аналогии с LoginActivity.onAuthSuccess:
     * сохраняет сессию, пользователя и запускает загрузку диалогов.
     */
    private fun finalizeLogin(user: TLRPC.User) {
        mainHandler.post {
            try {
                val controller = MessagesController.getInstance(ACCOUNT)
                controller.cleanup()
                ConnectionsManager.getInstance(ACCOUNT).setUserId(user.id)
                val userConfig = UserConfig.getInstance(ACCOUNT)
                userConfig.clearConfig()
                controller.cleanup()
                userConfig.syncContacts = true
                userConfig.setCurrentUser(user)
                userConfig.saveConfig(true)

                MessagesStorage.getInstance(ACCOUNT).cleanup(true)
                val users = ArrayList<TLRPC.User>().apply { add(user) }
                MessagesStorage.getInstance(ACCOUNT).putUsersAndChats(users, null, true, true)
                controller.putUser(user, false)

                ContactsController.getInstance(ACCOUNT).checkAppAccount()
                controller.checkPromoInfo(true)
                ConnectionsManager.getInstance(ACCOUNT).updateDcSettings()
                controller.loadAppConfig()
                controller.loadWebBrowserConfig()
                controller.checkPeerColors(false)

                controller.loadDialogs(0, 0, 100, false)
                VibeLogger.d(TAG, "Login finished for user ${user.id}")
            } catch (e: Throwable) {
                VibeLogger.e(TAG, "finalizeLogin failed", e)
            }
        }
    }

    private fun messageFromError(error: TLRPC.TL_error): String {
        val text = error.text ?: return "Ошибка сети. Проверьте соединение"
        return when {
            text.contains("PHONE_NUMBER_INVALID") -> "Неверный номер телефона"
            text.contains("PHONE_NUMBER_BANNED") -> "Этот номер заблокирован"
            text.contains("PHONE_NUMBER_FLOOD") || text.contains("PHONE_PASSWORD_FLOOD") ->
                "Слишком много запросов с этого номера. Попробуйте позже"
            text.contains("FLOOD_WAIT") -> "Подождите пару минут и повторите попытку"
            text.contains("PHONE_CODE_EMPTY") -> "Введите код из SMS"
            text.contains("PHONE_CODE_INVALID") -> "Неверный код подтверждения"
            text.contains("PHONE_CODE_EXPIRED") -> "Код истёк. Запросите новый код"
            text.contains("SESSION_PASSWORD_NEEDED") -> "Требуется облачный пароль (2FA)"
            text.contains("PHONE_CODE_HASH_EMPTY") -> "Код устарел, запросите новый"
            text == "PHONE_NUMBER_UNOCCUPIED" -> "Номер не зарегистрирован в Telegram"
            else -> text
        }
    }

    private suspend fun <T> request(
        req: TLObject,
        cm: ConnectionsManager,
        flags: Int,
        mapper: (TLObject?, TLRPC.TL_error?) -> T
    ): T {
        return suspendCancellableCoroutine { cont ->
            val reqId = cm.sendRequest(
                req,
                { response, error ->
                    mainHandler.post {
                        if (cont.isActive) {
                            cont.resume(mapper(response, error))
                        }
                    }
                },
                flags
            )
            cont.invokeOnCancellation {
                runCatching { cm.cancelRequest(reqId, false) }
            }
        }
    }

    class AuthError(message: String) : Exception(message)
}
