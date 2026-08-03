package com.vibe.ui.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vibe.ui.data.db.entity.BotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bot: BotEntity): Long

    @Update
    suspend fun update(bot: BotEntity)

    @Delete
    suspend fun delete(bot: BotEntity)

    @Query("SELECT * FROM bots ORDER BY createdAt DESC")
    fun getAll(): Flow<List<BotEntity>>

    @Query("SELECT * FROM bots ORDER BY createdAt DESC")
    suspend fun getAllNow(): List<BotEntity>

    @Query("SELECT * FROM bots WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BotEntity?

    @Query("SELECT * FROM bots WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): BotEntity?

    @Query("SELECT COUNT(*) FROM bots WHERE username = :username")
    suspend fun countByUsername(username: String): Int
}
