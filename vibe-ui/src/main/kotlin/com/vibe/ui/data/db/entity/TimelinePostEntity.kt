package com.vibe.ui.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timeline_posts")
data class TimelinePostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val authorName: String,
    val timestamp: Long,
    val imageUri: String? = null,
    val likes: Int = 0,
    val comments: Int = 0
)
