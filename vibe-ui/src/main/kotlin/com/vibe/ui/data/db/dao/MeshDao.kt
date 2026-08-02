package com.vibe.ui.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vibe.ui.data.db.entity.MeshMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeshDao {

    @Insert
    suspend fun insert(message: MeshMessageEntity): Long

    @Query("SELECT * FROM mesh_messages ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MeshMessageEntity>>

    @Query("SELECT * FROM mesh_messages ORDER BY createdAt DESC")
    suspend fun getAllNow(): List<MeshMessageEntity>

    @Query("SELECT * FROM mesh_messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getByMessageId(messageId: String): MeshMessageEntity?

    @Query("UPDATE mesh_messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateStatus(messageId: String, status: String)

    @Query("DELETE FROM mesh_messages")
    suspend fun clearAll()
}
