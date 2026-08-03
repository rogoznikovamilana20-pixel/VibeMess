package com.vibe.ui.compose.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.ui.compose.components.ChatListItemData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ChatListUiState {
    data object Loading : ChatListUiState()
    data class Success(val chats: List<ChatListItemData>) : ChatListUiState()
    data class Error(val message: String) : ChatListUiState()
}

enum class ChatMode { PERSONAL, WORK }

class ChatListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChatListUiState>(ChatListUiState.Loading)
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val _mode = MutableStateFlow(ChatMode.PERSONAL)
    val mode: StateFlow<ChatMode> = _mode.asStateFlow()

    init {
        loadChats()
    }

    fun switchMode(newMode: ChatMode) {
        _mode.value = newMode
        loadChats()
    }

    fun loadChats() {
        viewModelScope.launch {
            _uiState.value = ChatListUiState.Loading
            try {
                // TODO: Wire to ChatRepository / SupabaseChatViewModel when ready
                _uiState.value = ChatListUiState.Success(emptyList())
            } catch (e: Exception) {
                _uiState.value = ChatListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
