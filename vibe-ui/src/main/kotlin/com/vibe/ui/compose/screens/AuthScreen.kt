package com.vibe.ui.compose.screens

import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vibe.ui.compose.components.VibeButton
import com.vibe.ui.compose.components.VibeButtonSize
import com.vibe.ui.compose.components.VibeInput
import com.vibe.ui.feature.auth.TelegramLoginManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun detectCountryCode(context: Context): String {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return "+7 "
    val iso = tm.simCountryIso?.uppercase() ?: tm.networkCountryIso?.uppercase() ?: return "+7 "
    return (COUNTRY_DIAL_CODES[iso] ?: "+7") + " "
}

private val COUNTRY_DIAL_CODES = mapOf(
    "RU" to "+7", "KZ" to "+7", "UA" to "+380", "BY" to "+375",
    "US" to "+1", "CA" to "+1", "GB" to "+44", "DE" to "+49",
    "FR" to "+33", "IT" to "+39", "ES" to "+34", "TR" to "+90",
    "CN" to "+86", "JP" to "+81", "KR" to "+82", "IN" to "+91",
    "BR" to "+55", "AU" to "+61", "PL" to "+48", "CZ" to "+420",
    "IL" to "+972", "SE" to "+46", "NO" to "+47", "FI" to "+358",
    "DK" to "+45", "NL" to "+31", "BE" to "+32", "CH" to "+41",
    "AT" to "+43", "PT" to "+351", "GR" to "+30", "IE" to "+353",
    "NZ" to "+64", "SG" to "+65", "MY" to "+60", "TH" to "+66",
    "VN" to "+84", "PH" to "+63", "ID" to "+62", "PK" to "+92",
    "BD" to "+880", "EG" to "+20", "ZA" to "+27", "NG" to "+234",
    "AZ" to "+994", "AM" to "+374", "GE" to "+995", "KG" to "+996",
    "TJ" to "+992", "TM" to "+993", "UZ" to "+998", "MD" to "+373",
    "LV" to "+371", "LT" to "+370", "EE" to "+372", "MN" to "+976",
)

private enum class AuthStep { PHONE, CODE, PASSWORD, REGISTER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(AuthStep.PHONE) }
    var phoneNumber by remember { mutableStateOf(detectCountryCode(context)) }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var codeSentViaCall by remember { mutableStateOf(false) }
    var resendIn by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(resendIn) {
        if (resendIn > 0) {
            delay(1000)
            resendIn -= 1
        }
    }

    fun requestCode() {
        val phone = phoneNumber.filter { it.isDigit() }
        if (phone.length < 5) {
            error = "Введите корректный номер телефона"
            return
        }
        error = null
        loading = true
        scope.launch {
            val result = TelegramLoginManager.sendCode(phone)
            loading = false
            result.onSuccess { info ->
                step = AuthStep.CODE
                codeSentViaCall = info.viaCall
                resendIn = info.timeoutSeconds
                error = null
            }.onFailure { e ->
                error = e.message ?: "Ошибка сети. Проверьте соединение"
            }
        }
    }

    fun verify() {
        if (code.length < 3) {
            error = "Введите код из SMS"
            return
        }
        error = null
        loading = true
        scope.launch {
            when (val result = TelegramLoginManager.verifyCode(code)) {
                is TelegramLoginManager.VerifyResult.Success -> {
                    loading = false
                    onComplete()
                }
                is TelegramLoginManager.VerifyResult.Failure -> {
                    loading = false
                    error = result.message
                }
                TelegramLoginManager.VerifyResult.SignUpRequired -> {
                    loading = false
                    step = AuthStep.REGISTER
                    error = null
                }
                TelegramLoginManager.VerifyResult.PasswordRequired -> {
                    loading = false
                    step = AuthStep.PASSWORD
                    error = null
                }
            }
        }
    }

    fun submitPassword() {
        error = null
        loading = true
        scope.launch {
            when (val result = TelegramLoginManager.checkPassword(password)) {
                is TelegramLoginManager.VerifyResult.Success -> {
                    loading = false
                    onComplete()
                }
                is TelegramLoginManager.VerifyResult.Failure -> {
                    loading = false
                    error = result.message
                }
                TelegramLoginManager.VerifyResult.SignUpRequired -> {
                    loading = false
                    step = AuthStep.REGISTER
                    error = null
                }
                TelegramLoginManager.VerifyResult.PasswordRequired -> {
                    loading = false
                    error = "Введите облачный пароль"
                }
            }
        }
    }

    fun register() {
        error = null
        loading = true
        scope.launch {
            when (val result = TelegramLoginManager.signUp(firstName, lastName)) {
                is TelegramLoginManager.VerifyResult.Success -> {
                    loading = false
                    onComplete()
                }
                is TelegramLoginManager.VerifyResult.Failure -> {
                    loading = false
                    error = result.message
                }
                TelegramLoginManager.VerifyResult.PasswordRequired -> {
                    loading = false
                    step = AuthStep.PASSWORD
                    error = null
                }
                TelegramLoginManager.VerifyResult.SignUpRequired -> {
                    loading = false
                    error = null
                }
            }
        }
    }

    val title = when (step) {
        AuthStep.PHONE -> "Введите номер телефона"
        AuthStep.CODE -> "Введите код подтверждения"
        AuthStep.PASSWORD -> "Введите облачный пароль"
        AuthStep.REGISTER -> "Создайте аккаунт Telegram"
    }

    val subtitle = when (step) {
        AuthStep.PHONE -> "Мы отправим код подтверждения в Telegram"
        AuthStep.CODE -> if (codeSentViaCall) "Мы позвоним вам с последними цифрами кода" else "Мы отправили код подтверждения в SMS"
        AuthStep.PASSWORD -> "На аккаунте включена двухфакторная авторизация"
        AuthStep.REGISTER -> "Номер не зарегистрирован — укажите имя"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вход в Telegram", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        when (step) {
                            AuthStep.PHONE -> onBack()
                            AuthStep.CODE -> {
                                step = AuthStep.PHONE
                                error = null
                            }
                            AuthStep.PASSWORD, AuthStep.REGISTER -> {
                                step = AuthStep.CODE
                                error = null
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(visible = step == AuthStep.PHONE) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    VibeInput(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it; error = null },
                        label = "Номер телефона",
                        placeholder = "+7 (999) 123-45-67",
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                        error = error,
                        enabled = !loading,
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null,
                                 tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AnimatedVisibility(visible = step == AuthStep.CODE) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    VibeInput(
                        value = code,
                        onValueChange = { code = it.filter { c -> c.isDigit() }.take(6); error = null },
                        label = "Код подтверждения",
                        placeholder = "••••••",
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                        error = error,
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (resendIn > 0) {
                            Text(
                                text = "Отправить код повторно через $resendIn с",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Отправить код повторно",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable(enabled = !loading) { requestCode() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Изменить номер",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(enabled = !loading) {
                                step = AuthStep.PHONE
                                error = null
                            }
                            .padding(4.dp)
                    )
                }
            }

            AnimatedVisibility(visible = step == AuthStep.PASSWORD) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    VibeInput(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = "Облачный пароль",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        error = error,
                        enabled = !loading,
                        isPassword = true,
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null,
                                 tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AnimatedVisibility(visible = step == AuthStep.REGISTER) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    VibeInput(
                        value = firstName,
                        onValueChange = { firstName = it; error = null },
                        label = "Имя",
                        imeAction = ImeAction.Next,
                        error = error,
                        enabled = !loading,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null,
                                 tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    VibeInput(
                        value = lastName,
                        onValueChange = { lastName = it; error = null },
                        label = "Фамилия (необязательно)",
                        imeAction = ImeAction.Done,
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            VibeButton(
                text = when {
                    loading && step == AuthStep.CODE -> "Проверка кода..."
                    loading && step == AuthStep.PASSWORD -> "Проверка пароля..."
                    loading && step == AuthStep.REGISTER -> "Регистрация..."
                    loading -> "Отправка..."
                    step == AuthStep.CODE -> "Подтвердить"
                    step == AuthStep.PASSWORD -> "Войти"
                    step == AuthStep.REGISTER -> "Зарегистрироваться"
                    else -> "Получить код"
                },
                onClick = {
                    when (step) {
                        AuthStep.PHONE -> requestCode()
                        AuthStep.CODE -> verify()
                        AuthStep.PASSWORD -> submitPassword()
                        AuthStep.REGISTER -> register()
                    }
                },
                fullWidth = true,
                size = VibeButtonSize.LARGE,
                enabled = !loading
            )
        }
    }
}
