package com.vibe.ui.data.db.dao

import androidx.room.*
import com.vibe.ui.data.db.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for chats.
 */
@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isPersonal = 1 ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getPersonalChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isPersonal = 0 ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getWorkChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: Long): ChatEntity?

    @Query("SELECT * FROM chats WHERE title LIKE '%' || :query || '%'")
    fun searchChats(query: String): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    @Query("UPDATE chats SET unreadCount = :count WHERE id = :chatId")
    suspend fun updateUnreadCount(chatId: Long, count: Int)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun updatePinnedStatus(chatId: Long, isPinned: Boolean)

    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun updateMutedStatus(chatId: Long, isMuted: Boolean)
}
