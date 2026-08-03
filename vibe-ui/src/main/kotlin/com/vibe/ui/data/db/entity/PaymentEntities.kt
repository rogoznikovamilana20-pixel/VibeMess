package com.vibe.ui.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spark_balance")
data class SparkBalanceEntity(
    @PrimaryKey val id: Int = 1,
    val balance: Long = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vibe_plus_status")
data class VibePlusStatusEntity(
    @PrimaryKey val id: Int = 1,
    val isActive: Boolean = false,
    val expiresAt: Long = 0,
    val trialUsed: Boolean = false
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemType: String,
    val amountKopecks: Long,
    val status: String,
    val providerPaymentId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payout_requests")
data class PayoutRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sparks: Int,
    val bankName: String,
    val accountNumber: String,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)
