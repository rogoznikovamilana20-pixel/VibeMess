package com.vibe.ui.e2e

import android.util.Base64
import android.util.Log
import org.json.JSONObject

/**
 * Safety Numbers for verifying contact identity.
 * Prevents MITM attacks by allowing users to manually verify key fingerprints.
 *
 * Reference: https://signal.org/docs/specifications/safenumbers/
 */
class SafetyNumbers {

    companion object {
        private const val TAG = "SafetyNumbers"
        private const val SAFETY_NUMBER_VERSION = 1
    }

    /**
     * Generate a Safety Number for a contact.
     * Returns a formatted string for display and a hash for comparison.
     */
    fun generateSafetyNumber(
        ourIdentityKey: ByteArray,
        theirIdentityKey: ByteArray
    ): SafetyNumber {
        // Compute safety number hash
        val hash = CryptoUtils.computeSafetyNumber(ourIdentityKey, theirIdentityKey)

        // Format for display (groups of 5 digits)
        val formatted = CryptoUtils.formatSafetyNumber(hash)

        return SafetyNumber(
            version = SAFETY_NUMBER_VERSION,
            hash = hash,
            formattedNumber = formatted,
            ourKeyFingerprint = CryptoUtils.toHex(CryptoUtils.sha256(ourIdentityKey)).take(16),
            theirKeyFingerprint = CryptoUtils.toHex(CryptoUtils.sha256(theirIdentityKey)).take(16)
        )
    }

    /**
     * Verify that two Safety Numbers match.
     * Returns true if the contact's identity is verified.
     */
    fun verifySafetyNumber(
        expected: SafetyNumber,
        actual: SafetyNumber
    ): Boolean {
        // Compare hashes using constant-time comparison
        val match = CryptoUtils.constantTimeEquals(expected.hash, actual.hash)

        if (!match) {
            Log.w(TAG, "Safety Number mismatch - possible MITM attack!")
        }

        return match
    }

    /**
     * Serialize Safety Number to JSON for storage.
     */
    fun toJson(safetyNumber: SafetyNumber): String {
        return JSONObject().apply {
            put("version", safetyNumber.version)
            put("hash", Base64.encodeToString(safetyNumber.hash, Base64.NO_WRAP))
            put("formatted", safetyNumber.formattedNumber)
            put("our_fingerprint", safetyNumber.ourKeyFingerprint)
            put("their_fingerprint", safetyNumber.theirKeyFingerprint)
        }.toString()
    }

    /**
     * Deserialize Safety Number from JSON.
     */
    fun fromJson(json: String): SafetyNumber? {
        return try {
            val obj = JSONObject(json)
            SafetyNumber(
                version = obj.getInt("version"),
                hash = Base64.decode(obj.getString("hash"), Base64.NO_WRAP),
                formattedNumber = obj.getString("formatted"),
                ourKeyFingerprint = obj.getString("our_fingerprint"),
                theirKeyFingerprint = obj.getString("their_fingerprint")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Safety Number", e)
            null
        }
    }

    /**
     * Generate QR code data for Safety Number verification.
     */
    fun toQrData(safetyNumber: SafetyNumber): String {
        return "vibe-safety:${safetyNumber.version}:${Base64.encodeToString(safetyNumber.hash, Base64.NO_WRAP)}"
    }

    /**
     * Parse QR code data to Safety Number.
     */
    fun fromQrData(qrData: String): SafetyNumber? {
        return try {
            val parts = qrData.split(":")
            if (parts.size != 3 || parts[0] != "vibe-safety") return null

            val version = parts[1].toInt()
            val hash = Base64.decode(parts[2], Base64.NO_WRAP)

            SafetyNumber(
                version = version,
                hash = hash,
                formattedNumber = CryptoUtils.formatSafetyNumber(hash),
                ourKeyFingerprint = "",
                theirKeyFingerprint = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse QR data", e)
            null
        }
    }
}

/**
 * Safety Number data class.
 */
data class SafetyNumber(
    val version: Int,
    val hash: ByteArray,
    val formattedNumber: String,
    val ourKeyFingerprint: String,
    val theirKeyFingerprint: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SafetyNumber) return false
        return version == other.version && CryptoUtils.constantTimeEquals(hash, other.hash)
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + hash.contentHashCode()
        return result
    }
}
