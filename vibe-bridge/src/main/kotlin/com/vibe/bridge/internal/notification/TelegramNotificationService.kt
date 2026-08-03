package com.vibe.bridge.internal.notification

import com.vibe.bridge.api.INotificationService
import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import com.vibe.bridge.mapper.TelegramMapper
import com.vibe.bridge.model.MessageDeletion
import com.vibe.bridge.model.VibeChat
import com.vibe.bridge.model.VibeDeliveryStatus
import com.vibe.bridge.model.VibeMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.telegram.messenger.MessageObject
import org.telegram.messenger.NotificationCenter
import com.vibe.common.logging.VibeLogger
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of [INotificationService] that listens to Telegram [NotificationCenter] events.
 */
internal class TelegramNotificationService(
    private val mapper: TelegramMapper
) : INotificationService, NotificationCenter.NotificationCenterDelegate {

    private data class MediaCorrelationKey(
        val accountIndex: Int,
        val chatId: Long,
        val normalizedPath: String
    )

    private data class BufferedProgressKey(
        val accountIndex: Int,
        val normalizedPath: String
    )

    internal data class UploadProgress(
        val accountIndex: Int,
        val chatId: Long,
        val messageId: Long,
        val progress: Float
    )

    private val mediaRegistry = ConcurrentHashMap<MediaCorrelationKey, MutableSet<Long>>()
    private val bufferedProgress = ConcurrentHashMap<BufferedProgressKey, Float>()
    private val mediaRegistryTimestamps = ConcurrentHashMap<MediaCorrelationKey, Long>()

    init {
        // TTL cleanup for stale media registry entries (5 minutes)
        Thread {
            while (true) {
                Thread.sleep(60_000)
                val now = System.currentTimeMillis()
                mediaRegistryTimestamps.entries.forEach { (key, ts) ->
                    if (now - ts > 300_000) {
                        mediaRegistry.remove(key)
                        mediaRegistryTimestamps.remove(key)
                    }
                }
            }
        }.apply { isDaemon = true }.start()
    }

    private val _newMessages = MutableSharedFlow<List<VibeMessage>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _uploadProgress = MutableSharedFlow<UploadProgress>(
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val recentIds = Collections.synchronizedMap(object : LinkedHashMap<Triple<Int, Long, Long>, Boolean>(1024, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Triple<Int, Long, Long>, Boolean>?): Boolean {
            return size > 1000
        }
    })

    private val _messageEdits = MutableSharedFlow<VibeMessage>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _messageDeletions = MutableSharedFlow<MessageDeletion>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _historyCleared = MutableSharedFlow<Long>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _chatUpdates = MutableSharedFlow<VibeChat>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun observeNewMessages(): Flow<List<VibeMessage>> = _newMessages.asSharedFlow()
    override fun observeMessageEdits(): Flow<VibeMessage> = _messageEdits.asSharedFlow()
    override fun observeMessageDeletions(): Flow<MessageDeletion> = _messageDeletions.asSharedFlow()
    override fun observeHistoryCleared(): Flow<Long> = _historyCleared.asSharedFlow()
    override fun observeChatUpdates(): Flow<VibeChat> = _chatUpdates.asSharedFlow()

    fun observeUploadProgress(chatId: Long, messageId: Long): Flow<Float> {
        return _uploadProgress.asSharedFlow()
            .filter { it.chatId == chatId && it.messageId == messageId }
            .map { it.progress }
            .distinctUntilChanged()
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        try {
            handleNotification(id, account, args)
        } catch (e: Throwable) {
            VibeLogger.e("TelegramNotificationService", "didReceivedNotification failed (id=$id)", e)
        }
    }

    private fun handleNotification(id: Int, account: Int, args: Array<out Any?>) {
        when (id) {
            NotificationCenter.didReceiveNewMessages -> {
                if (args.size < 2) {
                    VibeLogger.w("TelegramNotificationService", "didReceiveNewMessages: invalid args size (${args.size})")
                    return
                }
                val dialogId = args[0] as? Long ?: return
                val messages = args[1] as? ArrayList<*> ?: return
                
                val batch = ArrayList<VibeMessage>()
                for (obj in messages) {
                    if (obj is MessageObject) {
                        val messageId = obj.id.toLong()
                        val key = Triple(account, dialogId, messageId)
                        
                        if (!recentIds.containsKey(key)) {
                            recentIds[key] = true
                            val vibeMessage = mapper.mapMessage(obj)
                            batch.add(vibeMessage)
                            
                            // Register media for progress tracking if it's our outgoing message
                            if (obj.isOut) {
                                val path = TelegramCoreAdapter.getMessageAttachPath(obj)
                                val normalized = normalizePath(path)
                                if (normalized != null) {
                                    val registryKey = MediaCorrelationKey(account, dialogId, normalized)
                                    val bindings = mediaRegistry.getOrPut(registryKey) { 
                                        Collections.newSetFromMap(ConcurrentHashMap<Long, Boolean>())
                                    }
                                    bindings.add(messageId)
                                    mediaRegistryTimestamps[registryKey] = System.currentTimeMillis()

                                    // Check if we have buffered progress for this path
                                    val bufKey = BufferedProgressKey(account, normalized)
                                    bufferedProgress[bufKey]?.let { progress ->
                                        _uploadProgress.tryEmit(UploadProgress(account, dialogId, messageId, progress))
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (batch.isNotEmpty()) {
                    _newMessages.tryEmit(batch)
                }
            }
            NotificationCenter.replaceMessagesObjects -> {
                if (args.size < 2) return
                val messages = args[1] as? ArrayList<*> ?: return
                for (obj in messages) {
                    if (obj is MessageObject) {
                        val vibeMessage = mapper.mapMessage(obj)
                        _messageEdits.tryEmit(vibeMessage)
                    }
                }
            }
            NotificationCenter.messagesDeleted -> {
                if (args.size < 2) return
                val ids = args[0] as? ArrayList<*> ?: return
                val channelId = args[1] as? Long ?: 0L
                val messageIds = ids.mapNotNull { (it as? Int)?.toLong() }
                
                if (messageIds.isNotEmpty()) {
                    var resolvedChatId: Long? = null
                    if (channelId != 0L) {
                        resolvedChatId = -channelId
                    } else {
                        // Try lookup from memory to resolve chatId for private chats/groups
                        val firstId = messageIds[0].toInt()
                        val msgObj = TelegramCoreAdapter.getMessageById(firstId, account)
                        if (msgObj != null) {
                            resolvedChatId = msgObj.dialogId
                        }
                    }
                    _messageDeletions.tryEmit(MessageDeletion(account, resolvedChatId, messageIds))
                }
            }
            NotificationCenter.historyCleared -> {
                if (args.isEmpty()) return
                val dialogId = args[0] as? Long ?: return
                _historyCleared.tryEmit(dialogId)
            }
            NotificationCenter.didUpdateReactions -> {
                if (args.size < 2) return
                val messages = args[1] as? ArrayList<*> ?: return
                for (obj in messages) {
                    if (obj is MessageObject) {
                        val vibeMessage = mapper.mapMessage(obj)
                        _messageEdits.tryEmit(vibeMessage)
                    }
                }
            }
            NotificationCenter.messageReceivedByServer, NotificationCenter.messageReceivedByServer2 -> {
                if (args.size < 3) return
                val oldId = args[0] as? Int ?: return
                val newId = args[1] as? Int ?: return
                val msgObj = args[2] as? MessageObject ?: return
                
                VibeLogger.d("TelegramNotificationService", "messageReceivedByServer: $oldId -> $newId")
                
                // Unregister from media registry as the local lifecycle is finished
                unregisterMedia(account, oldId.toLong())
                
                // Map the updated message object.
                // The mapper will set status=SENT because it's out and send_state=0.
                val vibeMessage = mapper.mapMessage(msgObj).copy(
                    id = newId.toLong(),
                    localId = oldId.toLong(),
                    deliveryStatus = VibeDeliveryStatus.SENT
                )
                _messageEdits.tryEmit(vibeMessage)
            }
            NotificationCenter.messageSendError -> {
                val msgId = args[0] as? Int ?: return
                VibeLogger.e("TelegramNotificationService", "messageSendError for msgId=$msgId")
                
                unregisterMedia(account, msgId.toLong())

                val msgObj = TelegramCoreAdapter.getMessageById(msgId, account)
                if (msgObj != null) {
                    val vibeMessage = mapper.mapMessage(msgObj).copy(
                        deliveryStatus = VibeDeliveryStatus.ERROR
                    )
                    _messageEdits.tryEmit(vibeMessage)
                } else {
                    VibeLogger.w("TelegramNotificationService", "messageSendError: MessageObject not found for id $msgId")
                }
            }
            NotificationCenter.fileUploadProgressChanged -> {
                val location = args[0] as? String ?: return
                val uploadedSize = args[1] as? Long ?: 0L
                val totalSize = args[2] as? Long ?: 0L
                
                if (totalSize <= 0L) return
                val progress = (uploadedSize.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
                
                val normalized = normalizePath(location) ?: return
                val bufKey = BufferedProgressKey(account, normalized)
                bufferedProgress[bufKey] = progress
                
                mediaRegistry.forEach { (key, messageIds) ->
                    if (key.accountIndex == account && key.normalizedPath == normalized) {
                        messageIds.forEach { msgId ->
                            _uploadProgress.tryEmit(UploadProgress(account, key.chatId, msgId, progress))
                        }
                    }
                }
            }
            NotificationCenter.fileUploadFailed -> {
                val location = args[0] as? String ?: return
                val normalized = normalizePath(location) ?: return
                
                bufferedProgress.remove(BufferedProgressKey(account, normalized))
                
                val keysToCleanup = mutableListOf<MediaCorrelationKey>()
                mediaRegistry.forEach { (key, messageIds) ->
                    if (key.accountIndex == account && key.normalizedPath == normalized) {
                        messageIds.forEach { msgId ->
                            val msgObj = TelegramCoreAdapter.getMessageById(msgId.toInt(), account)
                            if (msgObj != null) {
                                val vibeMessage = mapper.mapMessage(msgObj).copy(
                                    deliveryStatus = VibeDeliveryStatus.ERROR
                                )
                                _messageEdits.tryEmit(vibeMessage)
                            }
                        }
                        keysToCleanup.add(key)
                    }
                }
                keysToCleanup.forEach { mediaRegistry.remove(it) }
            }
        }
    }

    fun cleanup() {
        VibeLogger.d("TelegramNotificationService", "Cleanup: clearing registries")
        recentIds.clear()
        mediaRegistry.clear()
        mediaRegistryTimestamps.clear()
        bufferedProgress.clear()
    }

    fun notifyCancelled(accountIndex: Int, chatId: Long, messageId: Long) {
        VibeLogger.d("TelegramNotificationService", "Manual cancellation: account=$accountIndex, chat=$chatId, msg=$messageId")
        
        unregisterMedia(accountIndex, messageId)
        
        // Emit cancelled state if UI is observing edits
        val msgObj = TelegramCoreAdapter.getMessageById(messageId.toInt(), accountIndex)
        if (msgObj != null) {
            val vibeMessage = mapper.mapMessage(msgObj).copy(
                deliveryStatus = VibeDeliveryStatus.CANCELLED
            )
            _messageEdits.tryEmit(vibeMessage)
        }
    }

    private fun normalizePath(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        return try {
            java.io.File(path).absolutePath
        } catch (e: Exception) {
            path
        }
    }

    private fun unregisterMedia(accountIndex: Int, localMessageId: Long) {
        val iterator = mediaRegistry.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key.accountIndex == accountIndex) {
                val messageIds = entry.value
                if (messageIds.remove(localMessageId)) {
                    val path = entry.key.normalizedPath
                    val stillTracked = mediaRegistry.any { (k, v) -> 
                        k.accountIndex == accountIndex && k.normalizedPath == path && v.isNotEmpty()
                    }
                    if (!stillTracked) {
                        bufferedProgress.remove(BufferedProgressKey(accountIndex, path))
                    }
                }
                if (messageIds.isEmpty()) {
                    iterator.remove()
                }
            }
        }
    }
}
