package com.vibe.ui.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.vibe.ui.data.db.entity.TimelinePostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {

    @Query("SELECT * FROM timeline_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<TimelinePostEntity>>

    @Insert
    suspend fun insertPost(post: TimelinePostEntity)

    @Query("UPDATE timeline_posts SET likes = likes + :delta, isLiked = :liked WHERE id = :postId")
    suspend fun setLiked(postId: Long, liked: Boolean, delta: Int)

    @Delete
    suspend fun deletePost(post: TimelinePostEntity)

    @Query("DELETE FROM timeline_posts")
    suspend fun deleteAll()
}
