package com.vibe.ui.e2e

import org.junit.Assert.*
import org.junit.Test

class CryptoUtilsTest {

    @Test
    fun `constantTimeEquals compares byte arrays correctly`() {
        val a = byteArrayOf(1, 2, 3, 4, 5)
        val b = byteArrayOf(1, 2, 3, 4, 5)
        val c = byteArrayOf(1, 2, 3, 4, 6)

        assertTrue(CryptoUtils.constantTimeEquals(a, b))
        assertFalse(CryptoUtils.constantTimeEquals(a, c))
    }

    @Test
    fun `constantTimeEquals compares strings correctly`() {
        assertTrue(CryptoUtils.constantTimeEquals("hello", "hello"))
        assertFalse(CryptoUtils.constantTimeEquals("hello", "world"))
    }

    @Test
    fun `sha256 produces 32-byte hash`() {
        val data = "test data".toByteArray()
        val hash = CryptoUtils.sha256(data)
        assertEquals(32, hash.size)
    }

    @Test
    fun `sha256 is deterministic`() {
        val data = "deterministic".toByteArray()
        val hash1 = CryptoUtils.sha256(data)
        val hash2 = CryptoUtils.sha256(data)
        assertArrayEquals(hash1, hash2)
    }

    @Test
    fun `toHex and fromHex round-trip`() {
        val original = byteArrayOf(0x0A, 0xFF.toByte(), 0x10, 0x00, 0x7B)
        val hex = CryptoUtils.toHex(original)
        val decoded = CryptoUtils.fromHex(hex)

        assertEquals("0aff10007b", hex)
        assertArrayEquals(original, decoded)
    }

    @Test
    fun `generateSalt returns correct length`() {
        val salt32 = CryptoUtils.generateSalt(32)
        val salt16 = CryptoUtils.generateSalt(16)
        assertEquals(32, salt32.size)
        assertEquals(16, salt16.size)
    }

    @Test
    fun `generateSalt returns different values`() {
        val salt1 = CryptoUtils.generateSalt()
        val salt2 = CryptoUtils.generateSalt()
        assertFalse("Two random salts should differ",
            salt1.contentEquals(salt2))
    }

    @Test
    fun `computeSafetyNumber produces 32-byte fingerprint`() {
        val key1 = byteArrayOf(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31)
        val key2 = byteArrayOf(10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41)
        val safetyNumber = CryptoUtils.computeSafetyNumber(key1, key2)
        assertEquals(32, safetyNumber.size)
    }

    @Test
    fun `computeSafetyNumber differs for different key orders`() {
        val key1 = byteArrayOf(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31)
        val key2 = byteArrayOf(10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41)
        val sn1 = CryptoUtils.computeSafetyNumber(key1, key2)
        val sn2 = CryptoUtils.computeSafetyNumber(key2, key1)
        assertFalse("Safety numbers should differ when key order is swapped",
            sn1.contentEquals(sn2))
    }

    @Test
    fun `formatSafetyNumber produces readable format`() {
        val data = byteArrayOf(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31)
        val formatted = CryptoUtils.formatSafetyNumber(data)
        // Should be hex chunks of 5 separated by spaces
        assertTrue(formatted.contains(" "))
        assertTrue(formatted.all { it.isLetterOrDigit() || it == ' ' })
    }

    @Test
    fun `zeroize clears byte array`() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        CryptoUtils.zeroize(data)
        assertTrue(data.all { it == 0.toByte() })
    }
}
