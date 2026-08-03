package com.vibe.ui.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for chats.
 *
 * The primary key is (accountId, id): Telegram chat ids are only unique per account,
 * and Vibe supports multiple Telegram accounts.
 */
@Entity(
    tableName = "chats",
    primaryKeys = ["accountId", "id"],
    indices = [Index(value = ["accountId", "isPersonal", "isPinned", "lastMessageTime"])]
)
data class ChatEntity(
    val accountId: Long,
    val id: Long,
    val title: String,
    val type: String, // PRIVATE, GROUP, CHANNEL, SUPERGROUP
    val lastMessageText: String?,
    val lastMessageTime: Long?,
    val unreadCount: Int,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val draftText: String?,
    val isPersonal: Boolean = true, // Личное/Работа
    val avatarPath: String? = null, // Cached Telegram avatar file path
    val lastSynced: Long = System.currentTimeMillis()
)
