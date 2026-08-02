package com.vibe.ui.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bots")
data class BotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: String = "",
    val token: String = "",
    val name: String,
    val username: String,
    val description: String = "",
    val avatarInitial: String = "",
    val systemPrompt: String = "",
    val commandsJson: String = "[]",
    val scriptRepliesJson: String = "[]",
    val isAi: Boolean = true,
    val isEnabled: Boolean = true,
    val isLocal: Boolean = true,
    val lastUpdateId: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
