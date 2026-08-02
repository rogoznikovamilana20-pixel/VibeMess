package com.vibe.ui.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibe.bridge.model.VibeChat
import com.vibe.bridge.model.VibeSearchHit
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.compose.components.LoadingDots
import com.vibe.ui.di.VibeContainer
import com.vibe.ui.feature.search.SearchUiState
import com.vibe.ui.feature.search.SearchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.retry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SearchMode { CHATS, MESSAGES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchMessagesScreen(
    onBack: () -> Unit,
    onOpenChat: (chatId: Long, chatName: String) -> Unit,
    onOpenMessage: (chatId: Long, chatName: String, messageId: Long) -> Unit
) {
    val searchViewModel: SearchViewModel = viewModel()
    val searchState by searchViewModel.state.collectAsState()
    val semanticBusy by searchViewModel.semanticBusy.collectAsState()

    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(SearchMode.MESSAGES) }
    val focusRequester = remember { FocusRequester() }
    val timeFormat = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }

    var chats by remember { mutableStateOf<List<VibeChat>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (!VibeContainer.isInitialized()) {
            VibeContainer.initialize()
        }
        VibeContainer.getGateway().chats.getActiveChats()
            .retry(3) { true }
            .catch { }
            .collect { loaded ->
                chats = loaded.sortedByDescending { it.lastActivityDate }
            }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(query, mode) {
        if (mode == SearchMode.MESSAGES) {
            delay(450)
            searchViewModel.search(query)
        } else {
            searchViewModel.clear()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Поиск") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, "Очистить")
                                }
                            }
                        } else null
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == SearchMode.CHATS,
                    onClick = { mode = SearchMode.CHATS },
                    label = { Text("Чаты") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = mode == SearchMode.MESSAGES,
                    onClick = { mode = SearchMode.MESSAGES },
                    label = { Text("Сообщения") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            if (semanticBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when (mode) {
                SearchMode.CHATS -> ChatsResults(
                    chats = chats,
                    query = query.trim(),
                    onOpenChat = onOpenChat
                )
                SearchMode.MESSAGES -> MessagesResults(
                    state = searchState,
                    query = query.trim(),
                    timeFormat = timeFormat,
                    onOpenMessage = onOpenMessage,
                    onRetry = { searchViewModel.search(query) }
                )
            }
        }
    }
}

@Composable
private fun ChatsResults(
    chats: List<VibeChat>,
    query: String,
    onOpenChat: (Long, String) -> Unit
) {
    val filtered = if (query.isBlank()) chats
        else chats.filter { it.title.contains(query, ignoreCase = true) }

    if (chats.isEmpty()) {
        EmptyHint("Загрузка чатов...")
        return
    }
    if (filtered.isEmpty()) {
        EmptyHint("Ничего не найдено")
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(filtered, key = { it.id }) { chat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenChat(chat.id, chat.title) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VibeAvatar(name = chat.title, size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    chat.lastMessage?.let {
                        Text(
                            text = it.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagesResults(
    state: SearchUiState,
    query: String,
    timeFormat: SimpleDateFormat,
    onOpenMessage: (Long, String, Long) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is SearchUiState.Idle -> EmptyHint("Введите запрос")

        is SearchUiState.Searching -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingDots(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Поиск...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        is SearchUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ошибка поиска",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRetry) { Text("Повторить") }
                }
            }
        }

        is SearchUiState.Results -> {
            if (state.semanticFailed) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Семантика недоступна — показан текстовый поиск",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (state.groups.isEmpty()) {
                EmptyHint("Ничего не найдено")
                return
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.groups, key = { it.chatId }) { group ->
                    Text(
                        text = group.chatTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    group.hits.forEach { hit ->
                        SearchHitRow(
                            hit = hit,
                            query = query,
                            time = timeFormat.format(Date(hit.message.date * 1000)),
                            onClick = { onOpenMessage(hit.chatId, hit.chatTitle, hit.message.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHitRow(
    hit: VibeSearchHit,
    query: String,
    time: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (hit.message.isOutgoing) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(4.dp)
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlight(hit.message.text.ifBlank { "Вложение" }, query, MaterialTheme.colorScheme.primary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
private fun highlight(text: String, query: String, highlightColor: Color): androidx.compose.ui.text.AnnotatedString {
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    if (query.isBlank()) {
        return buildAnnotatedString { append(text) }
    }
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val match = lowerText.indexOf(lowerQuery, startIndex = index)
            if (match < 0) {
                append(text.substring(index))
                break
            }
            append(text.substring(index, match))
            withStyle(
                SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(text.substring(match, match + lowerQuery.length))
            }
            index = match + lowerQuery.length
        }
    }
}
