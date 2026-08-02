package com.vibe.ui.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vibe.ui.data.db.entity.PayoutRequestEntity
import com.vibe.ui.data.db.entity.PurchaseEntity
import com.vibe.ui.data.db.entity.SparkBalanceEntity
import com.vibe.ui.data.db.entity.VibePlusStatusEntity

@Dao
interface SparkBalanceDao {
    @Query("SELECT * FROM spark_balance WHERE id = 1")
    suspend fun get(): SparkBalanceEntity?

    @Insert
    suspend fun insert(entity: SparkBalanceEntity)

    @Update
    suspend fun update(entity: SparkBalanceEntity)

    /**
     * Atomically debits the balance, failing when the current balance is
     * insufficient. SQLite runs this check-and-update as one statement,
     * so concurrent spends cannot double-drain the balance.
     */
    @Query("UPDATE spark_balance SET balance = balance - :amount WHERE id = 1 AND balance >= :amount")
    suspend fun spend(amount: Long): Int

    @Query("UPDATE spark_balance SET balance = balance + :amount WHERE id = 1")
    suspend fun add(amount: Long): Int
}

@Dao
interface VibePlusStatusDao {
    @Query("SELECT * FROM vibe_plus_status WHERE id = 1")
    suspend fun get(): VibePlusStatusEntity?

    @Insert
    suspend fun insert(entity: VibePlusStatusEntity)

    @Update
    suspend fun update(entity: VibePlusStatusEntity)
}

@Dao
interface PurchaseDao {
    @Insert
    suspend fun insert(entity: PurchaseEntity): Long

    @Query("SELECT * FROM purchases ORDER BY createdAt DESC")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getById(id: Long): PurchaseEntity?

    @Query("UPDATE purchases SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}

@Dao
interface PayoutRequestDao {
    @Insert
    suspend fun insert(entity: PayoutRequestEntity): Long

    @Query("SELECT * FROM payout_requests ORDER BY createdAt DESC")
    suspend fun getAll(): List<PayoutRequestEntity>
}
