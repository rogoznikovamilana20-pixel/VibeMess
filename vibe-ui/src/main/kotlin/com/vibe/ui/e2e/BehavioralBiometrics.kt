package com.vibe.ui.e2e

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Behavioral Biometrics system.
 * Learns user's typing patterns to detect account compromise.
 *
 * Features:
 * - Typing speed analysis
 * - Pause pattern detection
 * - Error rate monitoring
 * - Active hours profiling
 * - Device behavior fingerprinting
 */
class BehavioralBiometrics {

    companion object {
        private const val TAG = "BehavioralBiometrics"
        private const val MIN_SAMPLES_FOR_PROFILE = 50
        private const val CONFIDENCE_THRESHOLD = 0.8
        private const val ANOMALY_THRESHOLD = 2.0 // Standard deviations
    }

    // User profiles per contact (who they're talking to)
    private val userProfiles = ConcurrentHashMap<String, UserProfile>()

    // Current session data
    private val currentSession = SessionData()

    /**
     * Record a typing event.
     */
    fun recordTypingEvent(
        userId: String,
        contactId: String,
        keyDownTime: Long,
        keyUpTime: Long,
        isSpecialKey: Boolean = false
    ) {
        val duration = keyUpTime - keyDownTime
        val pattern = userProfiles.getOrPut("${userId}_${contactId}") { UserProfile() }

        pattern.typingSpeeds.add(duration)
        if (pattern.typingSpeeds.size > 100) {
            pattern.typingSpeeds.removeAt(0)
        }

        // Update average
        pattern.avgTypingSpeed = pattern.typingSpeeds.average()
        pattern.stdDevTypingSpeed = calculateStdDev(pattern.typingSpeeds)
    }

    /**
     * Record a pause between words.
     */
    fun recordPause(
        userId: String,
        contactId: String,
        pauseDuration: Long
    ) {
        val pattern = userProfiles.getOrPut("${userId}_${contactId}") { UserProfile() }

        pattern.pauseDurations.add(pauseDuration)
        if (pattern.pauseDurations.size > 50) {
            pattern.pauseDurations.removeAt(0)
        }

        pattern.avgPauseDuration = pattern.pauseDurations.average()
    }

    /**
     * Record message sending time.
     */
    fun recordMessageTime(
        userId: String,
        contactId: String,
        hour: Int
    ) {
        val pattern = userProfiles.getOrPut("${userId}_${contactId}") { UserProfile() }

        pattern.activeHours.add(hour)
        if (pattern.activeHours.size > 200) {
            pattern.activeHours.removeAt(0)
        }

        // Update active hours distribution
        pattern.hourDistribution[hour] = (pattern.hourDistribution[hour] ?: 0) + 1
    }

    /**
     * Record emoji usage.
     */
    fun recordEmojiUsage(
        userId: String,
        contactId: String,
        emojiCount: Int,
        messageLength: Int
    ) {
        val pattern = userProfiles.getOrPut("${userId}_${contactId}") { UserProfile() }

        val emojiRatio = if (messageLength > 0) emojiCount.toDouble() / messageLength else 0.0
        pattern.emojiRatios.add(emojiRatio)
        if (pattern.emojiRatios.size > 100) {
            pattern.emojiRatios.removeAt(0)
        }

        pattern.avgEmojiRatio = pattern.emojiRatios.average()
    }

    /**
     * Analyze if current behavior matches user profile.
     */
    fun analyzeBehavior(
        userId: String,
        contactId: String,
        currentTypingSpeed: Long,
        currentPause: Long,
        currentHour: Int,
        currentEmojiRatio: Double
    ): BiometricResult {
        val profile = userProfiles["${userId}_${contactId}"]

        // Not enough data yet — don't auto-approve
        if (profile == null || profile.typingSpeeds.size < MIN_SAMPLES_FOR_PROFILE) {
            return BiometricResult(
                isMatch = false,
                confidence = 0.0,
                anomalies = listOf("Недостаточно данных для профиля")
            )
        }

        val anomalies = mutableListOf<String>()
        var totalScore = 0.0
        var factors = 0

        // Check typing speed
        val typingDeviation = abs(currentTypingSpeed - profile.avgTypingSpeed) / 
            if (profile.stdDevTypingSpeed > 0) profile.stdDevTypingSpeed else 1.0
        if (typingDeviation > ANOMALY_THRESHOLD) {
            anomalies.add("Необычная скорость печати: ${String.format("%.1f", typingDeviation)}σ")
        }
        totalScore += if (typingDeviation > ANOMALY_THRESHOLD) 0.0 else 1.0
        factors++

        // Check pause pattern
        val pauseDeviation = abs(currentPause - profile.avgPauseDuration) / 
            if (profile.avgPauseDuration > 0) profile.avgPauseDuration else 1.0
        if (pauseDeviation > ANOMALY_THRESHOLD) {
            anomalies.add("Необычные паузы: ${String.format("%.1f", pauseDeviation)}σ")
        }
        totalScore += if (pauseDeviation > ANOMALY_THRESHOLD) 0.0 else 1.0
        factors++

        // Check active hours
        val hourFreq = profile.hourDistribution[currentHour] ?: 0
        val totalHourMessages = profile.hourDistribution.values.sum()
        val hourProbability = if (totalHourMessages > 0) hourFreq.toDouble() / totalHourMessages else 0.0
        if (hourProbability < 0.05 && profile.activeHours.size > 50) {
            anomalies.add("Необычное время активности: ${currentHour}:00")
        }
        totalScore += if (hourProbability < 0.05) 0.0 else 1.0
        factors++

        // Check emoji ratio
        val emojiDeviation = abs(currentEmojiRatio - profile.avgEmojiRatio) / 
            if (profile.avgEmojiRatio > 0) profile.avgEmojiRatio else 1.0
        if (emojiDeviation > ANOMALY_THRESHOLD && profile.emojiRatios.size > 20) {
            anomalies.add("Необычное использование эмодзи")
        }
        totalScore += if (emojiDeviation > ANOMALY_THRESHOLD) 0.0 else 1.0
        factors++

        val confidence = if (factors > 0) totalScore / factors else 0.5

        return BiometricResult(
            isMatch = confidence >= CONFIDENCE_THRESHOLD,
            confidence = confidence,
            anomalies = anomalies
        )
    }

    /**
     * Get user profile for display.
     */
    fun getUserProfile(userId: String, contactId: String): UserProfile? {
        return userProfiles["${userId}_${contactId}"]
    }

    /**
     * Check if we have enough data for analysis.
     */
    fun hasEnoughData(userId: String, contactId: String): Boolean {
        val profile = userProfiles["${userId}_${contactId}"] ?: return false
        return profile.typingSpeeds.size >= MIN_SAMPLES_FOR_PROFILE
    }

    /**
     * Export profile data for persistence.
     */
    fun exportProfile(userId: String, contactId: String): String? {
        val profile = userProfiles["${userId}_${contactId}"] ?: return null
        return org.json.JSONObject().apply {
            put("avgTypingSpeed", profile.avgTypingSpeed)
            put("stdDevTypingSpeed", profile.stdDevTypingSpeed)
            put("avgPauseDuration", profile.avgPauseDuration)
            put("avgEmojiRatio", profile.avgEmojiRatio)
            put("sampleCount", profile.typingSpeeds.size)
        }.toString()
    }

    /**
     * Calculate standard deviation.
     */
    private fun calculateStdDev(values: List<Long>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    /**
     * Reset profile for a contact.
     */
    fun resetProfile(userId: String, contactId: String) {
        userProfiles.remove("${userId}_${contactId}")
    }
}

/**
 * User behavioral profile.
 */
data class UserProfile(
    val typingSpeeds: MutableList<Long> = mutableListOf(),
    val pauseDurations: MutableList<Long> = mutableListOf(),
    val activeHours: MutableList<Int> = mutableListOf(),
    val emojiRatios: MutableList<Double> = mutableListOf(),
    val hourDistribution: MutableMap<Int, Int> = mutableMapOf(),
    var avgTypingSpeed: Double = 0.0,
    var stdDevTypingSpeed: Double = 0.0,
    var avgPauseDuration: Double = 0.0,
    var avgEmojiRatio: Double = 0.0
)

/**
 * Biometric analysis result.
 */
data class BiometricResult(
    val isMatch: Boolean,
    val confidence: Double,
    val anomalies: List<String>
)

/**
 * Current session data.
 */
data class SessionData(
    var messageCount: Int = 0,
    var startTime: Long = System.currentTimeMillis(),
    var lastActivity: Long = System.currentTimeMillis()
)
