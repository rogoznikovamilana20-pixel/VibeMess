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
internal class TelegramContactService(
    private val currentAccount: Int = UserConfig.selectedAccount,
    private val contactsControllerProvider: (Int) -> ContactsController = { ContactsController.getInstance(it) }
) : IContactService, NotificationCenter.NotificationCenterDelegate {

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
            VibeLogger.e("TelegramContactService", "background coroutine crashed", e)
        }
    )

    private val _contactUpdates = MutableSharedFlow<List<VibeUser>>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun getContacts(): List<VibeUser> = withContext(Dispatchers.Default) {
        val cc = contactsControllerProvider(currentAccount)
        TelegramCoreAdapter.mapContacts(cc.contacts, currentAccount)
    }

    override fun observeContacts(): Flow<List<VibeUser>> = _contactUpdates.asSharedFlow()

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        if (id == NotificationCenter.contactsDidLoad) {
            VibeLogger.d("TelegramContactService", "contactsDidLoad received for account $account")
            val cc = contactsControllerProvider(account)
            val contacts = cc.contacts
            
            if (contacts.isNullOrEmpty()) {
                return
            }

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
