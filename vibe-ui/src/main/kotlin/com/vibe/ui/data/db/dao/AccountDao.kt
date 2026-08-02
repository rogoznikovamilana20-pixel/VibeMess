package com.vibe.ui.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vibe.ui.data.db.entity.AccountEntity

@Dao
interface AccountDao {

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query("SELECT * FROM vibe_accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM vibe_accounts WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): AccountEntity?

    @Query("SELECT * FROM vibe_accounts WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): AccountEntity?

    @Query("SELECT * FROM vibe_accounts WHERE email = :email AND passwordHash = :passwordHash LIMIT 1")
    suspend fun authenticate(email: String, passwordHash: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM vibe_accounts")
    suspend fun count(): Int

    @Query("SELECT * FROM vibe_accounts ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): AccountEntity?
}
