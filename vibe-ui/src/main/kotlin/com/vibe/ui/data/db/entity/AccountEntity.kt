package com.vibe.ui.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vibe_accounts",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["username"], unique = true)
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val vibeId: String,
    val avatarPath: String? = null,
    val bio: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
