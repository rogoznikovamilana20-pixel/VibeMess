package com.vibe.ui.ai

import com.vibe.common.logging.VibeLogger
import com.vibe.ui.data.db.entity.ChatEntity
import com.vibe.ui.network.ServerConfig
import kotlinx.coroutines.withTimeoutOrNull
import org.telegram.messenger.ApplicationLoader

/**
 * Classifies chats into «Личное» / «Работа».
 *
 * Two stages, both degrade gracefully:
 *  1. Deterministic fallback — channels always go to «Работа», title keyword rules otherwise.
 *  2. AI pass (batched, ≤ 100 chats per request) refines the result; its answers are
 *     merged over the fallback. On any AI failure the fallback result stands.
 */
object AurionClassifier {

    private const val TAG = "AurionClassifier"
    const val CLASSIFY_BATCH_SIZE = 100
    private const val CLASSIFY_TIMEOUT_MS = 15_000L

    /** Substring keywords (lowercase) that mark a chat as work-related. */
    private val WORK_KEYWORDS = listOf(
        "работа", "рабоч", "офис", "office", "team", "команд", "проект", "дедлайн",
        "задач", "sale", "sales", "продаж", "поддержк", "support", "маркетинг",
        "marketing", "разработк", "dev", "финанс", "hr", "отдел", "департамент",
        "бухгалтер", "логистик", "тендер", "собеседован", "ваканс", "резюме",
        "клиент", "заказчик", "договор", "отчёт", "отчет", "совещан", "спринт",
        "бэклог", "продакшн", "production", "стажировк", "аутсорс", "фриланс",
        "зарплат", "бюджет", "бизнес", "стартап", "b2b", "корп", "issue", "ишью",
        "jira", "джира", "slack", "слак", "трекер", "seo", "сео", "таск", "оффер",
        "news", "новост", "канал", "channel"
    )

    private val provider: AiProvider by lazy {
        ZvenoAIProvider(apiKey = ServerConfig(ApplicationLoader.applicationContext).getAiApiKey())
    }

    /**
     * Returns chatId → isPersonal for every chat in [chats].
     * Never throws; falls back to [fallbackIsPersonal] for everything.
     */
    suspend fun classify(chats: List<ChatEntity>): Map<Long, Boolean> {
        if (chats.isEmpty()) return emptyMap()
        val result = LinkedHashMap<Long, Boolean>()
        for (chat in chats) {
            result[chat.id] = fallbackIsPersonal(chat.type, chat.title)
        }
        if (chats.size < 2) return result

        for (batch in chats.chunked(CLASSIFY_BATCH_SIZE)) {
            try {
                val lines = batch.mapIndexed { i, c ->
                    "${i + 1} | ${c.title} | ${c.type}"
                }.joinToString("\n")

                val system = "Ты — помощник, который раскладывает чаты мессенджера на личные и рабочие. " +
                    "Личные: семья, друзья, увлечения, досуг, учёба, знакомые. " +
                    "Рабочие: работа, задачи, клиенты, каналы-рассылки, новости, организации. " +
                    "Ниже нумерованный список чатов (номер | название | тип). " +
                    "Верни строго JSON вида {\"1\":\"рабочая\",\"2\":\"личная\"} — только пары, " +
                    "где уверен(а). Ничего кроме JSON."

                val raw = withTimeoutOrNull(CLASSIFY_TIMEOUT_MS) {
                    runCatching {
                        provider.chat(
                            messages = listOf(
                                AiMessage(AiRole.SYSTEM, system),
                                AiMessage(AiRole.USER, lines)
                            ),
                            temperature = 0.1,
                            maxTokens = 400
                        ).content
                    }.onFailure { VibeLogger.w(TAG, "classify batch failed", it) }.getOrNull()
                }

                val byIndex = parseClassification(raw, batch.size)
                if (byIndex != null) {
                    for ((index, isPersonal) in byIndex) {
                        result[batch[index - 1].id] = isPersonal
                    }
                }
            } catch (e: Exception) {
                VibeLogger.w(TAG, "classify batch failed", e)
            }
        }
        return result
    }

    /**
     * Deterministic rule used when the AI is unavailable:
     * channels are broadcasts → work; title keywords → work; everything else → personal.
     * Pure function — unit tested.
     */
    internal fun fallbackIsPersonal(type: String, title: String): Boolean {
        if (type.equals("CHANNEL", ignoreCase = true)) return false
        val lower = title.lowercase()
        for (keyword in WORK_KEYWORDS) {
            if (lower.contains(keyword)) return false
        }
        return true
    }

    /**
     * Parses the AI's answer into 1-based indices → isPersonal.
     * Accepts `{"3":"рабочая","5":"личная"}` and bare `3: рабочая` lines.
     * Pure function — unit tested.
     */
    internal fun parseClassification(raw: String?, size: Int): Map<Int, Boolean>? {
        if (raw.isNullOrBlank() || size <= 0) return null
        val result = mutableMapOf<Int, Boolean>()

        val jsonPairs = Regex("\"(\\d+)\"\\s*:\\s*\"([^\"]+)\"").findAll(raw)
        for (match in jsonPairs) {
            val index = match.groupValues[1].toIntOrNull() ?: continue
            val value = classifyValue(match.groupValues[2]) ?: continue
            if (index in 1..size) result[index] = value
        }

        if (result.isEmpty()) {
            val bare = Regex("(\\d+)\\s*[:=\\-—]\\s*(личная|личн|рабочая|рабоч|работ|personal|work)").findAll(raw)
            for (match in bare) {
                val index = match.groupValues[1].toIntOrNull() ?: continue
                val value = classifyValue(match.groupValues[2]) ?: continue
                if (index in 1..size) result[index] = value
            }
        }

        return result.ifEmpty { null }
    }

    private fun classifyValue(value: String): Boolean? {
        return when (value.trim().lowercase()) {
            "личная", "личн", "personal", "true", "да" -> true
            "рабочая", "рабоч", "работ", "work", "false", "нет" -> false
            else -> null
        }
    }
}
