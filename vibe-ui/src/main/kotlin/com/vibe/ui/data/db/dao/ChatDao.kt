package com.vibe.ui.data.db.dao

import androidx.room.*
import com.vibe.ui.data.db.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for chats.
 */
@Dao
interface ChatDao {

    @Query(
        "SELECT * FROM chats WHERE accountId = :accountId " +
            "ORDER BY isPinned DESC, lastMessageTime DESC"
    )
    fun getAllChats(accountId: Long): Flow<List<ChatEntity>>

    @Query(
        "SELECT * FROM chats WHERE accountId = :accountId AND isPersonal = 1 " +
            "ORDER BY isPinned DESC, lastMessageTime DESC"
    )
    fun getPersonalChats(accountId: Long): Flow<List<ChatEntity>>

    @Query(
        "SELECT * FROM chats WHERE accountId = :accountId AND isPersonal = 0 " +
            "ORDER BY isPinned DESC, lastMessageTime DESC"
    )
    fun getWorkChats(accountId: Long): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE accountId = :accountId AND id = :chatId")
    suspend fun getChatById(accountId: Long, chatId: Long): ChatEntity?

    @Query("SELECT COUNT(*) FROM chats WHERE accountId = :accountId")
    suspend fun countChats(accountId: Long): Int

    @Query("SELECT * FROM chats WHERE accountId = :accountId AND title LIKE '%' || :query || '%'")
    fun searchChats(accountId: Long, query: String): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    /**
     * Syncs a chat from the bridge WITHOUT touching the user-controlled `isPersonal` flag:
     * on conflict only system fields (title, last message, unread, flags, draft) are updated.
     * New chats are inserted with their current `isPersonal` value (or the default when omitted).
     */
    @Query(
        """
        INSERT INTO chats
            (accountId, id, title, type, lastMessageText, lastMessageTime, unreadCount,
             isMuted, isPinned, isArchived, draftText, isPersonal, avatarPath, lastSynced)
        VALUES
            (:accountId, :id, :title, :type, :lastMessageText, :lastMessageTime, :unreadCount,
             :isMuted, :isPinned, :isArchived, :draftText,
             COALESCE((SELECT isPersonal FROM chats WHERE accountId = :accountId AND id = :id), :isPersonal),
             :avatarPath, :lastSynced)
        ON CONFLICT(accountId, id) DO UPDATE SET
            title = excluded.title,
            type = excluded.type,
            lastMessageText = excluded.lastMessageText,
            lastMessageTime = excluded.lastMessageTime,
            unreadCount = excluded.unreadCount,
            isMuted = excluded.isMuted,
            isPinned = excluded.isPinned,
            isArchived = excluded.isArchived,
            draftText = excluded.draftText,
            avatarPath = excluded.avatarPath,
            lastSynced = excluded.lastSynced
        """
    )
    suspend fun syncChat(
        accountId: Long,
        id: Long,
        title: String,
        type: String,
        lastMessageText: String?,
        lastMessageTime: Long?,
        unreadCount: Int,
        isMuted: Boolean,
        isPinned: Boolean,
        isArchived: Boolean,
        draftText: String?,
        isPersonal: Boolean,
        avatarPath: String?,
        lastSynced: Long
    )

    @Query("UPDATE chats SET isPersonal = :isPersonal WHERE accountId = :accountId AND id = :chatId")
    suspend fun setPersonal(accountId: Long, chatId: Long, isPersonal: Boolean)

    @Query("UPDATE chats SET unreadCount = :count WHERE accountId = :accountId AND id = :chatId")
    suspend fun updateUnreadCount(accountId: Long, chatId: Long, count: Int)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE accountId = :accountId AND id = :chatId")
    suspend fun updatePinnedStatus(accountId: Long, chatId: Long, isPinned: Boolean)

    @Query("UPDATE chats SET isMuted = :isMuted WHERE accountId = :accountId AND id = :chatId")
    suspend fun updateMutedStatus(accountId: Long, chatId: Long, isMuted: Boolean)
}
