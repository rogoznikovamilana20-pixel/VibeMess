package com.vibe.ui.data.bot

import android.content.Context
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.BotEntity
import com.vibe.ui.network.ServerBotReply
import com.vibe.ui.network.ServerConfig
import com.vibe.ui.network.VibeHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

data class BotCommand(
    val command: String,
    val description: String
)

data class BotScriptRule(
    val keyword: String,
    val response: String
)

data class ServerUpdate(
    val updateId: Long,
    val text: String
)

class BotRepository(context: Context) {

    private val tag = "BotRepository"
    private val db = VibeDatabase.getDatabase(context)
    private val serverConfig = ServerConfig(context)
    private val httpClient = VibeHttpClient(serverConfig)

    fun getAll(): Flow<List<BotEntity>> = db.botDao().getAll()

    suspend fun getBot(id: Long): BotEntity? = db.botDao().getById(id)

    suspend fun usernameTaken(username: String): Boolean {
        val normalized = username.trim().removePrefix("@").lowercase(Locale.ROOT)
        return db.botDao().countByUsername(normalized) > 0
    }

    suspend fun createBot(
        name: String,
        username: String,
        description: String,
        systemPrompt: String,
        commands: List<BotCommand>,
        scripts: List<BotScriptRule>,
        isAi: Boolean
    ): Result<BotEntity> = withContext(Dispatchers.IO) {
        val normalizedUsername = username.trim().removePrefix("@").lowercase(Locale.ROOT)
        if (name.isBlank()) return@withContext Result.failure(Exception("Name is required"))
        if (!normalizedUsername.matches(Regex("^[a-zA-Z0-9_]{3,32}$"))) {
            return@withContext Result.failure(
                Exception("Username must be 3-32 chars: a-z, 0-9, _")
            )
        }
        if (db.botDao().countByUsername(normalizedUsername) > 0) {
            return@withContext Result.failure(Exception("This bot username is already taken"))
        }

        val commandsJson = JSONArray().apply {
            commands.forEach { c ->
                put(JSONObject().put("command", c.command).put("description", c.description))
            }
        }.toString()
        val scriptsJson = JSONArray().apply {
            scripts.forEach { s ->
                put(JSONObject().put("keyword", s.keyword).put("response", s.response))
            }
        }.toString()

        var serverId = ""
        var token = ""
        var local = true

        val serverResult = httpClient.rustCreateBot(
            username = normalizedUsername,
            name = name.trim(),
            description = description,
            systemPrompt = systemPrompt,
            commandsJson = commandsJson,
            isAi = isAi
        )
        serverResult.onSuccess { bot ->
            serverId = bot.id
            token = bot.token
            local = false
        }.onFailure { e ->
            VibeLogger.d(tag, "Server bot create failed (local fallback): ${e.message}")
        }

        val bot = BotEntity(
            serverId = serverId,
            token = token,
            name = name.trim(),
            username = normalizedUsername,
            description = description,
            avatarInitial = name.trim().firstOrNull()?.uppercase() ?: "B",
            systemPrompt = systemPrompt,
            commandsJson = commandsJson,
            scriptRepliesJson = scriptsJson,
            isAi = isAi,
            isEnabled = true,
            isLocal = local
        )
        val id = db.botDao().insert(bot)
        Result.success(bot.copy(id = id))
    }

    suspend fun updateBot(bot: BotEntity) {
        withContext(Dispatchers.IO) {
            db.botDao().update(bot)
            if (bot.serverId.isNotBlank() && bot.token.isNotBlank()) {
                httpClient.rustToggleBot(bot.serverId, bot.isEnabled)
            }
        }
    }

    suspend fun deleteBot(bot: BotEntity) {
        withContext(Dispatchers.IO) {
            if (bot.serverId.isNotBlank()) {
                httpClient.rustDeleteBot(bot.serverId)
            }
            db.botMessageDao().clearByBotId(bot.id)
            db.botDao().delete(bot)
        }
    }

    suspend fun toggleBot(bot: BotEntity, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            db.botDao().update(bot.copy(isEnabled = enabled))
            if (bot.serverId.isNotBlank()) {
                httpClient.rustToggleBot(bot.serverId, enabled)
            }
        }
    }

    suspend fun ownedBots(): List<BotEntity> = withContext(Dispatchers.IO) {
        db.botDao().getAllNow().filter { it.token.isNotBlank() }
    }

    suspend fun sendToServerBot(bot: BotEntity, senderId: String, text: String): Boolean {
        if (bot.token.isBlank()) return false
        return httpClient.rustSendToBot(bot.token, senderId, text)
    }

    suspend fun pollServerUpdates(bot: BotEntity): List<ServerUpdate> {
        if (bot.token.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            val updates = httpClient.rustBotUpdates(bot.token, bot.lastUpdateId)
            if (updates.isEmpty()) return@withContext emptyList()
            val latest = updates.maxOf { it.id }
            db.botDao().update(bot.copy(lastUpdateId = latest))
            updates.map { ServerUpdate(it.id, it.text) }
        }
    }

    suspend fun answerServerUpdate(bot: BotEntity, updateId: Long, reply: String) {
        if (bot.token.isBlank()) return
        httpClient.rustAnswerUpdate(bot.token, updateId, reply)
    }

    suspend fun pollServerReplies(bot: BotEntity, userId: String, afterId: Long): List<ServerBotReply> {
        if (bot.serverId.isBlank() || userId.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            httpClient.rustBotReplies(bot.serverId, userId, afterId)
        }
    }

    fun generateLocalToken(): String = "vibe_${UUID.randomUUID().toString().replace("-", "").take(16)}"
}
