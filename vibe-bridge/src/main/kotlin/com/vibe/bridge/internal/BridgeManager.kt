package com.vibe.bridge.internal

import com.vibe.bridge.api.ITelegramGateway
import com.vibe.bridge.gateway.TelegramGateway
import com.vibe.bridge.internal.account.TelegramAccountService
import com.vibe.bridge.internal.chat.TelegramChatService
import com.vibe.bridge.internal.contact.TelegramContactService
import com.vibe.bridge.internal.media.TelegramMediaService
import com.vibe.bridge.internal.message.TelegramMessageService
import com.vibe.bridge.internal.notification.TelegramNotificationService
import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import com.vibe.bridge.internal.user.TelegramUserService
import com.vibe.bridge.mapper.ChatMapper
import com.vibe.bridge.mapper.MessageMapper
import com.vibe.bridge.mapper.TelegramMapper
import com.vibe.bridge.mapper.UserMapper
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig

/**
 * Manager for coordinating the Vibe Bridge lifecycle and integration.
 */
class BridgeManager {

    private var _isInitialized = false
    private var _activeAccountId: Int = 0
    private val _errorListeners = mutableListOf<(Throwable) -> Unit>()

    val isInitialized: Boolean
        get() = _isInitialized

    val activeAccountId: Int
        get() = _activeAccountId

    private val messageMapper = MessageMapper()
    private val chatMapper = ChatMapper(messageMapper)
    private val userMapper = UserMapper()
    private val telegramMapper = TelegramMapper()
    
    private val notificationService = TelegramNotificationService(telegramMapper)
    private val accountService = TelegramAccountService(telegramMapper)
    private val chatService = TelegramChatService(chatMapper)
    private val messageService = TelegramMessageService(messageMapper, notificationService)
    private val userService = TelegramUserService()
    private val contactService = TelegramContactService()
    private val mediaService = TelegramMediaService()

    /**
     * The single gateway instance for accessing Telegram data.
     */
    val gateway: ITelegramGateway = TelegramGateway(
        notificationService,
        accountService,
        chatService,
        messageService,
        userService,
        contactService,
        mediaService
    )

    /**
     * Registers a callback that will be invoked on all bridge errors.
     */
    fun onError(listener: (Throwable) -> Unit) {
        _errorListeners.add(listener)
    }

    private fun notifyError(e: Throwable) {
        _errorListeners.forEach { it(e) }
    }

    /**
     * Initializes the bridge and hooks into Telegram Core.
     */
    fun initialize(): Result<Unit> {
        if (_isInitialized) return Result.success(Unit)

        return try {
            _activeAccountId = try {
                UserConfig.selectedAccount
            } catch (_: Throwable) { 0 }

            TelegramCoreAdapter.setMediaRegistry(mediaService)

            org.telegram.messenger.AndroidUtilities.runOnUIThread {
                var i = 0
                while (i < UserConfig.MAX_ACCOUNT_COUNT) {
                    val nc = NotificationCenter.getInstance(i)
                    nc.addObserver(notificationService, NotificationCenter.didReceiveNewMessages)
                    nc.addObserver(notificationService, NotificationCenter.messageReceivedByServer)
                    nc.addObserver(notificationService, NotificationCenter.messageReceivedByServer2)
                    nc.addObserver(notificationService, NotificationCenter.messageSendError)
                    nc.addObserver(notificationService, NotificationCenter.replaceMessagesObjects)
                    nc.addObserver(notificationService, NotificationCenter.messagesDeleted)
                    nc.addObserver(notificationService, NotificationCenter.historyCleared)
                    nc.addObserver(notificationService, NotificationCenter.didUpdateReactions)
                    nc.addObserver(notificationService, NotificationCenter.fileUploadProgressChanged)
                    nc.addObserver(notificationService, NotificationCenter.fileUploadFailed)

                    nc.addObserver(contactService, NotificationCenter.contactsDidLoad)
                    nc.addObserver(userService, NotificationCenter.userInfoDidLoad)
                    nc.addObserver(userService, NotificationCenter.updateInterfaces)
                    nc.addObserver(userService, NotificationCenter.mainUserInfoChanged)

                    nc.addObserver(chatService, NotificationCenter.dialogsNeedReload)
                    nc.addObserver(chatService, NotificationCenter.newDraftReceived)
                    nc.addObserver(chatService, NotificationCenter.notificationsSettingsUpdated)
                    nc.addObserver(chatService, NotificationCenter.updateInterfaces)
                    nc.addObserver(chatService, NotificationCenter.messagesRead)

                    nc.addObserver(mediaService, NotificationCenter.fileLoaded)
                    nc.addObserver(mediaService, NotificationCenter.fileLoadFailed)
                    nc.addObserver(mediaService, NotificationCenter.fileLoadProgressChanged)
                    i++
                }
            }
            _isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            notifyError(e)
            Result.failure(e)
        }
    }

    /**
     * Switches the active account.
     */
    fun switchAccount(accountId: Int): Boolean {
        if (accountId < 0 || accountId >= UserConfig.MAX_ACCOUNT_COUNT) return false
        _activeAccountId = accountId
        return true
    }
    
    /**
     * Clean up observers to prevent memory leaks.
     */
    fun destroy() {
        if (!_isInitialized) return

        TelegramCoreAdapter.setMediaRegistry(null)
        mediaService.cleanup()
        contactService.cleanup()
        chatService.cleanup()
        notificationService.cleanup()

        var i = 0
        while (i < UserConfig.MAX_ACCOUNT_COUNT) {
            val nc = NotificationCenter.getInstance(i)
            nc.removeObserver(notificationService, NotificationCenter.didReceiveNewMessages)
            nc.removeObserver(notificationService, NotificationCenter.messageReceivedByServer)
            nc.removeObserver(notificationService, NotificationCenter.messageReceivedByServer2)
            nc.removeObserver(notificationService, NotificationCenter.messageSendError)
            nc.removeObserver(notificationService, NotificationCenter.replaceMessagesObjects)
            nc.removeObserver(notificationService, NotificationCenter.messagesDeleted)
            nc.removeObserver(notificationService, NotificationCenter.historyCleared)
            nc.removeObserver(notificationService, NotificationCenter.didUpdateReactions)
            nc.removeObserver(notificationService, NotificationCenter.fileUploadProgressChanged)
            nc.removeObserver(notificationService, NotificationCenter.fileUploadFailed)

            nc.removeObserver(chatService, NotificationCenter.dialogsNeedReload)
            nc.removeObserver(chatService, NotificationCenter.newDraftReceived)
            nc.removeObserver(chatService, NotificationCenter.notificationsSettingsUpdated)
            nc.removeObserver(chatService, NotificationCenter.updateInterfaces)
            nc.removeObserver(chatService, NotificationCenter.messagesRead)

            nc.removeObserver(contactService, NotificationCenter.contactsDidLoad)
            nc.removeObserver(userService, NotificationCenter.userInfoDidLoad)
            nc.removeObserver(userService, NotificationCenter.updateInterfaces)
            nc.removeObserver(userService, NotificationCenter.mainUserInfoChanged)

            nc.removeObserver(mediaService, NotificationCenter.fileLoaded)
            nc.removeObserver(mediaService, NotificationCenter.fileLoadFailed)
            nc.removeObserver(mediaService, NotificationCenter.fileLoadProgressChanged)
            i++
        }
        _isInitialized = false
    }
}
