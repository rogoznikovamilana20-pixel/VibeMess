package com.vibe.bridge.internal.contact

import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import com.vibe.bridge.model.VibeUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.ContactsController
import org.telegram.messenger.NotificationCenter
import org.telegram.tgnet.TLRPC

class TelegramContactServiceTest {

    private lateinit var contactService: TelegramContactService
    private var mockController: Any? = null

    @Before
    fun setup() {
        mockkObject(TelegramCoreAdapter)
        mockController = mockk<ContactsController>()
        contactService = TelegramContactService(
            currentAccount = 0,
            contactsControllerProvider = { mockController as ContactsController }
        )
    }

    @Test
    fun `service should be created with no initial contact updates`() = runBlocking {
        val updates = contactService.observeContacts()
        val emitted = mutableListOf<List<VibeUser>>()
        val job = launch {
            updates.collect { emitted.add(it) }
        }
        delay(100)
        job.cancel()
        assertTrue("No updates should be emitted before contactsDidLoad", emitted.isEmpty())
    }

    @Test
    fun `getContacts should return mapped contacts`() = runBlocking {
        every { TelegramCoreAdapter.mapContacts(any(), any<Int>()) } returns emptyList()
        val result = contactService.getContacts()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `cleanup should not throw`() {
        contactService.cleanup()
    }

    @Test
    fun `contactsDidLoad with empty args should not throw`() {
        contactService.didReceivedNotification(
            NotificationCenter.contactsDidLoad, 0
        )
    }
}
