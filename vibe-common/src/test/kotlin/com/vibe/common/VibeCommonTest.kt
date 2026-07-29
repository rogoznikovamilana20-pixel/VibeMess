package com.vibe.common

import com.vibe.common.extensions.capitalizeFirst
import com.vibe.common.extensions.isValidEmail
import com.vibe.common.result.VibeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VibeCommonTest {

    @Test
    fun testStringExtensions() {
        assertEquals("Vibe", "vibe".capitalizeFirst())
        assertTrue("test@example.com".isValidEmail())
    }

    @Test
    fun testVibeResult() {
        val success = VibeResult.Success("data")
        assertEquals("data", success.getOrNull())
        assertTrue(success.isSuccess)
    }
}
