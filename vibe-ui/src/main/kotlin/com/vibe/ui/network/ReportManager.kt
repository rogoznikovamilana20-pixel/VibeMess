package com.vibe.ui.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manages report (complaint) submissions for UGC moderation.
 * Reports are stored in Supabase via Edge Function or direct insert.
 */
object ReportManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    enum class ReportReason(val label: String) {
        SPAM("Спам"),
        HARASSMENT("Нежелательные преследования"),
        VIOLENCE("Насилие"),
        HATE_SPEECH("Разжигание ненависти"),
        NUDITY("Непристойный контент"),
        OTHER("Другое")
    }

    data class ReportResult(
        val success: Boolean,
        val error: String? = null
    )

    suspend fun reportMessage(
        supabaseUrl: String,
        anonKey: String,
        authToken: String,
        messageId: String,
        reason: ReportReason,
        description: String = ""
    ): ReportResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("message_id", messageId)
                put("reason", reason.label)
                put("description", description)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/message_reports")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 201) {
                ReportResult(true)
            } else {
                ReportResult(false, "Ошибка отправки: ${response.code}")
            }
        } catch (e: Exception) {
            ReportResult(false, "Ошибка сети: ${e.localizedMessage}")
        }
    }

    suspend fun blockUser(
        context: Context,
        userId: String
    ): Boolean {
        val prefs = context.getSharedPreferences("vibe_blocked_users", Context.MODE_PRIVATE)
        val blocked = prefs.getStringSet("blocked_ids", emptySet()) ?: emptySet()
        prefs.edit().putStringSet("blocked_ids", blocked + userId).apply()
        return true
    }

    suspend fun unblockUser(
        context: Context,
        userId: String
    ): Boolean {
        val prefs = context.getSharedPreferences("vibe_blocked_users", Context.MODE_PRIVATE)
        val blocked = prefs.getStringSet("blocked_ids", emptySet()) ?: emptySet()
        prefs.edit().putStringSet("blocked_ids", blocked - userId).apply()
        return true
    }

    fun isBlocked(context: Context, userId: String): Boolean {
        val prefs = context.getSharedPreferences("vibe_blocked_users", Context.MODE_PRIVATE)
        val blocked = prefs.getStringSet("blocked_ids", emptySet()) ?: emptySet()
        return userId in blocked
    }
}
