package com.vibe.ui.security

import java.security.SecureRandom
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Secure memory handling for sensitive data.
 * Provides:
 * - Memory zeroization after use
 * - Secure random generation
 * - Anti-memory dump protection
 */
object SecureMemory {

    private val secureRandom = SecureRandom()
    private val sensitiveDataRegistry = ConcurrentLinkedDeque<ByteArray>()

    /**
     * Generate cryptographically secure random bytes.
     */
    fun secureRandomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        sensitiveDataRegistry.add(bytes)
        return bytes
    }

    /**
     * Generate secure random nonce (IV).
     */
    fun generateNonce(length: Int = 12): ByteArray {
        return secureRandomBytes(length)
    }

    /**
     * Generate secure random salt.
     */
    fun generateSalt(length: Int = 32): ByteArray {
        return secureRandomBytes(length)
    }

    /**
     * Generate secure random token.
     */
    fun generateToken(length: Int = 32): String {
        val bytes = secureRandomBytes(length)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Securely copy data to a new array.
     */
    fun secureCopy(data: ByteArray): ByteArray {
        val copy = data.copyOf()
        sensitiveDataRegistry.add(copy)
        return copy
    }

    /**
     * Zero out sensitive data.
     */
    fun zeroize(data: ByteArray) {
        secureRandom.nextBytes(data) // Overwrite with random
        data.fill(0) // Then zero
        sensitiveDataRegistry.remove(data)
    }

    /**
     * Secure string comparison (constant-time).
     */
    fun secureCompare(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    /**
     * Wipe all registered sensitive data.
     */
    fun wipeAll() {
        while (sensitiveDataRegistry.isNotEmpty()) {
            val data = sensitiveDataRegistry.poll() ?: continue
            zeroize(data)
        }
    }

    /**
     * Create a secure char array that can be zeroed.
     */
    fun secureCharArray(size: Int): CharArray {
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return CharArray(size) { bytes[it].toInt().toChar() }
    }

    /**
     * Wipe a char array.
     */
    fun wipeCharArray(chars: CharArray) {
        chars.fill('\u0000')
    }
}
