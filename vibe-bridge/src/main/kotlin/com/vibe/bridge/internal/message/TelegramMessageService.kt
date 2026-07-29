package com.vibe.bridge.internal.message

import com.vibe.bridge.api.IMessageService
import com.vibe.bridge.api.INotificationService
import com.vibe.bridge.internal.VibeBridgeConstants
import com.vibe.bridge.internal.notification.TelegramNotificationService
import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import com.vibe.bridge.mapper.MessageMapper
import com.vibe.bridge.model.VibeHistoryCursor
import com.vibe.bridge.model.VibeMessage
import com.vibe.bridge.model.VibeMessagePage
import com.vibe.common.logging.VibeLogger
import com.vibe.common.validation.ValidationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig
import org.telegram.messenger.MessageObject
import org.telegram.tgnet.ConnectionsManager

/**
 * Read-only implementation of [IMessageService] backed by Telegram core.
 *
 * Design notes (PHASE 13 — Message History Integration):
 *  - Uses ONLY read operations on [MessagesController] / [NotificationCenter].
 *  - Triggers a local history load via [MessagesController.loadMessages] with `fromCache = true`,
 *    which resolves to a local SQLite read ([MessagesStorage]). Because the result goes through
 *    the cache path, [MessagesController.processLoadedMessages] does NOT write back to the database.
 *  - Listens exclusively for [NotificationCenter.messagesDidLoad] filtered by a private [ConnectionsManager]
 *    class guid, so it never intercepts ChatActivity's own loads.
 *  - Emits exactly one [VibeMessagePage] per request (cursor-based paging); the caller feeds
 *    [VibeMessagePage.nextCursor] back to walk older history. Memory stays bounded: only one page
 *    (<= [IMessageService.MAX_HISTORY_PAGE_SIZE]) is materialised at a time.
 */
internal class TelegramMessageService(
    private val messageMapper: MessageMapper,
    private val notificationService: INotificationService
) : IMessageService {

    override fun getMessageHistory(
        chatId: Long,
        cursor: VibeHistoryCursor?,
        limit: Int
    ): Flow<VibeMessagePage> = callbackFlow {
        val scope = this
        val account = UserConfig.selectedAccount
        val controller = MessagesController.getInstance(account)
        val nc = NotificationCenter.getInstance(account)
        val classGuid = ConnectionsManager.generateClassGuid()

        val safeLimit = limit.coerceIn(1, IMessageService.MAX_HISTORY_PAGE_SIZE)
        val maxId = (cursor?.maxId ?: 0L).toInt()
        val offsetDate = (cursor?.offsetDate ?: 0L).toInt()

        var triedNetwork = false

        // Safety timeout for database/network load
        launch {
            kotlinx.coroutines.delay(15000L) // Increased for network fallback
            if (!scope.isClosedForSend) {
                VibeLogger.w("TelegramMessageService", "getMessageHistory: timeout (chatId=$chatId)")
                scope.close()
            }
        }

        val delegate = object : NotificationCenter.NotificationCenterDelegate {
            override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                if (id == NotificationCenter.loadingMessagesFailed) {
                    val eventClassGuid = args[0] as? Int ?: return
                    if (eventClassGuid != classGuid) return
                    VibeLogger.e("TelegramMessageService", "loadingMessagesFailed for chatId=$chatId")
                    scope.close()
                    return
                }

                if (id != NotificationCenter.messagesDidLoad) return

                if (args.size < 11) {
                    VibeLogger.w("TelegramMessageService", "messagesDidLoad: invalid args size (${args.size})")
                    return
                }

                val eventDialogId = args[0] as? Long ?: return
                if (eventDialogId != chatId) return

                val eventClassGuid = args[10] as? Int ?: return
                if (eventClassGuid != classGuid) return

                val objects = args[2] as? ArrayList<*> ?: return
                val isCache = args[3] as? Boolean ?: true
                val isEnd = args[9] as? Boolean ?: false

                // Fallback to network if cache returned empty
                if (objects.isEmpty() && isCache && !triedNetwork) {
                    triedNetwork = true
                    VibeLogger.d("TelegramMessageService", "Cache empty for chatId=$chatId, trying network...")
                    controller.loadMessages(
                        chatId, chatId, false, safeLimit, maxId, offsetDate,
                        false, // fromCache = false
                        0, classGuid, MessagesController.LOAD_BACKWARD, 0,
                        VibeBridgeConstants.TELEGRAM_MODE_DEFAULT, 0, 0, 0, false
                    )
                    return
                }

                // Offload mapping off the (UI) notification thread.
                scope.launch(Dispatchers.IO) {
                    // Deduplicate by message ID
                    val uniqueObjects = objects.filterIsInstance<MessageObject>().distinctBy { it.id }
                    
                    val messages = ArrayList<VibeMessage>(uniqueObjects.size)
                    for (obj in uniqueObjects) {
                        messages.add(messageMapper.mapMessage(obj))
                    }

                    val nextCursor = if (isEnd || messages.isEmpty()) {
                        null
                    } else {
                        val oldestId = messages.minOf { it.id }
                        VibeHistoryCursor(
                            chatId = chatId,
                            maxId = oldestId,
                            offsetDate = 0,
                            isEnd = false
                        )
                    }

                    val page = VibeMessagePage(
                        chatId = chatId,
                        messages = messages,
                        nextCursor = nextCursor,
                        isEnd = isEnd,
                        fromCache = isCache
                    )
                    scope.trySend(page)
                    scope.close()
                }
            }
        }

        org.telegram.messenger.AndroidUtilities.runOnUIThread {
            nc.addObserver(delegate, NotificationCenter.messagesDidLoad)
            nc.addObserver(delegate, NotificationCenter.loadingMessagesFailed)

            controller.loadMessages(
                chatId,
                chatId,                      // mergeDialogId (normal chats)
                false,                       // loadInfo
                safeLimit,
                maxId,
                offsetDate,
                true,                        // fromCache -> local DB only first
                0,                           // midDate
                classGuid,
                MessagesController.LOAD_BACKWARD,
                0,                           // last_message_id
                VibeBridgeConstants.TELEGRAM_MODE_DEFAULT,
                0,                           // threadMessageId
                0,                           // replyFirstUnread
                0,                           // loadIndex
                false                        // isTopic
            )
        }

        awaitClose {
            org.telegram.messenger.AndroidUtilities.runOnUIThread {
                nc.removeObserver(delegate, NotificationCenter.messagesDidLoad)
                nc.removeObserver(delegate, NotificationCenter.loadingMessagesFailed)
            }
        }
    }

    override fun getRecentMessages(chatId: Long, limit: Int): Flow<List<VibeMessage>> {
        return getMessageHistory(chatId, cursor = null, limit = limit).map { it.messages }
    }

    override suspend fun sendTextMessage(
        chatId: Long,
        text: String,
        replyToMsgId: Long?
    ): Result<VibeMessage> {
        // Input validation
        ValidationUtils.requireValid(ValidationUtils.isValidChatId(chatId), "Invalid chat ID: $chatId")
        ValidationUtils.requireValid(ValidationUtils.isValidMessageText(text), "Invalid message text")
        
        if (replyToMsgId != null) {
            ValidationUtils.requireValid(ValidationUtils.isValidMessageId(replyToMsgId), "Invalid reply message ID: $replyToMsgId")
        }
        
        val account = UserConfig.selectedAccount
        return try {
            val mo = TelegramCoreAdapter.sendTextMessage(account, chatId, text, replyToMsgId)
            if (mo != null) {
                Result.success(messageMapper.mapMessage(mo))
            } else {
                if (replyToMsgId != null) {
                    Result.failure(Exception("Reply target message not found: $replyToMsgId"))
                } else {
                    Result.failure(Exception("Failed to initiate message sending"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPhoto(
        chatId: Long,
        path: String,
        caption: String?,
        replyToMsgId: Long?
    ): Result<Unit> {
        val account = UserConfig.selectedAccount
        return try {
            TelegramCoreAdapter.prepareAndSendPhoto(account, chatId, path, caption, replyToMsgId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendVideo(
        chatId: Long,
        path: String,
        caption: String?,
        replyToMsgId: Long?
    ): Result<Unit> {
        val account = UserConfig.selectedAccount
        return try {
            TelegramCoreAdapter.prepareAndSendVideo(account, chatId, path, caption, replyToMsgId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendDocument(
        chatId: Long,
        path: String,
        caption: String?,
        replyToMsgId: Long?
    ): Result<Unit> {
        val account = UserConfig.selectedAccount
        return try {
            TelegramCoreAdapter.prepareAndSendDocument(account, chatId, path, caption, replyToMsgId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun forwardMessages(
        fromChatId: Long,
        messageIds: List<Long>,
        toChatId: Long
    ): Result<List<VibeMessage>> {
        val account = UserConfig.selectedAccount
        return try {
            val messages = TelegramCoreAdapter.forwardMessages(account, fromChatId, messageIds, toChatId)
            if (messages != null) {
                Result.success(messages.map { messageMapper.mapMessage(it) })
            } else {
                Result.failure(Exception("One or more source messages not found or invalid"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun forwardMessagesAsCopy(
        fromChatId: Long,
        messageIds: List<Long>,
        toChatId: Long
    ): Result<List<VibeMessage>> {
        val account = UserConfig.selectedAccount
        return try {
            val messages = TelegramCoreAdapter.forwardMessagesAsCopy(account, fromChatId, messageIds, toChatId)
            if (messages != null) {
                Result.success(messages.map { messageMapper.mapMessage(it) })
            } else {
                Result.failure(Exception("One or more source messages not found or invalid"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeUploadProgress(chatId: Long, messageId: Long): Flow<Float> {
        return (notificationService as TelegramNotificationService).observeUploadProgress(chatId, messageId)
    }

    override suspend fun cancelSending(chatId: Long, messageId: Long): Result<Unit> {
        val account = UserConfig.selectedAccount
        return try {
            if (TelegramCoreAdapter.cancelSendingMessage(account, messageId.toInt())) {
                (notificationService as TelegramNotificationService).notifyCancelled(account, chatId, messageId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Message not found or not cancellable: $messageId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun editMessage(
        chatId: Long,
        messageId: Long,
        newText: String
    ): Result<VibeMessage> {
        val account = UserConfig.selectedAccount
        return try {
            val mo = TelegramCoreAdapter.editMessage(account, chatId, messageId, newText)
            if (mo != null) {
                Result.success(messageMapper.mapMessage(mo))
            } else {
                Result.failure(Exception("Message not found or not editable: $messageId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessages(
        chatId: Long,
        messageIds: List<Long>,
        revoke: Boolean
    ): Result<Unit> {
        val account = UserConfig.selectedAccount
        return try {
            if (TelegramCoreAdapter.deleteMessages(account, chatId, messageIds, revoke)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete messages: empty list"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
