package com.vibe.ui.feature.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.bridge.api.IChatService
import com.vibe.bridge.model.VibeChat
import com.vibe.bridge.model.VibeUser
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.di.VibeContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch

sealed interface ChatListUiState {
    data object Loading : ChatListUiState
    data class Success(
        val chats: List<VibeChat>,
        val userName: String = "",
        val userTag: String = ""
    ) : ChatListUiState
    data class Error(val message: String) : ChatListUiState
}

class ChatListViewModel : ViewModel() {

    private val _state = MutableStateFlow<ChatListUiState>(ChatListUiState.Loading)
    val state: StateFlow<ChatListUiState> = _state.asStateFlow()

    private var chatService: IChatService? = null
    private var userName: String = ""
    private var userTag: String = ""

    init {
        load()
    }

    fun load() {
        if (!VibeContainer.isInitialized()) {
            val ok = VibeContainer.initialize()
            if (!ok) {
                _state.value = ChatListUiState.Error(
                    VibeContainer.getInitializationError()?.message ?: "Bridge initialization failed"
                )
                return
            }
        }

        try {
            val gateway = VibeContainer.getGateway()
            chatService = gateway.chats

            viewModelScope.launch {
                try {
                    val account = gateway.accounts.getCurrentAccount()
                    val user = gateway.users.getUser(account.userId)
                    user?.let {
                        userName = it.firstName + (it.lastName?.let { " $it" } ?: "")
                        userTag = "@${it.username ?: account.userId.toString()}"
                    }
                } catch (e: Exception) {
                    VibeLogger.e("ChatListVM", "Failed to load user profile", e)
                }
            }

            viewModelScope.launch {
                gateway.chats.getActiveChats()
                    .retry(3) { cause ->
                        VibeLogger.e("ChatListVM", "Chat flow error, retrying", cause)
                        true
                    }
                    .catch { e ->
                        VibeLogger.e("ChatListVM", "Chat flow failed", e)
                        _state.value = ChatListUiState.Error(
                            e.message ?: "Failed to load chats"
                        )
                    }
                    .collect { chatList ->
                        _state.value = ChatListUiState.Success(
                            chats = chatList.sortedByDescending { it.lastActivityDate },
                            userName = userName,
                            userTag = userTag
                        )
                    }
            }
        } catch (e: Exception) {
            VibeLogger.e("ChatListVM", "Failed to get gateway", e)
            _state.value = ChatListUiState.Error(
                e.message ?: "Failed to initialize chat service"
            )
        }
    }

    fun retry() {
        _state.value = ChatListUiState.Loading
        load()
    }
}
