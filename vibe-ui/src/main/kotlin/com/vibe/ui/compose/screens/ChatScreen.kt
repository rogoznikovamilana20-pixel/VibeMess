package com.vibe.ui.compose.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibe.ui.compose.components.MessageStatus
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.compose.components.VibeChatBubble
import com.vibe.ui.feature.chat.ChatScreenUiState
import com.vibe.ui.feature.chat.ChatScreenViewModel
import com.vibe.ui.feature.chat.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: Long,
    chatName: String,
    onBack: () -> Unit,
    onOpenCall: (Boolean) -> Unit,
    scrollToMessageId: Long? = null
) {
    val viewModel: ChatScreenViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val scrollTarget by viewModel.scrollTarget.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var showChatMenu by remember { mutableStateOf(false) }
    var selectedMessageId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(chatId, scrollToMessageId) {
        viewModel.load(chatId, scrollToMessageId)
    }

    LaunchedEffect(scrollTarget) {
        val index = scrollTarget ?: return@LaunchedEffect
        try {
            listState.scrollToItem(index)
        } catch (e: Exception) {
            // Index out of bounds — ignore, history is still shown.
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VibeAvatar(name = chatName, size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = chatName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "\u0432 \u0441\u0435\u0442\u0438",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { messageText = "@aurion " }) {
                        Icon(Icons.Default.AutoAwesome, "Aurion",
                            tint = Color(0xFF8D2BFA))
                    }
                    IconButton(onClick = { onOpenCall(false) }) {
                        Icon(Icons.Default.Call, "Call",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { onOpenCall(true) }) {
                        Icon(Icons.Default.Videocam, "Video",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { showChatMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    DropdownMenu(
                        expanded = showChatMenu,
                        onDismissRequest = { showChatMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("В раздел «Личное»", color = Color(0xFF8D2BFA)) },
                            onClick = {
                                showChatMenu = false
                                viewModel.setSection(isPersonal = true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("В раздел «Работа»", color = Color(0xFF10B6FA)) },
                            onClick = {
                                showChatMenu = false
                                viewModel.setSection(isPersonal = false)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { /* TODO: open attachment picker */ }) {
                            Icon(Icons.Default.Add, "Attach",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text("\u0421\u043E\u043E\u0431\u0449\u0435\u043D\u0438\u0435...",
                                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 4
                        )

                        if (messageText.isBlank()) {
                            IconButton(onClick = { isRecording = !isRecording }) {
                                Icon(
                                    if (isRecording) Icons.Default.Mic else Icons.Default.KeyboardVoice,
                                    "Voice",
                                    tint = if (isRecording) Color(0xFFEF4444)
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (messageText.isNotBlank()) {
                            IconButton(onClick = {
                                val msg = messageText.trim()
                                if (msg.isNotEmpty()) {
                                    viewModel.sendMessage(msg)
                                    messageText = ""
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send, "Send",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val s = state) {
            is ChatScreenUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u0417\u0430\u0433\u0440\u0443\u0437\u043A\u0430 \u0441\u043E\u043E\u0431\u0449\u0435\u043D\u0438\u0439...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is ChatScreenUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\u041E\u0448\u0438\u0431\u043A\u0430: ${s.message}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            is ChatScreenUiState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(s.messages, key = { it.id }) { msg ->
                        if (msg.id.startsWith("aurion_")) {
                            AurionBubble(text = msg.text, time = msg.time)
                        } else {
                            Box {
                                VibeChatBubble(
                                    text = msg.text,
                                    isOutgoing = msg.isOutgoing,
                                    time = msg.time,
                                    status = msg.status,
                                    reactions = msg.reactions,
                                    replyPreview = msg.replyPreview,
                                    modifier = Modifier,
                                    onLongClick = if (!msg.isOutgoing) {
                                        { selectedMessageId = msg.id }
                                    } else null
                                )
                                DropdownMenu(
                                    expanded = selectedMessageId == msg.id,
                                    onDismissRequest = { selectedMessageId = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Пожаловаться") },
                                        onClick = {
                                            selectedMessageId = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Заблокировать пользователя") },
                                        onClick = {
                                            selectedMessageId = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (s.aurionTyping) {
                        item(key = "aurion_typing") {
                            AurionTypingIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AurionBubble(text: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.widthIn(max = 280.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2A1A4A),
                                Color(0xFF1A0F2E)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF8D2BFA),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Aurion",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8D2BFA),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AurionTypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF8D2BFA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8D2BFA).copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}
