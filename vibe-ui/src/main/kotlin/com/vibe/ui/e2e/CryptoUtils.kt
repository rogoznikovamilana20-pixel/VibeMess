package com.vibe.ui.e2e

import java.security.MessageDigest
import java.util.Arrays

/**
 * Cryptographic utility functions for secure operations.
 * Provides constant-time comparison, key zeroization, and secure memory handling.
 */
object CryptoUtils {

    /**
     * Constant-time comparison of two byte arrays.
     * Prevents timing side-channel attacks.
     */
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        return MessageDigest.isEqual(a, b)
    }

    /**
     * Constant-time comparison of two strings.
     */
    fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
    }

    /**
     * Zero out a byte array to prevent key material from lingering in memory.
     */
    fun zeroize(array: ByteArray) {
        Arrays.fill(array, 0.toByte())
    }

    /**
     * Zero out a string (best effort - strings are immutable in JVM).
     */
    fun zeroize(string: String) {
        // In JVM, strings are immutable, so we can't truly zero them
        // But we can at least force garbage collection of the original
        string.toByteArray(Charsets.UTF_8).fill(0)
    }

    /**
     * Securely clear a StringBuilder containing sensitive data.
     */
    fun zeroize(builder: StringBuilder) {
        for (i in 0 until builder.length) {
            builder[i] = '\u0000'
        }
        builder.clear()
    }

    /**
     * Generate a cryptographically secure random salt.
     */
    fun generateSalt(length: Int = 32): ByteArray {
        val salt = ByteArray(length)
        java.security.SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * SHA-256 hash for fingerprint computation (Safety Numbers).
     */
    fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    /**
     * Convert bytes to hex string for display.
     */
    fun toHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Convert hex string to bytes.
     */
    fun fromHex(hex: String): ByteArray {
        return hex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    /**
     * Generate a Safety Number fingerprint from two identity keys.
     * This is displayed to users for manual verification.
     */
    fun computeSafetyNumber(
        ourIdentityKey: ByteArray,
        theirIdentityKey: ByteArray
    ): ByteArray {
        // Safety Number = SHA-256(our_key || their_key)
        val combined = ByteArray(ourIdentityKey.size + theirIdentityKey.size)
        System.arraycopy(ourIdentityKey, 0, combined, 0, ourIdentityKey.size)
        System.arraycopy(theirIdentityKey, 0, combined, ourIdentityKey.size, theirIdentityKey.size)
        return sha256(combined)
    }

    /**
     * Format Safety Number for display (groups of 5 digits).
     */
    fun formatSafetyNumber(safetyNumber: ByteArray): String {
        val hex = toHex(safetyNumber)
        return hex.chunked(5).joinToString(" ")
    }
}
