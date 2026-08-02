package com.vibe.ui.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bot_messages")
data class BotMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val botId: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
