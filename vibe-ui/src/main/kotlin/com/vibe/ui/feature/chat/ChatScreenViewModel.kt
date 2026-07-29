package com.vibe.ui.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.bridge.api.INotificationService
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.di.VibeContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.vibe.bridge.model.VibeMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.vibe.ui.compose.components.MessageStatus
import com.vibe.ui.compose.components.BubbleReaction

data class ChatMessage(
    val id: String,
    val text: String,
    val isOutgoing: Boolean,
    val time: String,
    val status: MessageStatus,
    val reactions: List<BubbleReaction> = emptyList(),
    val replyPreview: String? = null
)

sealed interface ChatScreenUiState {
    data object Loading : ChatScreenUiState
    data class Success(
        val messages: List<ChatMessage>,
        val chatId: Long
    ) : ChatScreenUiState
    data class Error(val message: String) : ChatScreenUiState
}

class ChatScreenViewModel : ViewModel() {

    private val _state = MutableStateFlow<ChatScreenUiState>(ChatScreenUiState.Loading)
    val state: StateFlow<ChatScreenUiState> = _state.asStateFlow()

    private var chatId: Long = 0L
    private var observeJob: Job? = null
    private var notificationService: INotificationService? = null

    fun load(chatId: Long) {
        if (this.chatId == chatId && _state.value is ChatScreenUiState.Success) return
        this.chatId = chatId

        if (!VibeContainer.isInitialized()) {
            VibeContainer.initialize()
        }

        viewModelScope.launch {
            try {
                val gateway = withContext(Dispatchers.IO) {
                    VibeContainer.getGateway()
                }
                notificationService = gateway.notifications
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                gateway.messages.getRecentMessages(chatId, 50).catch { e ->
                    VibeLogger.e("ChatScreenVM", "getRecentMessages failed", e)
                    _state.value = ChatScreenUiState.Error(
                        e.message ?: "Failed to load messages"
                    )
                }.collect { msgList ->
                    val mapped = msgList.map { mapMessage(it, timeFormat) }.reversed()
                    _state.value = ChatScreenUiState.Success(
                        messages = mapped,
                        chatId = chatId
                    )
                }

                observeJob?.cancel()
                observeJob = viewModelScope.launch {
                    gateway.notifications.observeNewMessages().catch { e ->
                        VibeLogger.e("ChatScreenVM", "observeNewMessages failed", e)
                    }.collect { newMsgs ->
                        val filtered = newMsgs.filter { it.chatId == chatId }
                        if (filtered.isNotEmpty()) {
                            val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val current = _state.value
                            if (current is ChatScreenUiState.Success) {
                                _state.value = current.copy(
                                    messages = current.messages + filtered.map {
                                        mapMessage(it, tf)
                                    }
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                VibeLogger.e("ChatScreenVM", "Load failed", e)
                _state.value = ChatScreenUiState.Error(
                    e.message ?: "Failed to load chat"
                )
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || chatId == 0L) return

        viewModelScope.launch {
            try {
                val gateway = VibeContainer.getGateway()
                gateway.messages.sendTextMessage(chatId, text)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val localMsg = ChatMessage(
                    id = "tmp_${System.currentTimeMillis()}",
                    text = text,
                    isOutgoing = true,
                    time = timeFormat.format(Date()),
                    status = MessageStatus.SENT
                )
                val current = _state.value
                if (current is ChatScreenUiState.Success) {
                    _state.value = current.copy(
                        messages = current.messages + localMsg
                    )
                }
            } catch (e: Exception) {
                VibeLogger.e("ChatScreenVM", "sendTextMessage failed", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
    }

    private fun mapMessage(msg: VibeMessage, tf: SimpleDateFormat): ChatMessage {
        return ChatMessage(
            id = msg.id.toString(),
            text = msg.text,
            isOutgoing = msg.isOutgoing,
            time = tf.format(Date(msg.date * 1000)),
            status = when (msg.deliveryStatus) {
                com.vibe.bridge.model.VibeDeliveryStatus.PENDING -> MessageStatus.SENT
                com.vibe.bridge.model.VibeDeliveryStatus.SENT -> MessageStatus.DELIVERED
                com.vibe.bridge.model.VibeDeliveryStatus.ERROR -> MessageStatus.SENT
                com.vibe.bridge.model.VibeDeliveryStatus.CANCELLED -> MessageStatus.SENT
                null -> if (msg.isOutgoing) MessageStatus.READ else MessageStatus.READ
            },
            reactions = msg.reactions.map {
                BubbleReaction(it.emoji, it.count, it.isChosen)
            },
            replyPreview = null
        )
    }
}
