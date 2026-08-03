package com.vibe.ui.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Supabase Auth manager — email/password registration and login via REST API.
 * No SDK required — uses OkHttp directly.
 */
object SupabaseAuthManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class AuthResult(
        val success: Boolean,
        val token: String? = null,
        val refreshToken: String? = null,
        val userId: String? = null,
        val email: String? = null,
        val error: String? = null
    )

    suspend fun signUp(
        supabaseUrl: String,
        anonKey: String,
        email: String,
        password: String
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/signup")
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }

            if (response.isSuccessful) {
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val userId = json.optJSONObject("user")?.optString("id", "")
                val userEmail = json.optJSONObject("user")?.optString("email", "")
                if (accessToken.isNotEmpty()) {
                    AuthResult(true, token = accessToken, refreshToken = refreshToken, userId = userId, email = userEmail)
                } else {
                    AuthResult(false, error = "Регистрация требует подтверждения email. Проверьте почту.")
                }
            } else {
                val errorMsg = json.optString("msg", json.optString("error_description", "Ошибка регистрации"))
                AuthResult(false, error = errorMsg)
            }
        } catch (e: Exception) {
            AuthResult(false, error = "Ошибка сети: ${e.localizedMessage}")
        }
    }

    suspend fun signIn(
        supabaseUrl: String,
        anonKey: String,
        email: String,
        password: String
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=password")
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }

            if (response.isSuccessful) {
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val userId = json.optJSONObject("user")?.optString("id", "")
                val userEmail = json.optJSONObject("user")?.optString("email", "")
                if (accessToken.isNotEmpty()) {
                    AuthResult(true, token = accessToken, refreshToken = refreshToken, userId = userId, email = userEmail)
                } else {
                    AuthResult(false, error = "Ошибка: токен не получен")
                }
            } else {
                val errorMsg = json.optString("msg", json.optString("error_description", "Неверный email или пароль"))
                AuthResult(false, error = errorMsg)
            }
        } catch (e: Exception) {
            AuthResult(false, error = "Ошибка сети: ${e.localizedMessage}")
        }
    }

    suspend fun refreshSession(supabaseUrl: String, anonKey: String, refreshToken: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply { put("refresh_token", refreshToken) }
                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/token?grant_type=refresh_token")
                    .addHeader("apikey", anonKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType())).build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }

                if (response.isSuccessful) {
                    val accessToken = json.optString("access_token", "")
                    val userId = json.optJSONObject("user")?.optString("id", "")
                    if (accessToken.isNotEmpty()) {
                        AuthResult(true, token = accessToken, userId = userId)
                    } else {
                        AuthResult(false, error = "Ошибка обновления токена")
                    }
                } else {
                    AuthResult(false, error = "Сессия истекла, войдите заново")
                }
            } catch (e: Exception) {
                AuthResult(false, error = "Ошибка сети: ${e.localizedMessage}")
            }
        }
}
