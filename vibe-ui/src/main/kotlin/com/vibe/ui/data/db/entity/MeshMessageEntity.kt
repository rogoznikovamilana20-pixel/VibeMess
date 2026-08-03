package com.vibe.ui.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mesh_messages")
data class MeshMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val fromPeerId: String,
    val toPeerId: String = "",
    val text: String = "",
    val mediaPath: String? = null,
    val status: String = MeshStatus.PENDING,
    val isOutgoing: Boolean = false,
    val deliveredViaMesh: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

object MeshStatus {
    const val PENDING = "pending"
    const val SENT = "sent"
    const val ACKED = "acked"
    const val DELIVERED = "delivered"
    const val FAILED = "failed"
}
