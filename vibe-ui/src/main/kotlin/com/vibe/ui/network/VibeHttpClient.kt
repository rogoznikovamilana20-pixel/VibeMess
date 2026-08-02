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

    private fun authHeader(useRust: Boolean): String? {
        val token = if (useRust) serverConfig.getRustAuthToken() else serverConfig.getAuthToken()
        return token.takeIf { it.isNotEmpty() }?.let { "Bearer $it" }
    }

    private suspend fun post(
        endpoint: String,
        body: JSONObject,
        useRust: Boolean = false,
        extraHeaders: Map<String, String> = emptyMap()
    ): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = if (useRust) serverConfig.getRustServerUrl() else serverConfig.getServerUrl()
                val url = URL("$baseUrl/$endpoint")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    authHeader(useRust)?.let { setRequestProperty("Authorization", it) }
                    extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
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
                    authHeader(useRust)?.let { setRequestProperty("Authorization", it) }
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

    private suspend fun getArray(
        endpoint: String,
        useRust: Boolean = false,
        extraHeaders: Map<String, String> = emptyMap()
    ): org.json.JSONArray? {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = if (useRust) serverConfig.getRustServerUrl() else serverConfig.getServerUrl()
                val url = URL("$baseUrl/$endpoint")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    authHeader(useRust)?.let { setRequestProperty("Authorization", it) }
                    extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
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

                if (responseBody != null && responseBody.isNotEmpty() && responseBody.trimStart().startsWith("[")) {
                    org.json.JSONArray(responseBody)
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
                    serverConfig.setRustAuthToken(token)
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
                    serverConfig.setRustAuthToken(token)
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

    // ============ Rust identity (bots & payments need a server-side UUID user) ============

    private fun jwtUserId(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = String(
                android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP),
                Charsets.UTF_8
            )
            JSONObject(payload).optString("sub").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Ensures this device has a server-side identity (UUID user on vibe-server).
     * Bots and payments require it. Returns the rust user id or null when offline.
     * Retries on every call — the server may come online later.
     */
    suspend fun rustEnsureIdentity(): String? {
        serverConfig.getRustUserId().takeIf { it.isNotBlank() }?.let { return it }

        val suffix = serverConfig.getUserId().takeIf { it.isNotBlank() } ?: System.currentTimeMillis().toString()
        val username = "vibe_" + suffix.takeLast(10) + "_" + (System.currentTimeMillis() % 100000)
        val email = "$username@vibe.local"
        val password = "vibe_" + java.util.UUID.randomUUID().toString().replace("-", "").take(24)

        val token = rustRegister(username, email, password).getOrNull() ?: return null
        val userId = jwtUserId(token) ?: username
        serverConfig.setRustUserId(userId)
        return userId
    }

    // ============ Rust Bots API ============

    suspend fun rustCreateBot(
        username: String,
        name: String,
        description: String,
        systemPrompt: String,
        commandsJson: String,
        isAi: Boolean
    ): Result<ServerBot> {
        return try {
            val identity = rustEnsureIdentity() ?: return Result.failure(Exception("Server offline"))
            val body = JSONObject().apply {
                put("username", username)
                put("name", name)
                put("description", description)
                put("system_prompt", systemPrompt)
                put("commands", JSONObject(commandsJson))
                put("is_ai", isAi)
            }
            val response = post("api/bots", body, useRust = true)
                ?: return Result.failure(Exception("No response from server"))
            val bot = ServerBot(
                id = response.optString("id"),
                username = response.optString("username"),
                name = response.optString("name"),
                description = "",
                token = response.optString("token"),
                isAi = isAi,
                isActive = true,
                isLocal = false
            )
            Result.success(bot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rustBotCatalog(): List<ServerBot> {
        return try {
            val array = getArray("api/bots/catalog", useRust = true) ?: return emptyList()
            parseBotList(array)
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustBotCatalog failed", e)
            emptyList()
        }
    }

    private fun parseBotList(array: org.json.JSONArray): List<ServerBot> {
        return (0 until array.length()).mapNotNull { i ->
            val b = array.getJSONObject(i)
            ServerBot(
                id = b.optString("id"),
                username = b.optString("username"),
                name = b.optString("name"),
                description = b.optString("description"),
                token = "",
                isAi = b.optBoolean("is_ai", true),
                isActive = b.optBoolean("is_active", true),
                isLocal = false
            )
        }
    }

    suspend fun rustSendToBot(token: String, senderId: String, text: String): Boolean {
        return try {
            val body = JSONObject().apply {
                put("sender_id", senderId)
                put("text", text)
            }
            post(
                "api/bot/message",
                body,
                useRust = true,
                extraHeaders = mapOf("X-Bot-Token" to token)
            )?.optBoolean("ok", false) ?: false
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustSendToBot failed", e)
            false
        }
    }

    suspend fun rustBotUpdates(token: String, afterId: Long): List<ServerBotUpdate> {
        return try {
            val array = getArray(
                "api/bot/updates?after_id=$afterId",
                useRust = true,
                extraHeaders = mapOf("X-Bot-Token" to token)
            ) ?: return emptyList()
            (0 until array.length()).mapNotNull { i ->
                val u = array.getJSONObject(i)
                ServerBotUpdate(
                    id = u.optLong("id", 0),
                    senderId = u.optString("sender_id"),
                    text = u.optString("text")
                )
            }
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustBotUpdates failed", e)
            emptyList()
        }
    }

    suspend fun rustAnswerUpdate(token: String, updateId: Long, reply: String): Boolean {
        return try {
            val body = JSONObject().apply {
                put("update_id", updateId)
                put("reply", reply)
            }
            post(
                "api/bot/answer",
                body,
                useRust = true,
                extraHeaders = mapOf("X-Bot-Token" to token)
            )?.optBoolean("ok", false) ?: false
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustAnswerUpdate failed", e)
            false
        }
    }

    suspend fun rustBotReplies(botId: String, userId: String, afterId: Long): List<ServerBotReply> {
        return try {
            val array = getArray("api/bots/$botId/replies?user_id=$userId&after_id=$afterId", useRust = true)
                ?: return emptyList()
            (0 until array.length()).mapNotNull { i ->
                val r = array.getJSONObject(i)
                ServerBotReply(
                    id = r.optLong("id", 0),
                    reply = r.optString("reply")
                )
            }
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustBotReplies failed", e)
            emptyList()
        }
    }

    suspend fun rustToggleBot(botId: String, isActive: Boolean): Boolean {
        return try {
            val body = JSONObject().apply { put("is_active", isActive) }
            post("api/bots/$botId/toggle", body, useRust = true)?.optBoolean("ok", false) ?: false
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustToggleBot failed", e)
            false
        }
    }

    suspend fun rustDeleteBot(botId: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val baseUrl = serverConfig.getRustServerUrl()
                val url = URL("$baseUrl/api/bots/$botId")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "DELETE"
                    setRequestProperty("Accept", "application/json")
                    authHeader(useRust = true)?.let {
                        setRequestProperty("Authorization", it)
                    }
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                val code = connection.responseCode
                connection.disconnect()
                code in 200..299
            }
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustDeleteBot failed", e)
            false
        }
    }

    // ============ Rust TURN API ============

    /**
     * Requests short-lived TURN credentials from vibe-server.
     * Returns null if the server is unreachable or TURN is not configured
     * (the app then falls back to the built-in public TURN relay).
     */
    suspend fun getTurnCredentials(): TurnCredentials? {
        return try {
            withContext(Dispatchers.IO) {
                val baseUrl = serverConfig.getRustServerUrl()
                val url = URL("$baseUrl/api/turn/credentials")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    authHeader(useRust = true)?.let {
                        setRequestProperty("Authorization", it)
                    }
                    connectTimeout = 3000
                    readTimeout = 3000
                }
                val code = connection.responseCode
                if (code in 200..299) {
                    val body = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(body)
                    val urlsArray = json.optJSONArray("urls")
                    val urls = (0 until (urlsArray?.length() ?: 0)).map { i -> urlsArray.getString(i) }
                    TurnCredentials(
                        urls = urls,
                        username = json.optString("username"),
                        credential = json.optString("credential"),
                        ttl = json.optInt("ttl", 3600)
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            VibeLogger.w(tag, "getTurnCredentials failed: ${e.message}")
            null
        }
    }

    // ============ Rust Payments API ============

    suspend fun rustPaymentPlans(): List<ServerPlan> {
        return try {
            val array = getArray("api/payments/plans", useRust = true) ?: return emptyList()
            (0 until array.length()).mapNotNull { i ->
                val p = array.getJSONObject(i)
                ServerPlan(
                    itemType = p.optString("item_type"),
                    label = p.optString("label"),
                    amountKopecks = p.optLong("amount_kopecks", 0)
                )
            }
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustPaymentPlans failed", e)
            emptyList()
        }
    }

    suspend fun rustCreatePayment(itemType: String): Result<ServerPayment> {
        return try {
            rustEnsureIdentity() ?: return Result.failure(Exception("Server offline"))
            val body = JSONObject().apply { put("item_type", itemType) }
            val response = post("api/payments/create", body, useRust = true)
                ?: return Result.failure(Exception("No response from server"))
            Result.success(
                ServerPayment(
                    paymentId = response.optString("payment_id"),
                    itemType = response.optString("item_type"),
                    amountKopecks = response.optLong("amount_kopecks", 0),
                    status = response.optString("status"),
                    confirmationUrl = response.optString("confirmation_url").ifEmpty { null },
                    demo = response.optBoolean("demo", true)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rustPaymentStatus(paymentId: String): String? {
        return try {
            get("api/payments/status?payment_id=$paymentId", useRust = true)?.optString("status")
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustPaymentStatus failed", e)
            null
        }
    }

    suspend fun rustDemoComplete(paymentId: String): Boolean {
        return try {
            post("api/payments/demo-complete?payment_id=$paymentId", JSONObject(), useRust = true)
                ?.optString("status") == "succeeded"
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustDemoComplete failed", e)
            false
        }
    }

    suspend fun rustPaymentBalance(): ServerBalance? {
        return try {
            rustEnsureIdentity() ?: return null
            val json = get("api/payments/balance", useRust = true) ?: return null
            ServerBalance(
                balance = json.optLong("balance", 0),
                subscriptionPlan = json.optString("subscription_plan").ifEmpty { null },
                subscriptionExpiresAt = json.optString("subscription_expires_at").ifEmpty { null }
            )
        } catch (e: Exception) {
            VibeLogger.e(tag, "rustPaymentBalance failed", e)
            null
        }
    }

    suspend fun rustRequestPayout(sparks: Int, bankName: String, accountNumber: String): Result<ServerPayout> {
        return try {
            rustEnsureIdentity() ?: return Result.failure(Exception("Server offline"))
            val body = JSONObject().apply {
                put("sparks_amount", sparks)
                put("bank_name", bankName)
                put("account_number", accountNumber)
            }
            val response = post("api/payouts", body, useRust = true)
                ?: return Result.failure(Exception("No response from server"))
            Result.success(
                ServerPayout(
                    id = response.optString("id"),
                    amountKopecks = response.optLong("amount_kopecks", 0),
                    status = response.optString("status")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ServerBot(
    val id: String,
    val username: String,
    val name: String,
    val description: String,
    val token: String,
    val isAi: Boolean,
    val isActive: Boolean,
    val isLocal: Boolean
)

data class ServerBotUpdate(
    val id: Long,
    val senderId: String,
    val text: String
)

data class ServerBotReply(
    val id: Long,
    val reply: String
)

data class ServerPlan(
    val itemType: String,
    val label: String,
    val amountKopecks: Long
)

data class ServerPayment(
    val paymentId: String,
    val itemType: String,
    val amountKopecks: Long,
    val status: String,
    val confirmationUrl: String?,
    val demo: Boolean
)

data class ServerPayout(
    val id: String,
    val amountKopecks: Long,
    val status: String
)

data class ServerBalance(
    val balance: Long,
    val subscriptionPlan: String?,
    val subscriptionExpiresAt: String?
)

data class TurnCredentials(
    val urls: List<String>,
    val username: String,
    val credential: String,
    val ttl: Int
)

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
