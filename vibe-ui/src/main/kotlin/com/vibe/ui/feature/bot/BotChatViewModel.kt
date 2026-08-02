package com.vibe.ui.feature.bot

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.data.bot.BotEngine
import com.vibe.ui.data.bot.BotRepository
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.BotEntity
import com.vibe.ui.data.db.entity.BotMessageEntity
import com.vibe.ui.network.ServerConfig
import com.vibe.ui.network.VibeHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BotChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val time: String
)

sealed interface BotChatUiState {
    data object Loading : BotChatUiState
    data class Success(
        val bot: BotEntity,
        val messages: List<BotChatMessage>,
        val typing: Boolean = false
    ) : BotChatUiState
    data class Error(val message: String) : BotChatUiState
}

class BotChatViewModel : ViewModel() {

    private val tag = "BotChatViewModel"
    private val _state = MutableStateFlow<BotChatUiState>(BotChatUiState.Loading)
    val state: StateFlow<BotChatUiState> = _state.asStateFlow()

    private var botId: Long = 0L
    private var appContext: Context? = null
    private var db: VibeDatabase? = null
    private var repo: BotRepository? = null
    private var historyJob: Job? = null
    private var replyPollJob: Job? = null
    private var lastReplyId: Long = 0L

    fun load(appContext: Context, id: Long) {
        if (this.botId == id && _state.value is BotChatUiState.Success) return
        this.botId = id
        this.appContext = appContext.applicationContext
        this.db = VibeDatabase.getDatabase(appContext)
        this.repo = BotRepository(appContext)

        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            val bot = repo?.getBot(id)
            if (bot == null) {
                _state.value = BotChatUiState.Error("Bot not found")
                return@launch
            }
            val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
            db?.botMessageDao()?.getByBotId(id)?.collect { messages ->
                _state.value = BotChatUiState.Success(
                    bot = bot,
                    messages = messages.map {
                        BotChatMessage(
                            id = it.id,
                            text = it.text,
                            isUser = it.isUser,
                            time = tf.format(Date(it.timestamp))
                        )
                    }
                )
            }
        }
    }

    fun send(text: String) {
        val current = _state.value
        if (current !is BotChatUiState.Success || text.isBlank()) return
        val bot = current.bot

        viewModelScope.launch {
            val db = db ?: return@launch
            db.botMessageDao().insert(
                BotMessageEntity(botId = bot.id, text = text, isUser = true)
            )
            _state.value = current.copy(typing = true)

            val history = db.botMessageDao().getByBotId(bot.id).first()

                val serverHandled = if (!bot.isLocal && bot.token.isNotBlank()) {
                    try {
                        val ctx = appContext ?: return@launch
                        val http = VibeHttpClient(ServerConfig(ctx))
                    val senderId = http.rustEnsureIdentity()
                    if (senderId != null) {
                        val sent = repo?.sendToServerBot(bot, senderId, text) == true
                        if (sent) {
                            startReplyPolling(bot, senderId)
                        }
                        sent
                    } else false
                } catch (e: Exception) {
                    VibeLogger.e(tag, "server send failed", e)
                    false
                }
            } else false

            if (!serverHandled) {
                val reply = BotEngine.reply(bot, text, history)
                if (reply != null) {
                    db.botMessageDao().insert(
                        BotMessageEntity(botId = bot.id, text = reply, isUser = false)
                    )
                }
                val s = _state.value
                if (s is BotChatUiState.Success) {
                    _state.value = s.copy(typing = false)
                }
            }
        }
    }

    private fun startReplyPolling(bot: BotEntity, senderId: String) {
        replyPollJob?.cancel()
        replyPollJob = viewModelScope.launch(Dispatchers.IO) {
            var attempts = 0
            while (attempts < 40) {
                delay(3_000)
                attempts++
                try {
                    val replies = repo?.pollServerReplies(bot, senderId, lastReplyId) ?: emptyList()
                    if (replies.isEmpty()) continue
                    val newOnes = replies.filter { it.id > lastReplyId }
                    if (newOnes.isEmpty()) continue
                    lastReplyId = newOnes.maxOf { it.id }
                    val db = db ?: continue
                    val s = _state.value
                    if (s is BotChatUiState.Success) {
                        _state.value = s.copy(typing = false)
                    }
                    newOnes.forEach { r ->
                        db.botMessageDao().insert(
                            BotMessageEntity(botId = bot.id, text = r.reply, isUser = false)
                        )
                    }
                } catch (e: Exception) {
                    VibeLogger.e(tag, "reply poll failed", e)
                }
            }
            val s = _state.value
            if (s is BotChatUiState.Success) {
                _state.value = s.copy(typing = false)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            db?.botMessageDao()?.clearByBotId(botId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        historyJob?.cancel()
        replyPollJob?.cancel()
    }
}
