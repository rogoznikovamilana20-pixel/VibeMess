package com.vibe.ui.monitoring

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.vibe.common.logging.VibeLogger

/**
 * Centralized analytics manager for tracking user behavior and app performance.
 * Uses Firebase Analytics for production analytics.
 */
class AnalyticsManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AnalyticsManager? = null

        fun getInstance(context: Context): AnalyticsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnalyticsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    /**
     * Track a screen view event.
     * @param screenName The name of the screen
     */
    fun trackScreenView(screenName: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            VibeLogger.d("Analytics", "Screen view: $screenName")
        } catch (e: Exception) {
            VibeLogger.e("Analytics", "Failed to track screen view: $screenName", e)
        }
    }

    /**
     * Track a user action event.
     * @param eventName The name of the event
     * @param params Optional parameters for the event
     */
    fun trackEvent(eventName: String, params: Map<String, Any>? = null) {
        try {
            val bundle = Bundle()
            params?.forEach { (key, value) ->
                when (value) {
                    is String -> bundle.putString(key, value)
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                }
            }
            firebaseAnalytics.logEvent(eventName, bundle)
            VibeLogger.d("Analytics", "Event: $eventName, params: $params")
        } catch (e: Exception) {
            VibeLogger.e("Analytics", "Failed to track event: $eventName", e)
        }
    }

    /**
     * Track a chat interaction.
     * @param chatId The ID of the chat
     * @param action The action performed (e.g., "message_sent", "chat_opened")
     */
    fun trackChatInteraction(chatId: Long, action: String) {
        trackEvent("chat_interaction", mapOf(
            "chat_id" to chatId,
            "action" to action
        ))
    }

    /**
     * Track a message sent event.
     * @param chatId The ID of the chat
     * @param messageType The type of message (e.g., "text", "image", "voice")
     */
    fun trackMessageSent(chatId: Long, messageType: String) {
        trackEvent("message_sent", mapOf(
            "chat_id" to chatId,
            "message_type" to messageType
        ))
    }

    /**
     * Track an error event.
     * @param errorType The type of error
     * @param errorMessage The error message
     */
    fun trackError(errorType: String, errorMessage: String) {
        trackEvent("error", mapOf(
            "error_type" to errorType,
            "error_message" to errorMessage
        ))
    }

    /**
     * Track app performance metrics.
     * @param metricName The name of the metric
     * @param value The value of the metric
     */
    fun trackPerformance(metricName: String, value: Long) {
        trackEvent("performance", mapOf(
            "metric_name" to metricName,
            "value" to value
        ))
    }

    /**
     * Set user property for segmentation.
     * @param propertyName The name of the property
     * @param value The value of the property
     */
    fun setUserProperty(propertyName: String, value: String) {
        try {
            firebaseAnalytics.setUserProperty(propertyName, value)
            VibeLogger.d("Analytics", "User property: $propertyName = $value")
        } catch (e: Exception) {
            VibeLogger.e("Analytics", "Failed to set user property: $propertyName", e)
        }
    }

    /**
     * Set user ID for analytics.
     * @param userId The user ID
     */
    fun setUserId(userId: String) {
        try {
            firebaseAnalytics.setUserId(userId)
            VibeLogger.d("Analytics", "User ID set: $userId")
        } catch (e: Exception) {
            VibeLogger.e("Analytics", "Failed to set user ID", e)
        }
    }

    /**
     * Reset analytics data (for logout).
     */
    fun resetAnalytics() {
        try {
            firebaseAnalytics.resetAnalyticsData()
            VibeLogger.d("Analytics", "Analytics data reset")
        } catch (e: Exception) {
            VibeLogger.e("Analytics", "Failed to reset analytics", e)
        }
    }
}
