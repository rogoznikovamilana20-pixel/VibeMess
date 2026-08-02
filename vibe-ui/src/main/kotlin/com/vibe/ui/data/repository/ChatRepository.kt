package com.vibe.ui.data.repository

import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for chat data operations.
 * Provides single source of truth for the «Личное/Работа» sections.
 */
class ChatRepository(private val database: VibeDatabase) {

    private val chatDao = database.chatDao()

    fun getAllChats(accountId: Long): Flow<List<ChatEntity>> = chatDao.getAllChats(accountId)

    fun getPersonalChats(accountId: Long): Flow<List<ChatEntity>> = chatDao.getPersonalChats(accountId)

    fun getWorkChats(accountId: Long): Flow<List<ChatEntity>> = chatDao.getWorkChats(accountId)

    suspend fun getChatById(accountId: Long, chatId: Long): ChatEntity? =
        chatDao.getChatById(accountId, chatId)

    suspend fun countChats(accountId: Long): Int = chatDao.countChats(accountId)

    fun searchChats(accountId: Long, query: String): Flow<List<ChatEntity>> =
        chatDao.searchChats(accountId, query)

    suspend fun insertChats(chats: List<ChatEntity>) = chatDao.insertChats(chats)

    /**
     * Syncs bridge chat data into Room. Never overwrites the user-controlled `isPersonal` flag.
     */
    suspend fun syncChat(chat: ChatEntity) = chatDao.syncChat(
        accountId = chat.accountId,
        id = chat.id,
        title = chat.title,
        type = chat.type,
        lastMessageText = chat.lastMessageText,
        lastMessageTime = chat.lastMessageTime,
        unreadCount = chat.unreadCount,
        isMuted = chat.isMuted,
        isPinned = chat.isPinned,
        isArchived = chat.isArchived,
        draftText = chat.draftText,
        isPersonal = chat.isPersonal,
        avatarPath = chat.avatarPath,
        lastSynced = chat.lastSynced
    )

    suspend fun setPersonal(accountId: Long, chatId: Long, isPersonal: Boolean) =
        chatDao.setPersonal(accountId, chatId, isPersonal)
}
