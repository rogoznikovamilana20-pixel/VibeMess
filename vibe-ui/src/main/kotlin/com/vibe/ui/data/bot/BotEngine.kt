package com.vibe.ui.data.bot

import com.vibe.common.logging.VibeLogger
import com.vibe.ui.ai.AiMessage
import com.vibe.ui.ai.AiProvider
import com.vibe.ui.ai.AiRole
import com.vibe.ui.ai.ZvenoAIProvider
import com.vibe.ui.data.db.entity.BotEntity
import com.vibe.ui.data.db.entity.BotMessageEntity
import com.vibe.ui.network.ServerConfig
import org.json.JSONArray
import org.json.JSONObject
import org.telegram.messenger.ApplicationLoader

object BotEngine {

    private const val TAG = "BotEngine"

    fun parseCommands(json: String): List<BotCommand> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val o = array.getJSONObject(i)
                BotCommand(
                    command = o.optString("command"),
                    description = o.optString("description")
                )
            }.filter { it.command.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun commandsToJson(commands: List<BotCommand>): String {
        return JSONArray().apply {
            commands.forEach { c ->
                put(JSONObject().put("command", c.command).put("description", c.description))
            }
        }.toString()
    }

    fun parseScripts(json: String): List<BotScriptRule> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val o = array.getJSONObject(i)
                BotScriptRule(
                    keyword = o.optString("keyword"),
                    response = o.optString("response")
                )
            }.filter { it.keyword.isNotBlank() && it.response.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun scriptsToJson(scripts: List<BotScriptRule>): String {
        return JSONArray().apply {
            scripts.forEach { s ->
                put(JSONObject().put("keyword", s.keyword).put("response", s.response))
            }
        }.toString()
    }

    /**
     * Generates a bot reply for the given user message.
     * Order: /start, /help, custom commands text, script keywords, AI (if enabled).
     * Returns null when the bot has nothing to say.
     */
    suspend fun reply(
        bot: BotEntity,
        text: String,
        history: List<BotMessageEntity>
    ): String? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null

        if (trimmed.startsWith("/")) {
            val command = trimmed.substringAfter("/").substringBefore(" ").lowercase()
            when (command) {
                "start" -> return welcome(bot)
                "help" -> return help(bot)
            }
        }

        parseScripts(bot.scriptRepliesJson).firstOrNull {
            trimmed.contains(it.keyword, ignoreCase = true)
        }?.let { return it.response }

        if (bot.isAi && bot.systemPrompt.isNotBlank()) {
            return aiReply(bot, trimmed, history)
        }

        return null
    }

    private fun welcome(bot: BotEntity): String {
        val description = bot.description.ifBlank { "Я — бот в мессенджере Vibe." }
        val commands = parseCommands(bot.commandsJson)
        val helpSuffix = if (commands.isNotEmpty()) {
            "\n\nОтправьте /help, чтобы увидеть мои команды."
        } else {
            ""
        }
        return "Привет! Я ${bot.name}. $description$helpSuffix"
    }

    private fun help(bot: BotEntity): String {
        val commands = parseCommands(bot.commandsJson)
        if (commands.isEmpty()) {
            return "Доступные команды:\n/start — начать\n/help — справка"
        }
        val list = commands.joinToString("\n") { "/${it.command} — ${it.description}" }
        return "Доступные команды:\n$list"
    }

    private suspend fun aiReply(
        bot: BotEntity,
        text: String,
        history: List<BotMessageEntity>
    ): String? {
        return try {
            val provider: AiProvider = ZvenoAIProvider(
                apiKey = ServerConfig(ApplicationLoader.applicationContext).getAiApiKey()
            )
            val messages = buildList {
                add(AiMessage(AiRole.SYSTEM, bot.systemPrompt))
                history.takeLast(20).forEach { m ->
                    add(AiMessage(if (m.isUser) AiRole.USER else AiRole.ASSISTANT, m.text))
                }
                add(AiMessage(AiRole.USER, text))
            }
            provider.chat(messages).content
        } catch (e: Exception) {
            VibeLogger.e(TAG, "AI reply failed", e)
            null
        }
    }
}
