package com.vibe.ui.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vibe.ui.data.db.entity.MarketplaceListingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketplaceDao {

    @Query("SELECT * FROM marketplace_listings ORDER BY createdAt DESC")
    fun getAllListings(): Flow<List<MarketplaceListingEntity>>

    @Query("SELECT * FROM marketplace_listings WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveListings(): Flow<List<MarketplaceListingEntity>>

    @Query("SELECT * FROM marketplace_listings WHERE category = :category AND isActive = 1 ORDER BY createdAt DESC")
    fun getListingsByCategory(category: String): Flow<List<MarketplaceListingEntity>>

    @Insert
    suspend fun insertListing(listing: MarketplaceListingEntity)

    @Update
    suspend fun updateListing(listing: MarketplaceListingEntity)

    @Delete
    suspend fun deleteListing(listing: MarketplaceListingEntity)

    @Query("DELETE FROM marketplace_listings")
    suspend fun deleteAll()

    @Query("UPDATE marketplace_listings SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)
}
