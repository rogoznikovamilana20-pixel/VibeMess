package com.vibe.ui.call

import java.security.MessageDigest
import java.security.SecureRandom

class E2eeManager {

    private var localFingerprint: String = ""
    private var remoteFingerprint: String = ""
    private var safetyNumber: String = ""
    private var isVerified: Boolean = false

    data class VerificationResult(
        val verified: Boolean,
        val localFingerprint: String,
        val remoteFingerprint: String,
        val safetyNumber: String
    )

    fun setLocalFingerprint(sdp: String) {
        localFingerprint = extractFingerprint(sdp) ?: ""
        recalculateSafetyNumber()
    }

    fun setRemoteFingerprint(sdp: String) {
        remoteFingerprint = extractFingerprint(sdp) ?: ""
        recalculateSafetyNumber()
    }

    private fun recalculateSafetyNumber() {
        if (localFingerprint.isNotEmpty() && remoteFingerprint.isNotEmpty()) {
            safetyNumber = generateSafetyNumber(localFingerprint, remoteFingerprint)
        }
    }

    fun verify(): VerificationResult {
        return VerificationResult(
            verified = isVerified,
            localFingerprint = localFingerprint,
            remoteFingerprint = remoteFingerprint,
            safetyNumber = safetyNumber
        )
    }

    fun confirmVerification() {
        isVerified = safetyNumber.isNotEmpty()
    }

    fun resetVerification() {
        isVerified = false
    }

    fun getLocalFingerprint(): String = localFingerprint
    fun getRemoteFingerprint(): String = remoteFingerprint
    fun getSafetyNumber(): String = safetyNumber
    fun isVerified(): Boolean = isVerified

    companion object {
        private const val TAG = "E2eeManager"

        fun extractFingerprint(sdp: String): String? {
            val regex = "a=fingerprint:sha-256 ([A-Fa-f0-9:]+)".toRegex()
            return regex.find(sdp)?.groupValues?.getOrNull(1)?.uppercase()
        }

        fun generateSafetyNumber(local: String, remote: String): String {
            if (local.isEmpty() || remote.isEmpty()) return ""
            val sorted = listOf(local, remote).sorted()
            val combined = sorted[0] + sorted[1]
            val digest = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray())
            val hex = digest.joinToString("") { String.format("%02x", it) }.uppercase()
            return hex.chunked(8).joinToString("-").take(47)
        }

        fun generateShortCode(fingerprint: String): String {
            if (fingerprint.isEmpty()) return ""
            val clean = fingerprint.replace(":", "")
            if (clean.length < 8) return ""
            val bytes = clean.toByteArray()
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            val code = digest.take(4).joinToString("") { String.format("%02x", it) }
            return code.chunked(4).joinToString("-").uppercase()
        }

        fun generateVerificationDigits(): String {
            val random = SecureRandom()
            val digits = (1..6).map { random.nextInt(10) }
            return digits.joinToString("")
        }

        fun formatFingerprint(fingerprint: String): String {
            return fingerprint.chunked(16).joinToString("\n")
        }
    }
}
