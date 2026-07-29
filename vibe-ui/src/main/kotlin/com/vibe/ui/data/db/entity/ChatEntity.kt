package com.vibe.ui.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for chats.
 */
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
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
    val lastSynced: Long = System.currentTimeMillis()
)
