package com.vibe.ui.ai

import com.vibe.ui.data.AchievementManager
import com.vibe.common.logging.VibeLogger
import org.telegram.messenger.ApplicationLoader
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object AurionManager {

    private val crashHandler = CoroutineExceptionHandler { _, e ->
        VibeLogger.e("AurionManager", "background coroutine crashed", e)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + crashHandler)

    private var currentProvider: AiProvider = ZvenoAIProvider()

    private val _messageHistory = mutableMapOf<Long, MutableList<AiMessage>>()
    private val _typing = MutableStateFlow(false)
    val typing: StateFlow<Boolean> = _typing.asStateFlow()

    private const val SYSTEM_PROMPT = """
Ты — Aurion, AI-ассистент мессенджера Vibe. Помогай пользователям в общении,
отвечай на вопросы кратко (2-5 предложений), дружелюбно и по делу.
Ты работаешь с большим контекстом (до 128K токенов).
Не упоминай название своей модели."""

    fun updateApiKey(key: String) {
        if (key.isNotBlank()) {
            currentProvider = ZvenoAIProvider(apiKey = key)
        }
    }

    fun isAvailable(): Boolean = true

    fun chatAsync(chatId: Long, text: String, onResponse: (String) -> Unit, onError: (String) -> Unit) {
        scope.launch {
            _typing.value = true
            try {
                val history = _messageHistory.getOrPut(chatId) { mutableListOf() }
                history.add(AiMessage(AiRole.USER, text))

                val messages = mutableListOf(AiMessage(AiRole.SYSTEM, SYSTEM_PROMPT))
                messages.addAll(history.takeLast(50))

                val response = currentProvider.chat(messages)
                history.add(AiMessage(AiRole.ASSISTANT, response.content))
                AchievementManager(ApplicationLoader.applicationContext)
                    .unlock(AchievementManager.Id.FIRST_AI)
                onResponse(response.content)
            } catch (e: Exception) {
                onError(e.message ?: "Неизвестная ошибка")
            } finally {
                _typing.value = false
            }
        }
    }

    fun clearHistory(chatId: Long) {
        _messageHistory.remove(chatId)
    }
}
