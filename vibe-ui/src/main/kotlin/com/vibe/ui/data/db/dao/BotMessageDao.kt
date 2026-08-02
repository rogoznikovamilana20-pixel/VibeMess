package com.vibe.ui.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vibe.ui.data.db.entity.BotMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BotMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: BotMessageEntity): Long

    @Query("SELECT * FROM bot_messages WHERE botId = :botId ORDER BY timestamp ASC")
    fun getByBotId(botId: Long): Flow<List<BotMessageEntity>>

    @Query("DELETE FROM bot_messages WHERE botId = :botId")
    suspend fun clearByBotId(botId: Long)
}
