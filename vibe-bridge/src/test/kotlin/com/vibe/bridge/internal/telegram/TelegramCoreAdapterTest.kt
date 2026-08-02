package com.vibe.bridge.internal.telegram

import io.mockk.unmockkAll
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TelegramCoreAdapterTest {

    @Before
    fun setup() {
        unmockkAll()
    }

    @Test
    fun `mapUser should handle null input`() {
        assertNull(TelegramCoreAdapter.mapUser(null))
    }

    @Test
    fun `mapContacts should handle null input`() {
        assertTrue(TelegramCoreAdapter.mapContacts(null, 0).isEmpty())
    }

    @Test
    fun `mapContacts should handle empty input`() {
        assertTrue(TelegramCoreAdapter.mapContacts(ArrayList(), 0).isEmpty())
    }
}
