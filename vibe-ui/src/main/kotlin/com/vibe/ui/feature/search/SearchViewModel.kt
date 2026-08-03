package com.vibe.ui.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.bridge.model.VibeSearchHit
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.ai.AurionSearch
import com.vibe.ui.di.VibeContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SearchGroup(
    val chatId: Long,
    val chatTitle: String,
    val hits: List<VibeSearchHit>
)

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Searching : SearchUiState
    data class Results(
        val groups: List<SearchGroup>,
        val semanticFailed: Boolean = false
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel : ViewModel() {

    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    /** True while the semantic pass (expansion + rerank) is running behind the results. */
    private val _semanticBusy = MutableStateFlow(false)
    val semanticBusy: StateFlow<Boolean> = _semanticBusy.asStateFlow()

    private var searchJob: Job? = null

    fun search(rawQuery: String) {
        val query = rawQuery.trim()
        searchJob?.cancel()
        _semanticBusy.value = false
        if (query.isEmpty()) {
            _state.value = SearchUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            _state.value = SearchUiState.Searching

            // Phase 1 — plain text search for the original query.
            val textHits = runCatching {
                ensureInitialized()
                withContext(Dispatchers.IO) {
                    VibeContainer.getGateway().search
                        .searchMessages(query, limit = 50)
                        .first()
                }
            }.getOrElse { e ->
                VibeLogger.e("SearchVM", "text search failed", e)
                _state.value = SearchUiState.Error(e.message ?: "Поиск не выполнен")
                return@launch
            }

            if (textHits.isEmpty()) {
                _state.value = SearchUiState.Results(emptyList())
                return@launch
            }

            // Show text results immediately.
            _state.value = SearchUiState.Results(group(textHits))
            if (query.length < 2) return@launch

            // Phase 2 — semantic pass (never breaks the results above).
            _semanticBusy.value = true
            try {
                val variants = AurionSearch.expandQuery(query)
                val merged = LinkedHashMap<String, VibeSearchHit>()
                textHits.forEach { merged[key(it)] = it }

                if (variants.size > 1) {
                    for (variant in variants.drop(1)) {
                        val more = withContext(Dispatchers.IO) {
                            runCatching {
                                VibeContainer.getGateway().search
                                    .searchMessages(variant, limit = 50)
                                    .first()
                            }.getOrDefault(emptyList())
                        }
                        more.forEach { merged[key(it)] = it }
                        delay(400) // rate-limit guard for Telegram global search
                    }

                    val mergedHits = merged.values.toList()
                    val ranked = AurionSearch.rerank(query, mergedHits)
                    _state.value = SearchUiState.Results(group(ranked))
                }
            } catch (e: Exception) {
                VibeLogger.w("SearchVM", "semantic pass failed", e)
                val current = _state.value
                if (current is SearchUiState.Results) {
                    _state.value = current.copy(semanticFailed = true)
                }
            } finally {
                _semanticBusy.value = false
            }
        }
    }

    fun clear() {
        searchJob?.cancel()
        _semanticBusy.value = false
        _state.value = SearchUiState.Idle
    }

    private fun ensureInitialized() {
        if (!VibeContainer.isInitialized()) {
            VibeContainer.initialize()
        }
    }

    private fun key(hit: VibeSearchHit): String = "${hit.chatId}:${hit.message.id}"

    private fun group(hits: List<VibeSearchHit>): List<SearchGroup> {
        val order = LinkedHashMap<Long, SearchGroup>()
        for (hit in hits) {
            val group = order.getOrPut(hit.chatId) {
                SearchGroup(hit.chatId, hit.chatTitle, mutableListOf())
            }
            (group.hits as MutableList).add(hit)
        }
        return order.values.toList()
    }
}
