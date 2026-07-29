package com.vibe.ui.data.db.dao

import androidx.room.*
import com.vibe.ui.data.db.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for contacts.
 */
@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY firstName ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE firstName LIKE '%' || :query || '%' OR lastName LIKE '%' || :query || '%'")
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()
}
