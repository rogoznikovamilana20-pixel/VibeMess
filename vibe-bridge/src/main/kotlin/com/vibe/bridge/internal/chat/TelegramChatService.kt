package com.vibe.bridge.internal.chat

import com.vibe.bridge.api.IChatService
import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import com.vibe.bridge.mapper.ChatMapper
import com.vibe.bridge.model.VibeChat
import com.vibe.bridge.model.VibeReadState
import com.vibe.bridge.model.VibeTypingStatus
import com.vibe.common.logging.VibeLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import androidx.collection.LongSparseArray
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Implementation of [IChatService] using Telegram internal classes.
 */
internal class TelegramChatService(
    private val chatMapper: ChatMapper
) : IChatService, NotificationCenter.NotificationCenterDelegate {

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
            VibeLogger.e("TelegramChatService", "background coroutine crashed", e)
        }
    )

    private val _chatUpdates = MutableSharedFlow<List<VibeChat>>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val typingFlows = ConcurrentHashMap<Long, MutableSharedFlow<List<VibeTypingStatus>>>()
    private val readStateFlows = ConcurrentHashMap<Long, MutableSharedFlow<VibeReadState>>()

    init {
        serviceScope.launch {
            while (isActive) {
                delay(30_000)
                cleanupStaleFlows()
            }
        }
    }

    private fun cleanupStaleFlows() {
        val subscriberCounts = mutableMapOf<Long, Int>()
        typingFlows.forEach { (chatId, _) ->
            subscriberCounts[chatId] = (subscriberCounts[chatId] ?: 0) + 1
        }
        typingFlows.keys.filter { chatId ->
            typingFlows[chatId]?.subscriptionCount?.value == 0
        }.forEach { chatId ->
            typingFlows.remove(chatId)
        }
        readStateFlows.keys.filter { chatId ->
            readStateFlows[chatId]?.subscriptionCount?.value == 0
        }.forEach { chatId ->
            readStateFlows.remove(chatId)
        }
    }

    override suspend fun getChat(chatId: Long): VibeChat? {
        val accountIndex = UserConfig.selectedAccount
        var chat: VibeChat? = null
        runOnUiThreadAndWait {
            val controller = MessagesController.getInstance(accountIndex)
            val dialog = controller.dialogs_dict.get(chatId)
            chat = if (dialog != null) chatMapper.mapChat(dialog, accountIndex) else null
        }
        return chat
    }

    override fun getActiveChats(): Flow<List<VibeChat>> {
        // Initial emit if not already emitted
        if (_chatUpdates.replayCache.isEmpty()) {
            refreshChats(UserConfig.selectedAccount)
        }
        return _chatUpdates.asSharedFlow()
    }

    override fun markChatAsRead(chatId: Long) {
        val accountIndex = UserConfig.selectedAccount
        runOnUiThreadAndWait {
            try {
                val controller = MessagesController.getInstance(accountIndex)
                val req = TLRPC.TL_messages_readHistory()
                req.peer = controller.getInputPeer(chatId)
                req.max_id = Int.MAX_VALUE
                ConnectionsManager.getInstance(accountIndex).sendRequest(
                    req,
                    { response, error ->
                        if (error != null) {
                            VibeLogger.w("TelegramChatService", "markChatAsRead failed for chat $chatId: $error")
                        } else {
                            val result = response as? TLRPC.TL_messages_affectedMessages
                            if (result == null) {
                                VibeLogger.w("TelegramChatService", "markChatAsRead: unexpected response type")
                            } else {
                                refreshChats(accountIndex)
                            }
                        }
                    },
                    { },
                    ConnectionsManager.RequestFlagFailOnServerErrors
                )
            } catch (e: Exception) {
                VibeLogger.e("TelegramChatService", "markChatAsRead failed for chat $chatId", e)
            }
        }
    }

    override fun observeTyping(chatId: Long): Flow<List<VibeTypingStatus>> {
        val flow = typingFlows.getOrPut(chatId) {
            MutableSharedFlow<List<VibeTypingStatus>>(
                replay = 1,
                extraBufferCapacity = 4,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            ).also { 
                // Initial state
                it.tryEmit(TelegramCoreAdapter.getTypingUsers(chatId, UserConfig.selectedAccount))
            }
        }
        return flow.asSharedFlow().distinctUntilChanged()
    }

    override fun observeReadState(chatId: Long): Flow<VibeReadState> {
        val flow = readStateFlows.getOrPut(chatId) {
            MutableSharedFlow<VibeReadState>(
                replay = 1,
                extraBufferCapacity = 4,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            ).also { 
                // Initial state
                it.tryEmit(TelegramCoreAdapter.getReadState(chatId, UserConfig.selectedAccount))
            }
        }
        return flow.asSharedFlow().distinctUntilChanged()
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        if (id == NotificationCenter.dialogsNeedReload || 
            id == NotificationCenter.newDraftReceived || 
            id == NotificationCenter.notificationsSettingsUpdated) {
            VibeLogger.d("TelegramChatService", "Notification $id received for account $account")
            refreshChats(account)
        } else if (id == NotificationCenter.updateInterfaces) {
            val mask = args[0] as? Int ?: 0
            if (mask and MessagesController.UPDATE_MASK_USER_PRINT != 0) {
                updateAllTyping(account)
            }
            if (mask and MessagesController.UPDATE_MASK_READ_DIALOG_MESSAGE != 0) {
                // For now, we update all active read state flows when this mask is received
                // as the notification doesn't always specify the chatId in simple way.
                updateAllReadStates(account)
            }
        } else if (id == NotificationCenter.messagesRead) {
            val inbox = args[0] as? LongSparseArray<*>
            val outbox = args[1] as? LongSparseArray<*>
            
            val affectedChatIds = mutableSetOf<Long>()
            inbox?.let { for (i in 0 until it.size()) affectedChatIds.add(it.keyAt(i)) }
            outbox?.let { for (i in 0 until it.size()) affectedChatIds.add(it.keyAt(i)) }
            
            affectedChatIds.forEach { updateReadState(it, account) }
        }
    }

    private fun refreshChats(accountIndex: Int) {
        serviceScope.launch {
            val vibeChats = runOnUiThreadAndWait<List<VibeChat>> {
                val controller = MessagesController.getInstance(accountIndex)
                val dialogs = controller.allDialogs
                val dialogsCopy = ArrayList(dialogs)
                dialogsCopy.map { dialog ->
                    chatMapper.mapChat(dialog, accountIndex)
                }
            } ?: emptyList()
            _chatUpdates.emit(vibeChats)
            VibeLogger.d("TelegramChatService", "Active chats updated: ${vibeChats.size} chats emitted")
        }
    }

    private fun <T> runOnUiThreadAndWait(action: () -> T): T? {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return action()
        }
        val latch = CountDownLatch(1)
        var result: T? = null
        var failure: Throwable? = null
        org.telegram.messenger.AndroidUtilities.runOnUIThread {
            try {
                result = action()
            } catch (e: Exception) {
                failure = e
            } finally {
                latch.countDown()
            }
        }
        try {
            latch.await(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        if (failure != null) {
            VibeLogger.w("TelegramChatService", "runOnUiThreadAndWait failed", failure)
        }
        return result
    }

    private fun updateAllTyping(accountIndex: Int) {
        typingFlows.forEach { (chatId, flow) ->
            val statuses = TelegramCoreAdapter.getTypingUsers(chatId, accountIndex)
            flow.tryEmit(statuses)
        }
    }

    private fun updateAllReadStates(accountIndex: Int) {
        readStateFlows.keys.forEach { chatId ->
            updateReadState(chatId, accountIndex)
        }
    }

    private fun updateReadState(chatId: Long, accountIndex: Int) {
        val flow = readStateFlows[chatId] ?: return
        val state = TelegramCoreAdapter.getReadState(chatId, accountIndex)
        flow.tryEmit(state)
    }

    fun cleanup() {
        VibeLogger.d("TelegramChatService", "Cleanup: canceling service scope")
        serviceScope.cancel()
        typingFlows.clear()
        readStateFlows.clear()
    }
}
