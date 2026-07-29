package com.vibe.ui.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "marketplace_listings")
data class MarketplaceListingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageUri: String? = null,
    val createdAt: Long,
    val sellerName: String,
    val isActive: Boolean = true
)
