package com.vibe.ui.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for messages.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val text: String,
    val timestamp: Long,
    val type: String, // TEXT, PHOTO, VIDEO, DOCUMENT, VOICE, etc.
    val isOutgoing: Boolean,
    val isPinned: Boolean,
    val replyId: Long?,
    val attachmentPath: String?,
    val deliveryStatus: String?, // PENDING, SENT, ERROR, CANCELLED
    val lastSynced: Long = System.currentTimeMillis()
)
