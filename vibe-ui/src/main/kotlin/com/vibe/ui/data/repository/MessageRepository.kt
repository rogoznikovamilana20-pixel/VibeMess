package com.vibe.ui.data.repository

import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for message data operations.
 */
class MessageRepository(private val database: VibeDatabase) {

    private val messageDao = database.messageDao()

    fun getMessagesByChatId(chatId: Long): Flow<List<MessageEntity>> = 
        messageDao.getMessagesByChatId(chatId)

    suspend fun getRecentMessages(chatId: Long, limit: Int = 50): List<MessageEntity> = 
        messageDao.getRecentMessages(chatId, limit)

    suspend fun getMessageById(messageId: Long): MessageEntity? = 
        messageDao.getMessageById(messageId)

    fun searchMessages(query: String): Flow<List<MessageEntity>> = 
        messageDao.searchMessages(query)

    suspend fun insertMessage(message: MessageEntity) = messageDao.insertMessage(message)

    suspend fun insertMessages(messages: List<MessageEntity>) = messageDao.insertMessages(messages)

    suspend fun updateMessage(message: MessageEntity) = messageDao.updateMessage(message)

    suspend fun deleteMessage(message: MessageEntity) = messageDao.deleteMessage(message)

    suspend fun deleteMessagesByChatId(chatId: Long) = messageDao.deleteMessagesByChatId(chatId)

    suspend fun deleteAllMessages() = messageDao.deleteAllMessages()

    suspend fun getMessageCount(chatId: Long): Int = messageDao.getMessageCount(chatId)
}
