package com.vibe.ui.network

import android.util.Log
import com.vibe.ui.e2e.E2EEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

object SupabaseClient {

    private const val TAG = "SupabaseClient"
    private val jsonType = "application/json".toMediaType()

    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ============ Models ============

    data class Profile(
        val id: String,
        val displayName: String = "",
        val avatarUrl: String? = null,
        val isOnline: Boolean = false,
        val lastSeen: String = "",
        val role: String = "user",
        val isBanned: Boolean = false
    )

    data class Chat(
        val id: String,
        val title: String,
        val isGroup: Boolean = false,
        val lastMessage: String = "",
        val lastMessageAt: String = "",
        val space: String = "personal"
    )

    data class Message(
        val id: String,
        val chatId: String,
        val senderId: String,
        val content: String,
        val messageType: String = "text",
        val createdAt: String = ""
    )

    data class TypingEvent(
        val chatId: String,
        val userId: String,
        val isTyping: Boolean
    )

    // ============ Profiles ============

    suspend fun createProfile(
        supabaseUrl: String, anonKey: String, authToken: String,
        displayName: String, username: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = getUserIdFromToken(authToken)
            val body = JSONObject().apply {
                put("id", userId)
                put("display_name", displayName)
                if (!username.isNullOrBlank()) put("username", username)
                put("is_online", true)
                put("last_seen", java.time.Instant.now().toString())
            }
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body.toString().toRequestBody(jsonType)).build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "createProfile failed", e)
            false
        }
    }

    suspend fun getProfile(supabaseUrl: String, anonKey: String, authToken: String, userId: String): Profile? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/profiles?id=eq.$userId&select=*")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $authToken")
                    .addHeader("Content-Type", "application/json")
                    .get().build()

                val response = client.newCall(request).execute()
                val arr = JSONArray(response.body?.string() ?: "[]")
                if (arr.length() == 0) return@withContext null
                val obj = arr.getJSONObject(0)
                Profile(
                    id = obj.getString("id"),
                    displayName = obj.optString("display_name", ""),
                    avatarUrl = obj.optString("avatar_url", null.toString()),
                    isOnline = obj.optBoolean("is_online", false),
                    lastSeen = obj.optString("last_seen", ""),
                    role = obj.optString("role", "user"),
                    isBanned = obj.optBoolean("is_banned", false)
                )
            } catch (_: Exception) { null }
        }

    suspend fun getProfiles(supabaseUrl: String, anonKey: String, authToken: String, userIds: List<String>): Map<String, Profile> =
        withContext(Dispatchers.IO) {
            try {
                val ids = userIds.joinToString(",") { "eq.$it" }
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/profiles?id=($ids)&select=*")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $authToken")
                    .addHeader("Content-Type", "application/json")
                    .get().build()

                val response = client.newCall(request).execute()
                val arr = JSONArray(response.body?.string() ?: "[]")
                val map = mutableMapOf<String, Profile>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val p = Profile(
                        id = obj.getString("id"),
                        displayName = obj.optString("display_name", ""),
                        avatarUrl = obj.optString("avatar_url", null.toString()),
                        isOnline = obj.optBoolean("is_online", false),
                        lastSeen = obj.optString("last_seen", ""),
                        role = obj.optString("role", "user"),
                        isBanned = obj.optBoolean("is_banned", false)
                    )
                    map[p.id] = p
                }
                map
            } catch (_: Exception) { emptyMap() }
        }

    suspend fun updateAvatar(supabaseUrl: String, anonKey: String, authToken: String, avatarUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val userId = getUserIdFromToken(authToken)
                val body = JSONObject().apply { put("avatar_url", avatarUrl) }
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/profiles?id=eq.$userId")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $authToken")
                    .addHeader("Content-Type", "application/json")
                    .patch(body.toString().toRequestBody(jsonType)).build()
                client.newCall(request).execute().isSuccessful
            } catch (_: Exception) { false }
        }

    suspend fun uploadAvatar(supabaseUrl: String, anonKey: String, authToken: String, fileName: String, bytes: ByteArray): String? =
        withContext(Dispatchers.IO) {
            try {
                val userId = getUserIdFromToken(authToken)
                val path = "$userId/$fileName"
                val body = bytes.toRequestBody("image/png".toMediaType())
                val request = Request.Builder()
                    .url("$supabaseUrl/storage/v1/object/avatars/$path")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $authToken")
                    .addHeader("Content-Type", "image/png")
                    .put(body).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    "$supabaseUrl/storage/v1/object/public/avatars/$path"
                } else null
            } catch (_: Exception) { null }
        }

    // ============ Online Status ============

    suspend fun setOnline(supabaseUrl: String, anonKey: String, authToken: String, online: Boolean) =
        withContext(Dispatchers.IO) {
            try {
                val userId = getUserIdFromToken(authToken)
                val body = JSONObject().apply {
                    put("is_online", online)
                    put("last_seen", java.time.Instant.now().toString())
                }
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/profiles?id=eq.$userId")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $authToken")
                    .addHeader("Content-Type", "application/json")
                    .patch(body.toString().toRequestBody(jsonType)).build()
                client.newCall(request).execute()
            } catch (_: Exception) {}
        }

    // ============ Typing ============

    suspend fun setTyping(supabaseUrl: String, anonKey: String, authToken: String, chatId: String, typing: Boolean) =
        withContext(Dispatchers.IO) {
            try {
                val userId = getUserIdFromToken(authToken)
                val body = JSONObject().apply {
                    put("chat_id", chatId)
                    put("user_id", userId)
                    put("is_typing", typing)
                }
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/typing_status")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $authToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(body.toString().toRequestBody(jsonType)).build()
                client.newCall(request).execute()
            } catch (_: Exception) {}
        }

    suspend fun getTypingUsers(supabaseUrl: String, anonKey: String, authToken: String, chatId: String): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val myId = getUserIdFromToken(authToken)
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/typing_status?chat_id=eq.$chatId&is_typing=eq.true&user_id=neq.$myId&select=user_id")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $authToken")
                    .addHeader("Content-Type", "application/json")
                    .get().build()
                val response = client.newCall(request).execute()
                val arr = JSONArray(response.body?.string() ?: "[]")
                (0 until arr.length()).map { arr.getJSONObject(it).getString("user_id") }
            } catch (_: Exception) { emptyList() }
        }

    // ============ Chats ============

    suspend fun getChats(supabaseUrl: String, anonKey: String, authToken: String, space: String? = null): List<Chat> =
        withContext(Dispatchers.IO) {
            try {
                val userId = getUserIdFromToken(authToken)
                var url = "$supabaseUrl/rest/v1/chats?select=*,chat_members!inner(user_id)&chat_members.user_id=eq.$userId"
                if (space != null) {
                    url += "&space=eq.$space"
                }
                url += "&order=last_message_at.desc"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $authToken")
                    .addHeader("Content-Type", "application/json")
                    .get().build()

                val response = client.newCall(request).execute()
                val arr = JSONArray(response.body?.string() ?: "[]")
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    Chat(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        isGroup = obj.optBoolean("is_group", false),
                        lastMessage = obj.optString("last_message", ""),
                        lastMessageAt = obj.optString("last_message_at", ""),
                        space = obj.optString("space", "personal")
                    )
                }
            } catch (_: Exception) { emptyList() }
        }

    suspend fun createChat(
        supabaseUrl: String, anonKey: String, authToken: String,
        title: String, otherUserId: String? = null, space: String = "personal"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val userId = getUserIdFromToken(authToken)
            val chatBody = JSONObject().apply {
                put("title", title)
                put("created_by", userId)
                put("space", space)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/chats")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(chatBody.toString().toRequestBody(jsonType)).build()

            val response = client.newCall(request).execute()
            val arr = JSONArray(response.body?.string() ?: "[]")
            if (arr.length() == 0) return@withContext null

            val chatId = arr.getJSONObject(0).getString("id")
            addMember(supabaseUrl, anonKey, authToken, chatId, userId)
            if (otherUserId != null) {
                addMember(supabaseUrl, anonKey, authToken, chatId, otherUserId)
            }
            chatId
        } catch (_: Exception) { null }
    }

    private suspend fun addMember(supabaseUrl: String, anonKey: String, authToken: String, chatId: String, userId: String) {
        try {
            val body = JSONObject().apply {
                put("chat_id", chatId)
                put("user_id", userId)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/chat_members")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonType)).build()
            client.newCall(request).execute()
        } catch (_: Exception) {}
    }

    // ============ Messages ============

    suspend fun getMessages(
        supabaseUrl: String, anonKey: String, authToken: String,
        chatId: String, limit: Int = 50
    ): List<Message> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/messages?chat_id=eq.$chatId&order=created_at.asc&limit=$limit")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .get().build()

            val response = client.newCall(request).execute()
            val arr = JSONArray(response.body?.string() ?: "[]")
            val userId = getUserIdFromToken(authToken)

            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val rawContent = obj.getString("content")

                // E2EE: decrypt content if encrypted
                val decryptedContent = if (E2EEngine.isReady()) {
                    val chatId = obj.getString("chat_id")
                    E2EEngine.decryptMessage(chatId, rawContent) ?: rawContent
                } else {
                    rawContent
                }

                Message(
                    id = obj.getString("id"),
                    chatId = obj.getString("chat_id"),
                    senderId = obj.getString("sender_id"),
                    content = decryptedContent,
                    messageType = obj.optString("message_type", "text"),
                    createdAt = obj.optString("created_at", "")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun sendMessage(
        supabaseUrl: String, anonKey: String, authToken: String,
        chatId: String, content: String
    ): Message? = withContext(Dispatchers.IO) {
        try {
            val userId = getUserIdFromToken(authToken)

            // E2EE: encrypt content before sending
            val encryptedContent = if (E2EEngine.isReady()) {
                E2EEngine.encryptMessage(chatId, content) ?: content
            } else {
                content
            }

            val body = JSONObject().apply {
                put("chat_id", chatId)
                put("sender_id", userId)
                put("content", encryptedContent)
                put("message_type", "text")
            }
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/messages")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body.toString().toRequestBody(jsonType)).build()

            val response = client.newCall(request).execute()
            val arr = JSONArray(response.body?.string() ?: "[]")
            if (arr.length() == 0) return@withContext null
            val obj = arr.getJSONObject(0)
            Message(
                id = obj.getString("id"),
                chatId = obj.getString("chat_id"),
                senderId = obj.getString("sender_id"),
                content = content, // Return plaintext for local display
                messageType = obj.optString("message_type", "text"),
                createdAt = obj.optString("created_at", "")
            )
        } catch (_: Exception) { null }
    }

    // ============ Search ============

    suspend fun updateChatSpace(
        supabaseUrl: String, anonKey: String, authToken: String,
        chatId: String, space: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("space", space) }
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/chats?id=eq.$chatId")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .patch(body.toString().toRequestBody(jsonType)).build()
            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) { false }
    }

    suspend fun searchMessages(
        supabaseUrl: String, anonKey: String, authToken: String,
        query: String, limit: Int = 30
    ): List<Message> = withContext(Dispatchers.IO) {
        try {
            val myId = getUserIdFromToken(authToken)
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/rpc/search_messages?p_query=$query&p_user_id=$myId&p_limit=$limit")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .get().build()

            val response = client.newCall(request).execute()
            val arr = JSONArray(response.body?.string() ?: "[]")
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Message(
                    id = obj.getString("id"),
                    chatId = obj.getString("chat_id"),
                    senderId = obj.getString("sender_id"),
                    content = obj.getString("content"),
                    messageType = obj.optString("message_type", "text"),
                    createdAt = obj.optString("created_at", "")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    // ============ E2E Keys ============

    suspend fun uploadE2EKeys(
        supabaseUrl: String, anonKey: String, authToken: String,
        keysJson: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = getUserIdFromToken(authToken)
            val body = JSONObject().apply {
                put("id", userId)
                put("e2e_keys", keysJson)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.$userId")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .patch(body.toString().toRequestBody(jsonType)).build()

            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) { false }
    }

    suspend fun fetchE2EKeys(
        supabaseUrl: String, anonKey: String, authToken: String,
        userId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.$userId&select=e2e_keys")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .get().build()

            val response = client.newCall(request).execute()
            val arr = JSONArray(response.body?.string() ?: "[]")
            if (arr.length() > 0) {
                val keys = arr.getJSONObject(0).optString("e2e_keys", "")
                if (keys.isNotBlank()) keys else null
            } else null
        } catch (_: Exception) { null }
    }

    // ============ Admin ============

    suspend fun deleteProfile(
        supabaseUrl: String, anonKey: String, authToken: String, userId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.$userId")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .delete().build()
            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) { false }
    }

    suspend fun banUser(
        supabaseUrl: String, anonKey: String, authToken: String,
        userId: String, reason: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("is_banned", true)
                put("banned_at", java.time.Instant.now().toString())
                put("ban_reason", reason)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.$userId")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .patch(body.toString().toRequestBody(jsonType)).build()
            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) { false }
    }

    suspend fun unbanUser(
        supabaseUrl: String, anonKey: String, authToken: String, userId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("is_banned", false)
                put("banned_at", JSONObject.NULL)
                put("ban_reason", JSONObject.NULL)
            }
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.$userId")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .patch(body.toString().toRequestBody(jsonType)).build()
            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) { false }
    }

    suspend fun logAdminAction(
        supabaseUrl: String, anonKey: String, authToken: String,
        action: String, targetId: String? = null, targetName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val adminId = getUserIdFromToken(authToken)
            val body = JSONObject().apply {
                put("admin_id", adminId)
                put("action", action)
                targetId?.let { put("target_id", it) }
                targetName?.let { put("target_name", it) }
            }
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/admin_logs")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonType)).build()
            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) { false }
    }

    suspend fun getAdminLogs(
        supabaseUrl: String, anonKey: String, authToken: String,
        limit: Int = 50
    ): List<AdminLog> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/admin_logs?order=created_at.desc&limit=$limit")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .get().build()

            val response = client.newCall(request).execute()
            val arr = JSONArray(response.body?.string() ?: "[]")
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                AdminLog(
                    id = obj.getLong("id"),
                    adminId = obj.optString("admin_id", ""),
                    action = obj.getString("action"),
                    targetId = obj.optString("target_id", null),
                    targetName = obj.optString("target_name", null),
                    createdAt = obj.optString("created_at", "")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getStats(
        supabaseUrl: String, anonKey: String, authToken: String
    ): AdminStats = withContext(Dispatchers.IO) {
        try {
            val allProfiles = getAllProfiles(supabaseUrl, anonKey, authToken)
            val now = java.time.Instant.now()
            val weekAgo = now.minus(java.time.Duration.ofDays(7))

            AdminStats(
                totalUsers = allProfiles.size,
                onlineUsers = allProfiles.count { it.isOnline },
                adminCount = allProfiles.count { it.role == "admin" || it.role == "super_admin" },
                bannedCount = allProfiles.count { it.isBanned }
            )
        } catch (_: Exception) {
            AdminStats(0, 0, 0, 0)
        }
    }

    suspend fun getAllProfiles(
        supabaseUrl: String, anonKey: String, authToken: String
    ): List<Profile> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?select=*&order=display_name.asc")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .get().build()

            val response = client.newCall(request).execute()
            val arr = JSONArray(response.body?.string() ?: "[]")
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Profile(
                    id = obj.getString("id"),
                    displayName = obj.optString("display_name", ""),
                    avatarUrl = obj.optString("avatar_url", null.toString()),
                    isOnline = obj.optBoolean("is_online", false),
                    lastSeen = obj.optString("last_seen", ""),
                    role = obj.optString("role", "user"),
                    isBanned = obj.optBoolean("is_banned", false)
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun updateUserRole(
        supabaseUrl: String, anonKey: String, authToken: String,
        userId: String, role: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("role", role) }
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.$userId")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .patch(body.toString().toRequestBody(jsonType)).build()
            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) { false }
    }

    // ============ Helper ============

    fun getUserIdFromToken(jwt: String): String {
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return ""
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT))
            JSONObject(payload).optString("sub", "")
        } catch (_: Exception) { "" }
    }
}

data class AdminLog(
    val id: Long,
    val adminId: String,
    val action: String,
    val targetId: String? = null,
    val targetName: String? = null,
    val createdAt: String = ""
)

data class AdminStats(
    val totalUsers: Int = 0,
    val onlineUsers: Int = 0,
    val adminCount: Int = 0,
    val bannedCount: Int = 0
)
