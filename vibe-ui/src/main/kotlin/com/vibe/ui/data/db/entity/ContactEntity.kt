package com.vibe.ui.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for contacts.
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val phone: String?,
    val isBot: Boolean,
    val isPremium: Boolean,
    val avatarPhotoId: Long?,
    val lastSynced: Long = System.currentTimeMillis()
)
