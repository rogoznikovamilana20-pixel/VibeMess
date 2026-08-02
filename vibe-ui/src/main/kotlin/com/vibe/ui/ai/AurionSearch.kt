package com.vibe.ui.ai

import com.vibe.bridge.model.VibeSearchHit
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.network.ServerConfig
import kotlinx.coroutines.withTimeoutOrNull
import org.telegram.messenger.ApplicationLoader

/**
 * Semantic layer for message search.
 *
 * Pipeline (all stages are optional and degrade gracefully):
 *  1. [expandQuery] — turns the user query into a few semantic variants
 *     (synonyms / paraphrases) so Telegram's text search finds more.
 *  2. [rerank] — reorders the merged hits by relevance to the original query.
 *
 * Any failure (timeout, network, parse) falls back to the input unchanged,
 * so the feature never breaks plain text search.
 */
object AurionSearch {

    private const val TAG = "AurionSearch"

    const val MAX_VARIANTS = 5
    const val RERANK_CANDIDATES = 20

    private const val EXPAND_TIMEOUT_MS = 10_000L
    private const val RERANK_TIMEOUT_MS = 10_000L
    private const val MAX_VARIANT_LENGTH = 80

    private val provider: AiProvider by lazy {
        ZvenoAIProvider(apiKey = ServerConfig(ApplicationLoader.applicationContext).getAiApiKey())
    }

    /**
     * Returns [MAX_VARIANTS]-ish search variants: the original query first,
     * then semantic alternatives. On any failure returns just the query.
     */
    suspend fun expandQuery(query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()

        val system = "Ты — помощник поиска сообщений в мессенджере. Придумай от 3 до 5 вариантов того же запроса " +
            "по смыслу: синонимы, перефраз, более короткие формулировки. Каждый вариант с новой строки, " +
            "без нумерации, без кавычек и пояснений. Только сами варианты, на языке запроса."

        val raw = withTimeoutOrNull(EXPAND_TIMEOUT_MS) {
            runCatching {
                provider.chat(
                    messages = listOf(
                        AiMessage(AiRole.SYSTEM, system),
                        AiMessage(AiRole.USER, trimmed)
                    ),
                    temperature = 0.1,
                    maxTokens = 300
                ).content
            }.onFailure { VibeLogger.w(TAG, "expandQuery failed", it) }.getOrNull()
        } ?: return listOf(trimmed)

        val parsed = parseVariants(raw)
        if (parsed.isEmpty()) return listOf(trimmed)
        return dedupe(listOf(trimmed) + parsed).take(MAX_VARIANTS)
    }

    /**
     * Reorders [hits] by relevance to [query]. On any failure returns hits unchanged.
     */
    suspend fun rerank(query: String, hits: List<VibeSearchHit>): List<VibeSearchHit> {
        if (hits.size <= 1 || query.isBlank()) return hits

        val candidates = hits.take(RERANK_CANDIDATES)
        val lines = candidates.mapIndexed { i, hit ->
            val text = hit.message.text.ifBlank { "📎 ${hit.message.type}" }.take(200).replace('\n', ' ')
            "#$i | ${hit.chatTitle}: $text"
        }.joinToString("\n")

        val system = "Ниже запрос и список найденных сообщений (номер | чат: текст). " +
            "Отсортируй номера от самого релевантного запросу к наименее релевантному. " +
            "Верни строго JSON-массив чисел, например [3,0,1,2]. Ничего кроме массива."

        val raw = withTimeoutOrNull(RERANK_TIMEOUT_MS) {
            runCatching {
                provider.chat(
                    messages = listOf(
                        AiMessage(AiRole.SYSTEM, system),
                        AiMessage(AiRole.USER, "Запрос: $query\n\nСообщения:\n$lines")
                    ),
                    temperature = 0.0,
                    maxTokens = 300
                ).content
            }.onFailure { VibeLogger.w(TAG, "rerank failed", it) }.getOrNull()
        }

        val order = parseRerankOrder(raw, candidates.size)
        if (order.isEmpty()) return hits

        val ranked = order.mapNotNull { i -> candidates.getOrNull(i) }
        val rankedKeys = ranked.map { it.chatId to it.message.id }.toSet()
        val remaining = candidates.filterNot { (it.chatId to it.message.id) in rankedKeys }
        return ranked + remaining
    }

    /**
     * Parses a raw LLM answer into clean query variants.
     * Pure function — unit tested.
     */
    internal fun parseVariants(raw: String): List<String> {
        val result = mutableListOf<String>()
        for (line in raw.lines()) {
            var v = line.trim()
            if (v.isEmpty()) continue
            // strip list markers: "- ", "* ", "1. ", "1) "
            v = v.replace(Regex("^[-*•]+\\s*"), "").replace(Regex("^\\d+[.)]\\s*"), "")
            v = v.trim('"', '\'', '«', '»', '“', '”')
            v = v.trim()
            if (v.isEmpty() || v.length > MAX_VARIANT_LENGTH) continue
            result.add(v)
        }
        return dedupe(result)
    }

    /**
     * Parses a raw LLM answer into an ordered list of indices (0..size-1).
     * Pure function — unit tested.
     */
    internal fun parseRerankOrder(raw: String?, size: Int): List<Int> {
        if (raw.isNullOrBlank() || size <= 0) return emptyList()
        val match = Regex("\\[[0-9,\\s-]*]").find(raw) ?: return emptyList()
        val indices = match.value
            .trim('[', ']')
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0 until size }
        return indices.distinct()
    }

    private fun dedupe(list: List<String>): List<String> {
        val seen = HashSet<String>()
        val result = mutableListOf<String>()
        for (item in list) {
            val key = item.trim().lowercase()
            if (key.isEmpty()) continue
            if (seen.add(key)) result.add(item)
        }
        return result
    }
}
