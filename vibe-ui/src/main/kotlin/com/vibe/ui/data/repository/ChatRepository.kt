package com.vibe.ui.data.repository

import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for chat data operations.
 * Provides single source of truth for chat data.
 */
class ChatRepository(private val database: VibeDatabase) {

    private val chatDao = database.chatDao()

    fun getAllChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()

    fun getPersonalChats(): Flow<List<ChatEntity>> = chatDao.getPersonalChats()

    fun getWorkChats(): Flow<List<ChatEntity>> = chatDao.getWorkChats()

    suspend fun getChatById(chatId: Long): ChatEntity? = chatDao.getChatById(chatId)

    fun searchChats(query: String): Flow<List<ChatEntity>> = chatDao.searchChats(query)

    suspend fun insertChat(chat: ChatEntity) = chatDao.insertChat(chat)

    suspend fun insertChats(chats: List<ChatEntity>) = chatDao.insertChats(chats)

    suspend fun updateChat(chat: ChatEntity) = chatDao.updateChat(chat)

    suspend fun deleteChat(chat: ChatEntity) = chatDao.deleteChat(chat)

    suspend fun deleteAllChats() = chatDao.deleteAllChats()

    suspend fun updateUnreadCount(chatId: Long, count: Int) = chatDao.updateUnreadCount(chatId, count)

    suspend fun updatePinnedStatus(chatId: Long, isPinned: Boolean) = chatDao.updatePinnedStatus(chatId, isPinned)

    suspend fun updateMutedStatus(chatId: Long, isMuted: Boolean) = chatDao.updateMutedStatus(chatId, isMuted)
}
