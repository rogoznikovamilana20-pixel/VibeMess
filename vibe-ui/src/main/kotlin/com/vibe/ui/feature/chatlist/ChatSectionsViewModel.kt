package com.vibe.ui.feature.chatlist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.ai.AurionClassifier
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.ChatEntity
import com.vibe.ui.data.repository.ChatRepository
import com.vibe.ui.di.VibeContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.telegram.messenger.ApplicationLoader

data class ChatChange(
    val chatId: Long,
    val chatTitle: String,
    val isPersonal: Boolean
)

data class ClassificationPreview(
    val changes: List<ChatChange>,
    val personalCount: Int,
    val workCount: Int
)

sealed interface ChatSectionsUiState {
    data object Loading : ChatSectionsUiState
    data class Success(
        val chats: List<ChatEntity>,
        val userName: String = "",
        val userTag: String = ""
    ) : ChatSectionsUiState
    data class Error(val message: String) : ChatSectionsUiState
}

/**
 * ViewModel for the MainScreen chat list backed by Room.
 *
 * Provides:
 *  - chat list filtered by the current mode (Личное/Работа) via Room flows;
 *  - manual override of a chat's section ([overrideChat]);
 *  - AI classification preview + apply/dismiss ([classifyChats], [applyClassification],
 *    [dismissClassification]);
 *  - one-time «Разложить чаты?» hint per account ([showHint]).
 */
class ChatSectionsViewModel : ViewModel() {

    private val appContext = ApplicationLoader.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val repo = ChatRepository(VibeDatabase.getDatabase(appContext))

    private var accountId: Long = 0L
    private var userName: String = ""
    private var userTag: String = ""

    private val _isPersonalMode = MutableStateFlow(true)

    private val _state = MutableStateFlow<ChatSectionsUiState>(ChatSectionsUiState.Loading)
    val state: StateFlow<ChatSectionsUiState> = _state.asStateFlow()

    private val _showHint = MutableStateFlow(false)
    val showHint: StateFlow<Boolean> = _showHint.asStateFlow()

    private val _classification = MutableStateFlow<ClassificationPreview?>(null)
    val classification: StateFlow<ClassificationPreview?> = _classification.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                if (!VibeContainer.isInitialized()) {
                    VibeContainer.initialize()
                }
                val gateway = VibeContainer.getGateway()
                accountId = gateway.accounts.getCurrentAccount().userId

                viewModelScope.launch {
                    try {
                        val user = gateway.users.getUser(accountId)
                        user?.let {
                            userName = it.firstName + (it.lastName?.let { " $it" } ?: "")
                            userTag = "@${it.username ?: accountId.toString()}"
                        }
                    } catch (e: Exception) {
                        VibeLogger.e("ChatSectionsVM", "Failed to load user profile", e)
                    }
                }

                _isPersonalMode.flatMapLatest { personal ->
                    if (personal) repo.getPersonalChats(accountId)
                    else repo.getWorkChats(accountId)
                }.catch { e ->
                    VibeLogger.e("ChatSectionsVM", "Chat flow failed", e)
                    _state.value = ChatSectionsUiState.Error(
                        e.message ?: "Failed to load chats"
                    )
                }.collect { chats ->
                    _state.value = ChatSectionsUiState.Success(
                        chats = chats,
                        userName = userName,
                        userTag = userTag
                    )
                }

                repo.getAllChats(accountId).collect { all ->
                    val hint = !prefs.getBoolean("hint_shown_$accountId", false) &&
                        !prefs.getBoolean("classified_$accountId", false) &&
                        all.size > 3
                    _showHint.value = hint
                }
            } catch (e: Exception) {
                VibeLogger.e("ChatSectionsVM", "Failed to init", e)
                _state.value = ChatSectionsUiState.Error(
                    e.message ?: "Failed to initialize chat service"
                )
            }
        }
    }

    fun selectMode(isPersonal: Boolean) {
        _isPersonalMode.value = isPersonal
    }

    fun overrideChat(chatId: Long, isPersonal: Boolean) {
        viewModelScope.launch {
            runCatching {
                repo.setPersonal(accountId, chatId, isPersonal)
            }.onFailure { VibeLogger.e("ChatSectionsVM", "overrideChat failed", it) }
        }
    }

    fun classifyChats() {
        viewModelScope.launch {
            try {
                val all = withContext(Dispatchers.IO) { repo.getAllChats(accountId).first() }
                if (all.isEmpty()) {
                    _showHint.value = false
                    return@launch
                }
                val result = AurionClassifier.classify(all)
                val changes = result.mapNotNull { (chatId, isPersonal) ->
                    val chat = all.firstOrNull { it.id == chatId } ?: return@mapNotNull null
                    if (chat.isPersonal == isPersonal) null
                    else ChatChange(chatId, chat.title, isPersonal)
                }
                val personalCount = result.count { it.value }
                val workCount = result.size - personalCount
                _classification.value = ClassificationPreview(
                    changes = changes,
                    personalCount = personalCount,
                    workCount = workCount
                )
            } catch (e: Exception) {
                VibeLogger.e("ChatSectionsVM", "classifyChats failed", e)
            }
        }
    }

    fun applyClassification() {
        val preview = _classification.value ?: return
        viewModelScope.launch {
            preview.changes.forEach { change ->
                runCatching {
                    repo.setPersonal(accountId, change.chatId, change.isPersonal)
                }.onFailure {
                    VibeLogger.e("ChatSectionsVM", "applyClassification failed", it)
                }
            }
            prefs.edit()
                .putBoolean("classified_$accountId", true)
                .putBoolean("hint_shown_$accountId", true)
                .apply()
            _classification.value = null
            _showHint.value = false
        }
    }

    fun dismissClassification() {
        _classification.value = null
        prefs.edit().putBoolean("hint_shown_$accountId", true).apply()
        _showHint.value = false
    }

    fun dismissHint() {
        prefs.edit().putBoolean("hint_shown_$accountId", true).apply()
        _showHint.value = false
    }

    companion object {
        private const val PREFS_NAME = "vibe_sections"
    }
}
