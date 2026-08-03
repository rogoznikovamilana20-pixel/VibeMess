package com.vibe.ui.e2e

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Smart Key Rotation system.
 * AI decides optimal key rotation timing based on:
 * - Device usage patterns
 * - Threat level
 * - Battery status
 * - Network conditions
 * - Security requirements
 */
class SmartKeyRotation {

    companion object {
        private const val TAG = "SmartKeyRotation"

        // Default rotation intervals
        private const val DEFAULT_ROTATION_INTERVAL_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
        private const val MIN_ROTATION_INTERVAL_MS = 1L * 24 * 60 * 60 * 1000 // 1 day
        private const val MAX_ROTATION_INTERVAL_MS = 90L * 24 * 60 * 60 * 1000 // 90 days

        // Threat-based intervals
        private const val HIGH_THREAT_INTERVAL_MS = 1L * 24 * 60 * 60 * 1000 // 1 day
        private const val MEDIUM_THREAT_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }

    // Key rotation timestamps
    private val lastRotationTimes = ConcurrentHashMap<String, AtomicLong>()

    // User behavior patterns
    private val usagePatterns = ConcurrentHashMap<String, UsagePattern>()

    /**
     * Calculate optimal rotation interval for a user.
     */
    fun calculateOptimalInterval(
        userId: String,
        threatLevel: ThreatLevel,
        isBatteryLow: Boolean,
        isOnWiFi: Boolean,
        deviceChangeFrequency: Int // device changes per month
    ): Long {
        var interval = DEFAULT_ROTATION_INTERVAL_MS

        // Adjust based on threat level
        interval = when (threatLevel) {
            ThreatLevel.CRITICAL -> HIGH_THREAT_INTERVAL_MS
            ThreatLevel.HIGH -> HIGH_THREAT_INTERVAL_MS
            ThreatLevel.MEDIUM -> MEDIUM_THREAT_INTERVAL_MS
            ThreatLevel.LOW -> interval / 2
            ThreatLevel.NONE -> interval
        }

        // Adjust based on device change frequency
        if (deviceChangeFrequency > 3) {
            // Frequent device changes = more frequent rotation
            interval = (interval * 0.5).toLong()
        } else if (deviceChangeFrequency == 0) {
            // No device changes = can rotate less frequently
            interval = (interval * 1.5).toLong()
        }

        // Adjust based on network conditions
        if (!isOnWiFi) {
            // Mobile network = reduce rotation to save data
            interval = (interval * 1.2).toLong()
        }

        // Adjust based on battery
        if (isBatteryLow) {
            // Low battery = defer rotation
            interval = (interval * 2).toLong()
        }

        // Clamp to limits
        return interval.coerceIn(MIN_ROTATION_INTERVAL_MS, MAX_ROTATION_INTERVAL_MS)
    }

    /**
     * Check if rotation is needed.
     */
    fun shouldRotateKey(
        userId: String,
        threatLevel: ThreatLevel,
        isBatteryLow: Boolean,
        isOnWiFi: Boolean
    ): RotationDecision {
        val lastRotation = lastRotationTimes[userId]?.get() ?: 0
        val now = System.currentTimeMillis()
        val timeSinceLastRotation = now - lastRotation

        val pattern = usagePatterns.getOrPut(userId) { UsagePattern() }
        val optimalInterval = calculateOptimalInterval(
            userId = userId,
            threatLevel = threatLevel,
            isBatteryLow = isBatteryLow,
            isOnWiFi = isOnWiFi,
            deviceChangeFrequency = pattern.deviceChangesPerMonth
        )

        val shouldRotate = timeSinceLastRotation >= optimalInterval
        val urgency = calculateUrgency(timeSinceLastRotation, optimalInterval, threatLevel)

        return RotationDecision(
            shouldRotate = shouldRotate,
            urgency = urgency,
            reason = getRotationReason(shouldRotate, urgency, threatLevel),
            nextRotationIn = (optimalInterval - timeSinceLastRotation).coerceAtLeast(0)
        )
    }

    /**
     * Record key rotation.
     */
    fun recordRotation(userId: String) {
        lastRotationTimes[userId] = AtomicLong(System.currentTimeMillis())
        Log.d(TAG, "Key rotation recorded for $userId")
    }

    /**
     * Record device change.
     */
    fun recordDeviceChange(userId: String) {
        val pattern = usagePatterns.getOrPut(userId) { UsagePattern() }
        pattern.deviceChangesPerMonth++
        pattern.lastDeviceChange = System.currentTimeMillis()
    }

    /**
     * Calculate rotation urgency.
     */
    private fun calculateUrgency(
        timeSinceLastRotation: Long,
        optimalInterval: Long,
        threatLevel: ThreatLevel
    ): RotationUrgency {
        val ratio = timeSinceLastRotation.toDouble() / optimalInterval

        return when {
            threatLevel == ThreatLevel.CRITICAL -> RotationUrgency.IMMEDIATE
            threatLevel == ThreatLevel.HIGH -> RotationUrgency.URGENT
            ratio > 2.0 -> RotationUrgency.URGENT
            ratio > 1.5 -> RotationUrgency.SHOULD
            ratio > 1.0 -> RotationUrgency.CAN
            else -> RotationUrgency.NOT_NEEDED
        }
    }

    /**
     * Get rotation reason.
     */
    private fun getRotationReason(
        shouldRotate: Boolean,
        urgency: RotationUrgency,
        threatLevel: ThreatLevel
    ): String {
        if (!shouldRotate) return "Ротация не требуется"

        return when {
            threatLevel == ThreatLevel.CRITICAL -> "КРИТИЧЕСКАЯ УГРОЗА - немедленная ротация"
            threatLevel == ThreatLevel.HIGH -> "Высокая угроза - рекомендуется ротация"
            urgency == RotationUrgency.URGENT -> "Превышены сроки ротации"
            urgency == RotationUrgency.SHOULD -> "Рекомендуется ротация ключей"
            else -> "Ротация доступна"
        }
    }

    /**
     * Get next rotation time.
     */
    fun getNextRotationTime(userId: String): Long {
        return lastRotationTimes[userId]?.get() ?: 0
    }

    /**
     * Export rotation data for persistence.
     */
    fun exportRotationData(): Map<String, Long> {
        return lastRotationTimes.mapValues { it.value.get() }
    }

    /**
     * Import rotation data from persistence.
     */
    fun importRotationData(data: Map<String, Long>) {
        data.forEach { (userId, timestamp) ->
            lastRotationTimes[userId] = AtomicLong(timestamp)
        }
    }
}

/**
 * Usage pattern for key rotation decisions.
 */
data class UsagePattern(
    var deviceChangesPerMonth: Int = 0,
    var lastDeviceChange: Long = 0,
    var averageSessionLength: Long = 0,
    var messagesPerDay: Int = 0
)

/**
 * Rotation decision.
 */
data class RotationDecision(
    val shouldRotate: Boolean,
    val urgency: RotationUrgency,
    val reason: String,
    val nextRotationIn: Long
)

/**
 * Rotation urgency levels.
 */
enum class RotationUrgency {
    NOT_NEEDED,
    CAN,
    SHOULD,
    URGENT,
    IMMEDIATE
}
