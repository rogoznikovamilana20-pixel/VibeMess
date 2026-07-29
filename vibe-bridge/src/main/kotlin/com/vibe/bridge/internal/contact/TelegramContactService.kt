package com.vibe.bridge.internal.contact

import com.vibe.bridge.api.IContactService
import com.vibe.bridge.model.VibeUser
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import org.telegram.messenger.ContactsController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig
import com.vibe.common.logging.VibeLogger

/**
 * Implementation of [IContactService] using Telegram core.
 */
internal class TelegramContactService : IContactService, NotificationCenter.NotificationCenterDelegate {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _contactUpdates = MutableSharedFlow<List<VibeUser>>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun getContacts(): List<VibeUser> = withContext(Dispatchers.Default) {
        val account = UserConfig.selectedAccount
        val contactsController = ContactsController.getInstance(account)
        TelegramCoreAdapter.mapContacts(contactsController.contacts, account)
    }

    override fun observeContacts(): Flow<List<VibeUser>> = _contactUpdates.asSharedFlow()

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        if (id == NotificationCenter.contactsDidLoad) {
            VibeLogger.d("TelegramContactService", "contactsDidLoad received for account $account")
            val contactsController = ContactsController.getInstance(account)
            val contacts = contactsController.contacts
            
            if (contacts.isNullOrEmpty()) {
                return
            }

            // Create a copy to avoid ConcurrentModificationException during mapping on a background thread
            val contactsCopy = ArrayList(contacts)

            serviceScope.launch {
                val vibeUsers = TelegramCoreAdapter.mapContacts(contactsCopy, account)
                _contactUpdates.emit(vibeUsers)
                VibeLogger.d("TelegramContactService", "Contact list updated: ${vibeUsers.size} users emitted")
            }
        }
    }

    fun cleanup() {
        VibeLogger.d("TelegramContactService", "Cleanup: canceling service scope")
        serviceScope.cancel()
    }
}
