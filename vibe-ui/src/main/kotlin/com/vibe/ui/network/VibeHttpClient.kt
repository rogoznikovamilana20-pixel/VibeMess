package com.vibe.ui.network

import com.vibe.common.logging.VibeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class VibeHttpClient(private val serverConfig: ServerConfig) {

    private val tag = "VibeHttpClient"

    private suspend fun post(endpoint: String, body: JSONObject, useRust: Boolean = false): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = if (useRust) serverConfig.getRustServerUrl() else serverConfig.getServerUrl()
                val url = URL("$baseUrl/$endpoint")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    serverConfig.getAuthToken().takeIf { it.isNotEmpty() }?.let {
                        setRequestProperty("Authorization", "Bearer $it")
                    }
                    doOutput = true
                    connectTimeout = 30000
                    readTimeout = 60000
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body.toString())
                }

                val responseCode = connection.responseCode
                val responseBody = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                }

                VibeLogger.d(tag, "POST $endpoint -> $responseCode")

                if (responseBody != null && responseBody.isNotEmpty()) {
                    JSONObject(responseBody)
                } else {
                    null
                }
            } catch (e: Exception) {
                VibeLogger.e(tag, "POST $endpoint failed", e)
                null
            }
        }
    }

    private suspend fun get(endpoint: String, useRust: Boolean = false): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = if (useRust) serverConfig.getRustServerUrl() else serverConfig.getServerUrl()
                val url = URL("$baseUrl/$endpoint")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    serverConfig.getAuthToken().takeIf { it.isNotEmpty() }?.let {
                        setRequestProperty("Authorization", "Bearer $it")
                    }
                    connectTimeout = 30000
                    readTimeout = 60000
                }

                val responseCode = connection.responseCode
                val responseBody = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                }

                VibeLogger.d(tag, "GET $endpoint -> $responseCode")

                if (responseBody != null && responseBody.isNotEmpty()) {
                    JSONObject(responseBody)
                } else {
                    null
                }
            } catch (e: Exception) {
                VibeLogger.e(tag, "GET $endpoint failed", e)
                null
            }
        }
    }

    // ============ Python Server API (legacy, для AuthActivity) ============

    suspend fun requestVerificationCode(identifier: String): String? {
        val body = JSONObject().apply {
            put("type", "request_code")
            put("identifier", identifier)
        }
        val response = post("request_code", body)
        return response?.optString("code")
    }

    suspend fun verifyCode(identifier: String, code: String): AuthResponse {
        val body = JSONObject().apply {
            put("type", "verify_code")
            put("identifier", identifier)
            put("code", code)
        }
        val response = post("verify_code", body)

        return if (response?.optString("status") == "ok") {
            AuthResponse(
                success = true,
                userId = response.optString("userId"),
                vibeId = response.optString("vibeId"),
                token = response.optString("token")
            )
        } else {
            AuthResponse(
                success = false,
                error = response?.optString("message") ?: "Unknown error"
            )
        }
    }

    suspend fun getChats(): List<ChatResponse> {
        val body = JSONObject().apply {
            put("type", "get_chats")
            put("userId", serverConfig.getUserId())
        }
        val response = post("get_chats", body)
        val chatsArray = response?.optJSONArray("chats") ?: return emptyList()

        return (0 until chatsArray.length()).map { i ->
            val chatJson = chatsArray.getJSONObject(i)
            ChatResponse(
                id = chatJson.optString("id"),
                title = chatJson.optString("title"),
                type = chatJson.optString("type")
            )
        }
    }

    suspend fun createChat(title: String, type: String = "private"): ChatResponse? {
        val body = JSONObject().apply {
            put("type", "create_chat")
            put("userId", serverConfig.getUserId())
            put("title", title)
            put("type", type)
        }
        val response = post("create_chat", body)
        val chatJson = response?.optJSONObject("chat")

        return chatJson?.let {
            ChatResponse(
                id = it.optString("id"),
                title = it.optString("title"),
                type = it.optString("type")
            )
        }
    }

    suspend fun getMessages(chatId: String): List<MessageResponse> {
        val body = JSONObject().apply {
            put("type", "get_history")
            put("chatId", chatId)
        }
        val response = post("get_history", body)
        val messagesArray = response?.optJSONArray("messages") ?: return emptyList()

        return (0 until messagesArray.length()).map { i ->
            val msgJson = messagesArray.getJSONObject(i)
            MessageResponse(
                id = msgJson.optString("id"),
                chatId = msgJson.optString("chat_id"),
                senderId = msgJson.optString("sender_id"),
                content = msgJson.optString("content"),
                type = msgJson.optString("type"),
                createdAt = msgJson.optString("created_at")
            )
        }
    }

    suspend fun sendMessage(chatId: String, content: String, type: String = "text"): MessageResponse? {
        val body = JSONObject().apply {
            put("type", "msg")
            put("userId", serverConfig.getUserId())
            put("chatId", chatId)
            put("content", content)
            put("messageType", type)
        }
        val response = post("msg", body)
        val msgJson = response?.optJSONObject("message")

        return msgJson?.let {
            MessageResponse(
                id = it.optString("id"),
                chatId = it.optString("chat_id"),
                senderId = it.optString("sender_id"),
                content = it.optString("content"),
                type = it.optString("type"),
                createdAt = it.optString("created_at")
            )
        }
    }

    suspend fun storeKey(keyType: String, publicKey: String): Boolean {
        val body = JSONObject().apply {
            put("type", "store_key")
            put("userId", serverConfig.getUserId())
            put("keyType", keyType)
            put("publicKey", publicKey)
        }
        val response = post("store_key", body)
        return response?.optString("status") == "ok"
    }

    suspend fun getKey(userId: String, keyType: String = "identity"): String? {
        val body = JSONObject().apply {
            put("type", "get_key")
            put("userId", userId)
            put("keyType", keyType)
        }
        val response = post("get_key", body)
        return response?.optString("publicKey")
    }

    // ============ Rust Server API ============

    suspend fun rustRegister(username: String, email: String, password: String): Result<String> {
        val body = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
        }
        return try {
            val response = post("api/auth/register", body, useRust = true)
            response?.let {
                val token = it.optString("token", "")
                if (token.isNotEmpty()) {
                    serverConfig.setAuthToken(token)
                    Result.success(token)
                } else {
                    Result.failure(Exception(it.optString("error", "Registration failed")))
                }
            } ?: Result.failure(Exception("No response from server"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rustLogin(email: String, password: String): Result<String> {
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        return try {
            val response = post("api/auth/login", body, useRust = true)
            response?.let {
                val token = it.optString("token", "")
                if (token.isNotEmpty()) {
                    serverConfig.setAuthToken(token)
                    Result.success(token)
                } else {
                    Result.failure(Exception(it.optString("error", "Login failed")))
                }
            } ?: Result.failure(Exception("No response from server"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rustGetChats(): List<ChatResponse> {
        return try {
            val response = get("api/chats", useRust = true)
            val chatsArray = response?.optJSONArray("chats") ?: return emptyList()
            (0 until chatsArray.length()).map { i ->
                val c = chatsArray.getJSONObject(i)
                ChatResponse(
                    id = c.optString("id", c.optLong("id", 0).toString()),
                    title = c.optString("name", c.optString("title", "")),
                    type = if (c.optBoolean("is_group", false)) "group" else "private"
                )
            }
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustGetChats failed", e)
            emptyList()
        }
    }

    suspend fun rustSendMessage(chatId: String, content: String): MessageResponse? {
        val body = JSONObject().apply {
            put("content", content)
        }
        return try {
            val response = post("api/chats/$chatId/messages", body, useRust = true)
            response?.let {
                val msg = it.optJSONObject("message") ?: it
                MessageResponse(
                    id = msg.optString("id", msg.optLong("id", 0).toString()),
                    chatId = chatId,
                    senderId = msg.optString("sender_id", serverConfig.getUserId()),
                    content = msg.optString("content", ""),
                    type = "text",
                    createdAt = msg.optString("created_at", System.currentTimeMillis().toString())
                )
            }
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustSendMessage failed", e)
            null
        }
    }

    suspend fun rustGetMessages(chatId: String, limit: Int = 50): List<MessageResponse> {
        return try {
            val response = get("api/chats/$chatId/messages?limit=$limit", useRust = true)
            val messagesArray = response?.optJSONArray("messages") ?: return emptyList()
            (0 until messagesArray.length()).map { i ->
                val m = messagesArray.getJSONObject(i)
                MessageResponse(
                    id = m.optString("id", m.optLong("id", 0).toString()),
                    chatId = chatId,
                    senderId = m.optString("sender_id", ""),
                    content = m.optString("content", ""),
                    type = m.optString("type", "text"),
                    createdAt = m.optString("created_at", "")
                )
            }
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustGetMessages failed", e)
            emptyList()
        }
    }
}

data class AuthResponse(
    val success: Boolean,
    val userId: String = "",
    val vibeId: String = "",
    val token: String = "",
    val error: String? = null
)

data class ChatResponse(
    val id: String,
    val title: String,
    val type: String
)

data class MessageResponse(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val type: String,
    val createdAt: String
)
