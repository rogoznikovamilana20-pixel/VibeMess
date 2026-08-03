package com.vibe.ui.data.sync

import com.vibe.bridge.model.VibeChat
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.VibeAppContext
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.ChatEntity
import com.vibe.ui.data.repository.ChatRepository
import com.vibe.ui.di.VibeContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Background writer that mirrors the Telegram chat list into Room.
 *
 * Only system fields are synced ([ChatRepository.syncChat] preserves the
 * user-controlled `isPersonal` flag), so manual «Личное/Работа» overrides survive
 * every sync. The sync service is the sole Room writer for chat system data.
 */
object ChatSyncService {

    private const val TAG = "ChatSyncService"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    /**
     * Subscribes to the bridge chat flow and upserts every chat into Room.
     * Idempotent: calling start() again does nothing while the flow is active.
     */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            try {
                if (!VibeContainer.isInitialized()) {
                    VibeContainer.initialize()
                }
                val gateway = VibeContainer.getGateway()
                val accountId = runCatching {
                    gateway.accounts.getCurrentAccount().userId
                }.getOrDefault(0L)
                if (accountId <= 0L) {
                    VibeLogger.i(TAG, "No Telegram session — chat sync skipped")
                    return@launch
                }
                val repo = ChatRepository(VibeDatabase.getDatabase(VibeAppContext.get()))

                gateway.chats.getActiveChats()
                    .catch { e ->
                        VibeLogger.w(TAG, "sync flow failed, will retry on next emit", e)
                    }
                    .collect { chats ->
                        if (chats.isEmpty()) return@collect
                        for (chat in chats) {
                            repo.syncChat(chat.toEntity(accountId))
                        }
                    }
            } catch (e: Exception) {
                VibeLogger.e(TAG, "start failed", e)
            }
        }
    }

    private fun VibeChat.toEntity(accountId: Long): ChatEntity {
        return ChatEntity(
            accountId = accountId,
            id = id,
            title = title,
            type = type.name,
            lastMessageText = lastMessage?.text,
            lastMessageTime = lastActivityDate.takeIf { it > 0 } ?: lastMessage?.date,
            unreadCount = unreadCount,
            isMuted = isMuted,
            isPinned = isPinned,
            isArchived = isArchived,
            draftText = draftText,
            isPersonal = true,
            avatarPath = avatarPath,
            lastSynced = System.currentTimeMillis()
        )
    }
}
