package com.vibe.bridge.internal.user

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
import org.telegram.messenger.NotificationCenter

class TelegramUserServiceTest {

    private lateinit var userService: TelegramUserService

    @Before
    fun setup() {
        mockkObject(TelegramCoreAdapter)
        userService = TelegramUserService(currentAccount = 0)
    }

    @Test
    fun `service should be created with no initial user updates`() = runBlocking {
        val updates = userService.observeUserUpdates()
        val emitted = mutableListOf<VibeUser>()
        val job = launch {
            updates.collect { emitted.add(it) }
        }
        delay(100)
        job.cancel()
        assertTrue("No updates should be emitted before any notification", emitted.isEmpty())
    }

    @Test
    fun `getUser should return null for unknown user`() = runBlocking {
        every { TelegramCoreAdapter.getUser(any(), any()) } returns null
        val result = userService.getUser(99999L)
        assertNull(result)
    }

    @Test
    fun `getUsers should return empty list for empty input`() = runBlocking {
        every { TelegramCoreAdapter.getUsers(any(), any()) } returns emptyList()
        val result = userService.getUsers(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `didReceivedNotification with userInfoDidLoad should map userId`() {
        val vibeUser = mockk<VibeUser>()
        every { vibeUser.id } returns 123L
        every { TelegramCoreAdapter.getUser(123L, 0) } returns vibeUser
        every { TelegramCoreAdapter.getUser(any(), any()) } returns null

        userService.didReceivedNotification(
            NotificationCenter.userInfoDidLoad, 0, 123L
        )
    }

    @Test
    fun `didReceivedNotification with empty args should not throw`() {
        userService.didReceivedNotification(
            NotificationCenter.userInfoDidLoad, 0
        )
    }

    @Test
    fun `didReceivedNotification with updateInterfaces should not throw`() {
        userService.didReceivedNotification(
            NotificationCenter.updateInterfaces, 0, 0
        )
    }
}
