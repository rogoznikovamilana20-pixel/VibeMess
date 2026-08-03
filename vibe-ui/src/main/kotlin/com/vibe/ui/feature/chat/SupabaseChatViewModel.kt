package com.vibe.ui.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.ui.BuildConfig
import com.vibe.ui.network.ServerConfig
import com.vibe.ui.network.SupabaseClient
import com.vibe.ui.network.MessageCache
import com.vibe.ui.VibeAppContext
import com.vibe.ui.focus.FocusModeManager
import com.vibe.ui.focus.FocusSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SupabaseChatState {
    data object Loading : SupabaseChatState
    data class ChatList(val chats: List<SupabaseClient.Chat>) : SupabaseChatState
    data class Messages(val chatId: String, val chatTitle: String, val messages: List<SupabaseClient.Message>) : SupabaseChatState
    data class Error(val message: String) : SupabaseChatState
}

class SupabaseChatViewModel : ViewModel() {

    private val context = VibeAppContext.get()
    private val serverConfig = ServerConfig(context)
    private val cache = MessageCache(context)

    private val _state = MutableStateFlow<SupabaseChatState>(SupabaseChatState.Loading)
    val state: StateFlow<SupabaseChatState> = _state.asStateFlow()

    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    val typingUsers: StateFlow<List<String>> = _typingUsers.asStateFlow()

    private val _replyTo = MutableStateFlow<SupabaseClient.Message?>(null)
    val replyTo: StateFlow<SupabaseClient.Message?> = _replyTo.asStateFlow()

    private val profileCache = mutableMapOf<String, SupabaseClient.Profile>()
    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private var retryCount = 0
    private val maxRetries = 3

    private fun getToken(): String = serverConfig.getAuthToken()

    fun retry() {
        retryCount++
        if (retryCount <= maxRetries) {
            loadChats()
        }
    }

    fun getProfile(userId: String): SupabaseClient.Profile? = profileCache[userId]

    fun loadChats() {
        viewModelScope.launch {
            val token = getToken()
            if (token.isBlank()) {
                _state.value = SupabaseChatState.Error("Необходима авторизация")
                return@launch
            }
            val space = FocusModeManager.currentSpace.value.name.lowercase()
            val chats = SupabaseClient.getChats(supabaseUrl, anonKey, token, space)
            if (chats.isNotEmpty()) {
                cache.saveChats(chats)
                _state.value = SupabaseChatState.ChatList(chats)
            } else {
                val cached = cache.getChats().filter { it.space == space }
                if (cached.isNotEmpty()) {
                    _state.value = SupabaseChatState.ChatList(cached)
                } else {
                    _state.value = SupabaseChatState.ChatList(emptyList())
                }
            }
        }
    }

    fun moveChatToSpace(chatId: String, space: String) {
        viewModelScope.launch {
            val token = getToken()
            SupabaseClient.updateChatSpace(supabaseUrl, anonKey, token, chatId, space)
            loadChats()
        }
    }

    fun loadMessages(chatId: String, chatTitle: String) {
        viewModelScope.launch {
            val token = getToken()
            val messages = SupabaseClient.getMessages(supabaseUrl, anonKey, token, chatId)
            if (messages.isNotEmpty()) {
                cache.saveMessages(chatId, messages)
                _state.value = SupabaseChatState.Messages(chatId, chatTitle, messages)
                loadProfiles(messages.map { it.senderId }.distinct())
            } else {
                val cached = cache.getMessages(chatId)
                _state.value = SupabaseChatState.Messages(chatId, chatTitle, cached)
                loadProfiles(cached.map { it.senderId }.distinct())
            }
        }
    }

    private fun loadProfiles(userIds: List<String>) {
        viewModelScope.launch {
            val token = getToken()
            val profiles = SupabaseClient.getProfiles(supabaseUrl, anonKey, token, userIds)
            profileCache.putAll(profiles)
        }
    }

    fun sendMessage(chatId: String, content: String) {
        viewModelScope.launch {
            val token = getToken()
            val msg = SupabaseClient.sendMessage(supabaseUrl, anonKey, token, chatId, content)
            if (msg != null) {
                val current = _state.value
                if (current is SupabaseChatState.Messages) {
                    _state.value = current.copy(messages = current.messages + msg)
                }
            }
        }
    }

    fun setTyping(chatId: String, typing: Boolean) {
        viewModelScope.launch {
            val token = getToken()
            SupabaseClient.setTyping(supabaseUrl, anonKey, token, chatId, typing)
        }
    }

    fun setReplyTo(message: SupabaseClient.Message?) {
        _replyTo.value = message
    }

    private var typingPollingJob: kotlinx.coroutines.Job? = null

    fun startTypingPolling(chatId: String) {
        typingPollingJob?.cancel()
        typingPollingJob = viewModelScope.launch {
            while (true) {
                val token = getToken()
                val users = SupabaseClient.getTypingUsers(supabaseUrl, anonKey, token, chatId)
                _typingUsers.value = users
                delay(3000)
            }
        }
    }

    fun stopTypingPolling() {
        typingPollingJob?.cancel()
        typingPollingJob = null
        _typingUsers.value = emptyList()
    }

    fun createChat(title: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val token = getToken()
            val space = FocusModeManager.currentSpace.value.name.lowercase()
            val chatId = SupabaseClient.createChat(supabaseUrl, anonKey, token, title, space = space)
            if (chatId != null) {
                onComplete(chatId)
            }
        }
    }

    fun createWelcomeChat(onComplete: () -> Unit) {
        viewModelScope.launch {
            val token = getToken()
            val existingChats = SupabaseClient.getChats(supabaseUrl, anonKey, token)
            val welcomeChat = existingChats.find { it.title == "Добро пожаловать в Vibe" }
            if (welcomeChat != null) {
                onComplete()
                return@launch
            }

            val chatId = SupabaseClient.createChat(supabaseUrl, anonKey, token, "Добро пожаловать в Vibe")
            if (chatId != null) {
                SupabaseClient.sendMessage(supabaseUrl, anonKey, token, chatId,
                    "Привет! Это Vibe — безопасный мессенджер с end-to-end шифрованием.")
                SupabaseClient.sendMessage(supabaseUrl, anonKey, token, chatId,
                    "Здесь ты можешь отправлять сообщения, совершать голосовые и видеозвонки.")
                SupabaseClient.sendMessage(supabaseUrl, anonKey, token, chatId,
                    "Попробуй написать сообщение!")
            }
            onComplete()
        }
    }
}
