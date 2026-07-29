package com.vibe.common.validation

import org.junit.Assert.*
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun `isValidChatId returns true for non-zero`() {
        assertTrue(ValidationUtils.isValidChatId(42L))
        assertTrue(ValidationUtils.isValidChatId(-1L))
    }

    @Test
    fun `isValidChatId returns false for zero`() {
        assertFalse(ValidationUtils.isValidChatId(0L))
    }

    @Test
    fun `isValidUserId returns true for positive`() {
        assertTrue(ValidationUtils.isValidUserId(1L))
        assertTrue(ValidationUtils.isValidUserId(Long.MAX_VALUE))
    }

    @Test
    fun `isValidUserId returns false for non-positive`() {
        assertFalse(ValidationUtils.isValidUserId(0L))
        assertFalse(ValidationUtils.isValidUserId(-1L))
    }

    @Test
    fun `isValidMessageId returns true for positive`() {
        assertTrue(ValidationUtils.isValidMessageId(1L))
    }

    @Test
    fun `isValidMessageId returns false for non-positive`() {
        assertFalse(ValidationUtils.isValidMessageId(0L))
        assertFalse(ValidationUtils.isValidMessageId(-1L))
    }

    @Test
    fun `isValidMessageText accepts valid text`() {
        assertTrue(ValidationUtils.isValidMessageText("Hello"))
    }

    @Test
    fun `isValidMessageText rejects null or empty`() {
        assertFalse(ValidationUtils.isValidMessageText(null))
        assertFalse(ValidationUtils.isValidMessageText(""))
    }

    @Test
    fun `isValidMessageText rejects over max length`() {
        val longText = "x".repeat(4097)
        assertFalse(ValidationUtils.isValidMessageText(longText))
    }

    @Test
    fun `isValidMessageText respects custom max length`() {
        assertFalse(ValidationUtils.isValidMessageText("too long", maxLength = 5))
        assertTrue(ValidationUtils.isValidMessageText("short", maxLength = 10))
    }

    @Test
    fun `isValidChatTitle accepts valid title`() {
        assertTrue(ValidationUtils.isValidChatTitle("Group Chat"))
    }

    @Test
    fun `isValidChatTitle rejects null or empty`() {
        assertFalse(ValidationUtils.isValidChatTitle(null))
        assertFalse(ValidationUtils.isValidChatTitle(""))
    }

    @Test
    fun `isValidChatTitle rejects over max length`() {
        val longTitle = "x".repeat(129)
        assertFalse(ValidationUtils.isValidChatTitle(longTitle))
    }

    @Test
    fun `isValidUsername accepts valid username`() {
        assertTrue(ValidationUtils.isValidUsername("@username123"))
        assertTrue(ValidationUtils.isValidUsername("@user_1"))
    }

    @Test
    fun `isValidUsername rejects invalid format`() {
        assertFalse(ValidationUtils.isValidUsername(null))
        assertFalse(ValidationUtils.isValidUsername(""))
        assertFalse(ValidationUtils.isValidUsername("plainname"))
        assertFalse(ValidationUtils.isValidUsername("@a b"))
        assertFalse(ValidationUtils.isValidUsername("@ab!!"))
    }

    @Test
    fun `isValidPhoneNumber accepts valid phone`() {
        assertTrue(ValidationUtils.isValidPhoneNumber("+71234567890"))
        assertTrue(ValidationUtils.isValidPhoneNumber("+123456789012345"))
    }

    @Test
    fun `isValidPhoneNumber rejects invalid phone`() {
        assertFalse(ValidationUtils.isValidPhoneNumber(null))
        assertFalse(ValidationUtils.isValidPhoneNumber(""))
        assertFalse(ValidationUtils.isValidPhoneNumber("12345"))
        assertFalse(ValidationUtils.isValidPhoneNumber("+abc"))
    }

    @Test
    fun `isValidUrl accepts valid urls`() {
        assertTrue(ValidationUtils.isValidUrl("https://example.com"))
        assertTrue(ValidationUtils.isValidUrl("http://example.com/path?q=1"))
    }

    @Test
    fun `isValidUrl rejects invalid urls`() {
        assertFalse(ValidationUtils.isValidUrl(null))
        assertFalse(ValidationUtils.isValidUrl(""))
        assertFalse(ValidationUtils.isValidUrl("not-a-url"))
    }

    @Test
    fun `isValidFilePath accepts valid path`() {
        assertTrue(ValidationUtils.isValidFilePath("/path/to/file.txt"))
    }

    @Test
    fun `isValidFilePath rejects path with invalid chars`() {
        assertFalse(ValidationUtils.isValidFilePath("file<>.txt"))
        assertFalse(ValidationUtils.isValidFilePath("file|.txt"))
    }

    @Test
    fun `sanitizeString removes control characters`() {
        val result = ValidationUtils.sanitizeString("he\u0000llo\u0001world")
        assertEquals("helloworld", result)
    }

    @Test
    fun `sanitizeString preserves normal text`() {
        val result = ValidationUtils.sanitizeString("Hello, World!")
        assertEquals("Hello, World!", result)
    }

    @Test
    fun `requireValid throws on false`() {
        assertThrows(IllegalArgumentException::class.java) {
            ValidationUtils.requireValid(false, "error msg")
        }
    }

    @Test
    fun `requireValid does not throw on true`() {
        ValidationUtils.requireValid(true, "should not throw")
    }
}
