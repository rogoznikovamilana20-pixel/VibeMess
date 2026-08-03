package com.vibe.common.crypto

import java.security.MessageDigest

/**
 * Safety number generator for manual verification of E2EE sessions.
 * Both parties compare this number to ensure no MITM attack.
 */
object SafetyNumberGenerator {

    fun generate(ourIdentityKey: ByteArray, theirIdentityKey: ByteArray): String {
        val combined = ByteArray(ourIdentityKey.size + theirIdentityKey.size)
        System.arraycopy(ourIdentityKey, 0, combined, 0, ourIdentityKey.size)
        System.arraycopy(theirIdentityKey, 0, combined, ourIdentityKey.size, theirIdentityKey.size)
        
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined)
        
        return hash.joinToString("") { "%02X".format(it) }
            .chunked(5)
            .joinToString(" ")
    }

    fun verify(ourIdentityKey: ByteArray, theirIdentityKey: ByteArray, expected: String): Boolean {
        val actual = generate(ourIdentityKey, theirIdentityKey)
        return actual.equals(expected, ignoreCase = true)
    }

    fun formatForDisplay(safetyNumber: String): List<String> {
        return safetyNumber.split(" ")
    }
}
