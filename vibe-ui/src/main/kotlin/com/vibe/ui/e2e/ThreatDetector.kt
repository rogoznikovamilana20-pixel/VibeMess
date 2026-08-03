package com.vibe.ui.e2e

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * AI-powered Threat Detection system.
 * Monitors for suspicious activity in real-time.
 *
 * Detects:
 * - Mass key requests (network scanning)
 * - Rapid key rotation (possible attack)
 * - Unusual message patterns (compromise)
 * - Brute force attempts
 * - Anomalous behavior
 */
class ThreatDetector {

    companion object {
        private const val TAG = "ThreatDetector"
        private const val RATE_LIMIT_WINDOW_MS = 60_000L // 1 minute
        private const val MAX_KEY_REQUESTS_PER_MINUTE = 10
        private const val MAX_KEY_ROTATIONS_PER_DAY = 5
        private const val MAX_FAILED_DECRYPTIONS = 3
        private const val ANOMALY_THRESHOLD = 0.7
    }

    // Rate limiting
    private val keyRequestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val keyRequestTimestamps = ConcurrentHashMap<String, AtomicLong>()

    // Key rotation tracking
    private val keyRotationCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val keyRotationDates = ConcurrentHashMap<String, String>()

    // Failed decryption tracking
    private val failedDecryptionCounts = ConcurrentHashMap<String, AtomicInteger>()

    // Anomaly detection
    private val messagePatterns = ConcurrentHashMap<String, MessagePattern>()

    /**
     * Check if a key request is suspicious.
     */
    fun isKeyRequestSuspicious(userId: String, contactId: String): ThreatLevel {
        val now = System.currentTimeMillis()
        val windowStart = now - RATE_LIMIT_WINDOW_MS

        // Clean old timestamps
        keyRequestTimestamps[contactId]?.let { lastTime ->
            if (lastTime.get() < windowStart) {
                keyRequestCounts[contactId]?.set(0)
                keyRequestTimestamps[contactId]?.set(now)
            }
        }

        // Increment count
        val count = keyRequestCounts.getOrPut(contactId) { AtomicInteger(0) }
        val newCount = count.incrementAndGet()

        // Update timestamp
        keyRequestTimestamps.getOrPut(contactId) { AtomicLong(now) }.set(now)

        // Check rate limit
        if (newCount > MAX_KEY_REQUESTS_PER_MINUTE) {
            Log.w(TAG, "Suspicious key request rate from $contactId: $newCount/min")
            return ThreatLevel.HIGH
        }

        return ThreatLevel.NONE
    }

    /**
     * Track own key rotation (does NOT trigger threat detection).
     */
    fun trackOwnKeyRotation(userId: String) {
        val today = java.time.LocalDate.now().toString()
        if (keyRotationDates[userId] != today) {
            keyRotationCounts[userId]?.set(0)
            keyRotationDates[userId] = today
        }
        keyRotationCounts.getOrPut(userId) { AtomicInteger(0) }.incrementAndGet()
    }

    /**
     * Check if key rotation is suspicious (only for OTHER users, not self).
     */
    fun isKeyRotationSuspicious(userId: String): ThreatLevel {
        val today = java.time.LocalDate.now().toString()

        // Reset count if new day
        if (keyRotationDates[userId] != today) {
            keyRotationCounts[userId]?.set(0)
            keyRotationDates[userId] = today
        }

        val count = keyRotationCounts.getOrPut(userId) { AtomicInteger(0) }
        val newCount = count.incrementAndGet()

        if (newCount > MAX_KEY_ROTATIONS_PER_DAY) {
            Log.w(TAG, "Suspicious key rotation frequency: $newCount/day")
            return ThreatLevel.HIGH
        }

        if (newCount > MAX_KEY_ROTATIONS_PER_DAY / 2) {
            return ThreatLevel.MEDIUM
        }

        return ThreatLevel.NONE
    }

    /**
     * Track failed decryption attempt.
     */
    fun trackFailedDecryption(userId: String, contactId: String): ThreatLevel {
        val key = "${userId}_${contactId}"
        val count = failedDecryptionCounts.getOrPut(key) { AtomicInteger(0) }
        val newCount = count.incrementAndGet()

        if (newCount >= MAX_FAILED_DECRYPTIONS) {
            Log.w(TAG, "Multiple failed decryptions from $contactId: $newCount")
            return ThreatLevel.HIGH
        }

        if (newCount >= MAX_FAILED_DECRYPTIONS / 2) {
            return ThreatLevel.MEDIUM
        }

        return ThreatLevel.NONE
    }

    /**
     * Analyze message pattern for anomalies.
     */
    fun analyzeMessagePattern(
        userId: String,
        contactId: String,
        messageSize: Int,
        timestamp: Long
    ): ThreatLevel {
        val key = "${userId}_${contactId}"
        val pattern = messagePatterns.getOrPut(key) { MessagePattern() }

        // Update pattern
        pattern.totalMessages++
        pattern.totalSize += messageSize
        pattern.lastMessageTime = timestamp

        // Check for anomalies
        val avgSize = if (pattern.totalMessages > 0) pattern.totalSize / pattern.totalMessages else 0
        val sizeDeviation = if (avgSize > 0) Math.abs(messageSize - avgSize).toDouble() / avgSize else 0.0

        // Unusual message size
        if (sizeDeviation > 3.0 && pattern.totalMessages > 10) {
            Log.w(TAG, "Unusual message size from $contactId: $sizeDeviation deviation")
            return ThreatLevel.MEDIUM
        }

        // Unusual timing (messages at 3 AM etc)
        val hour = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .hour
        if (hour in 2..5 && pattern.totalMessages > 5) {
            return ThreatLevel.LOW
        }

        return ThreatLevel.NONE
    }

    /**
     * Get overall threat level for a contact.
     */
    fun getOverallThreatLevel(userId: String, contactId: String): ThreatResult {
        val threats = mutableListOf<ThreatInfo>()

        // Check all threat vectors
        val keyRequestThreat = isKeyRequestSuspicious(userId, contactId)
        if (keyRequestThreat != ThreatLevel.NONE) {
            threats.add(ThreatInfo("key_request_rate", keyRequestThreat, "Частые запросы ключей"))
        }

        val keyRotationThreat = isKeyRotationSuspicious(userId)
        if (keyRotationThreat != ThreatLevel.NONE) {
            threats.add(ThreatInfo("key_rotation", keyRotationThreat, "Частая смена ключей"))
        }

        val decryptionThreat = failedDecryptionCounts["${userId}_${contactId}"]?.let {
            if (it.get() >= MAX_FAILED_DECRYPTIONS / 2) ThreatLevel.MEDIUM else ThreatLevel.NONE
        } ?: ThreatLevel.NONE
        if (decryptionThreat != ThreatLevel.NONE) {
            threats.add(ThreatInfo("decryption_failure", decryptionThreat, "Ошибки расшифровки"))
        }

        // Calculate overall level
        val maxLevel = threats.maxByOrNull { it.level.ordinal }?.level ?: ThreatLevel.NONE

        return ThreatResult(
            level = maxLevel,
            threats = threats,
            recommendation = getRecommendation(maxLevel)
        )
    }

    /**
     * Get security recommendation based on threat level.
     */
    private fun getRecommendation(level: ThreatLevel): String {
        return when (level) {
            ThreatLevel.NONE -> "Всё в порядке"
            ThreatLevel.LOW -> "Обнаружена подозрительная активность. Рекомендуется проверка."
            ThreatLevel.MEDIUM -> "Высокая подозрительная активность. Рекомендуется смена ключей."
            ThreatLevel.HIGH -> "ВОЗМОЖНА АТАКА! Рекомендуется немедленная верификация контакта."
            ThreatLevel.CRITICAL -> "КРИТИЧЕСКАЯ УГРОЗА! Свяжитесь с поддержкой."
        }
    }

    /**
     * Reset tracking for a contact (after manual verification).
     */
    fun resetTracking(userId: String, contactId: String) {
        keyRequestCounts.remove(contactId)
        keyRequestTimestamps.remove(contactId)
        failedDecryptionCounts.remove("${userId}_${contactId}")
        messagePatterns.remove("${userId}_${contactId}")
        Log.d(TAG, "Tracking reset for $contactId")
    }

    /**
     * Export threat data for analysis.
     */
    fun exportThreatData(): Map<String, Any> {
        return mapOf(
            "key_requests" to keyRequestCounts.mapValues { it.value.get() },
            "key_rotations" to keyRotationCounts.mapValues { it.value.get() },
            "failed_decryptions" to failedDecryptionCounts.mapValues { it.value.get() }
        )
    }
}

/**
 * Threat levels.
 */
enum class ThreatLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Threat information.
 */
data class ThreatInfo(
    val type: String,
    val level: ThreatLevel,
    val description: String
)

/**
 * Threat detection result.
 */
data class ThreatResult(
    val level: ThreatLevel,
    val threats: List<ThreatInfo>,
    val recommendation: String
)

/**
 * Message pattern for anomaly detection.
 */
data class MessagePattern(
    var totalMessages: Int = 0,
    var totalSize: Long = 0,
    var lastMessageTime: Long = 0
)
