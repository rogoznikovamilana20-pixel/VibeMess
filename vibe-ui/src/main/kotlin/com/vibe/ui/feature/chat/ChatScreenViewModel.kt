package com.vibe.ui.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.bridge.api.INotificationService
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.ai.AurionManager
import com.vibe.ui.data.AchievementManager
import com.vibe.ui.di.VibeContainer
import com.vibe.ui.compose.components.MessageStatus
import com.vibe.ui.compose.components.BubbleReaction
import com.vibe.bridge.model.VibeHistoryCursor
import com.vibe.bridge.model.VibeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: String,
    val text: String,
    val isOutgoing: Boolean,
    val time: String,
    val status: MessageStatus = MessageStatus.SENT,
    val reactions: List<BubbleReaction> = emptyList(),
    val replyPreview: String? = null
) {
    companion object {
        fun aurion(text: String, time: String): ChatMessage = ChatMessage(
            id = "aurion_${System.nanoTime()}",
            text = text,
            isOutgoing = false,
            time = time,
            status = MessageStatus.READ
        )
    }
}

sealed interface ChatScreenUiState {
    data object Loading : ChatScreenUiState
    data class Success(
        val messages: List<ChatMessage>,
        val chatId: Long,
        val aurionTyping: Boolean = false
    ) : ChatScreenUiState
    data class Error(val message: String) : ChatScreenUiState
}

class ChatScreenViewModel : ViewModel() {

    private val _state = MutableStateFlow<ChatScreenUiState>(ChatScreenUiState.Loading)
    val state: StateFlow<ChatScreenUiState> = _state.asStateFlow()

    /** Index in [ChatScreenUiState.Success.messages] to scroll to (oldest-first order), or null. */
    private val _scrollTarget = MutableStateFlow<Int?>(null)
    val scrollTarget: StateFlow<Int?> = _scrollTarget.asStateFlow()

    private var chatId: Long = 0L
    private var observeJob: Job? = null
    private var editsJob: Job? = null
    private var jumpJob: Job? = null
    private var notificationService: INotificationService? = null

    fun load(chatId: Long, scrollToMessageId: Long? = null) {
        val alreadyLoaded = this.chatId == chatId && _state.value is ChatScreenUiState.Success
        this.chatId = chatId
        _scrollTarget.value = null
        if (alreadyLoaded) {
            if (scrollToMessageId != null) jumpToMessage(chatId, scrollToMessageId)
            return
        }
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

                // Opening a chat marks it as read on the server
                // (the only documented mutation on the bridge).
                runCatching {
                    gateway.chats.markChatAsRead(chatId)
                }.onFailure {
                    VibeLogger.w("ChatScreenVM", "markChatAsRead failed", it)
                }

                if (scrollToMessageId != null) {
                    jumpToMessage(chatId, scrollToMessageId)
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
                                val existingIds = current.messages.map { it.id }.toSet()
                                val newMapped = filtered.map { mapMessage(it, tf) }
                                    .filter { it.id !in existingIds }
                                if (newMapped.isNotEmpty()) {
                                    _state.value = current.copy(
                                        messages = current.messages + newMapped
                                    )
                                }
                            }
                        }
                    }
                }
                editsJob?.cancel()
                editsJob = viewModelScope.launch {
                    gateway.notifications.observeMessageEdits().catch { e ->
                        VibeLogger.e("ChatScreenVM", "observeMessageEdits failed", e)
                    }.collect { edited ->
                        if (edited.chatId != chatId) return@collect
                        val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val current = _state.value
                        if (current is ChatScreenUiState.Success) {
                            val replaced = current.messages.map { m ->
                                if (m.id == edited.id.toString() || edited.localId?.toString() == m.id) {
                                    mapMessage(edited, tf)
                                } else {
                                    m
                                }
                            }
                            _state.value = current.copy(messages = replaced)
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

    /**
     * Loads a page of history centered around [messageId] and scrolls to it.
     * The page keeps the newest-first Telegram order (matches search results order).
     */
    private fun jumpToMessage(chatId: Long, messageId: Long) {
        jumpJob?.cancel()
        jumpJob = viewModelScope.launch {
            try {
                val gateway = withContext(Dispatchers.IO) {
                    VibeContainer.getGateway()
                }
                val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val cursor = VibeHistoryCursor(
                    chatId = chatId,
                    maxId = messageId + 1,
                    offsetDate = 0,
                    isEnd = false
                )
                gateway.messages.getMessageHistory(chatId, cursor, 50)
                    .catch { e ->
                        VibeLogger.w("ChatScreenVM", "jumpToMessage failed", e)
                    }
                    .collect { page ->
                        val mapped = page.messages.map { mapMessage(it, tf) }
                        _state.value = ChatScreenUiState.Success(
                            messages = mapped,
                            chatId = chatId
                        )
                        val targetIndex = mapped.indexOfFirst { it.id == messageId.toString() }
                        _scrollTarget.value = if (targetIndex >= 0) targetIndex else null
                    }
            } catch (e: Exception) {
                VibeLogger.e("ChatScreenVM", "jumpToMessage failed", e)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || chatId == 0L) return

        val current = _state.value
        if (current is ChatScreenUiState.Success) {
            _state.value = current.copy(aurionTyping = true)
        }

        AurionManager.chatAsync(
            chatId = chatId,
            text = text,
            onResponse = { response ->
                val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val aurionMsg = ChatMessage.aurion(response, tf.format(Date()))
                val s = _state.value
                if (s is ChatScreenUiState.Success) {
                    _state.value = s.copy(
                        messages = s.messages + aurionMsg,
                        aurionTyping = false
                    )
                }
            },
            onError = { _ ->
                val s = _state.value
                if (s is ChatScreenUiState.Success) {
                    _state.value = s.copy(aurionTyping = false)
                }
            }
        )

        viewModelScope.launch {
            try {
                val gateway = VibeContainer.getGateway()
                gateway.messages.sendTextMessage(chatId, text)
                AchievementManager(ApplicationLoader.applicationContext).trackMessageSent()
            } catch (e: Exception) {
                VibeLogger.e("ChatScreenVM", "sendTextMessage failed", e)
            }
        }
    }

    /**
     * Moves the current chat into a section (Личное/Работа).
     */
    fun setSection(isPersonal: Boolean) {
        if (chatId == 0L) return
        viewModelScope.launch {
            runCatching {
                val db = com.vibe.ui.data.db.VibeDatabase.getDatabase(ApplicationLoader.applicationContext)
                val account = VibeContainer.getGateway().accounts.getCurrentAccount()
                com.vibe.ui.data.repository.ChatRepository(db).setPersonal(account.userId, chatId, isPersonal)
            }.onFailure {
                VibeLogger.e("ChatScreenVM", "setSection failed", it)
            }
        }
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

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
        editsJob?.cancel()
        jumpJob?.cancel()
    }
}
