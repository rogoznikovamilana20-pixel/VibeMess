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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibe.common.logging.VibeLogger
import com.vibe.bridge.model.VibeMessage
import com.vibe.ui.compose.components.BubbleReaction
import com.vibe.ui.compose.components.MessageStatus
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.compose.components.VibeChatBubble
import com.vibe.ui.di.VibeContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job

data class ChatMessage(
    val id: String,
    val text: String,
    val isOutgoing: Boolean,
    val time: String,
    val status: MessageStatus,
    val reactions: List<BubbleReaction> = emptyList(),
    val replyPreview: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: Long,
    chatName: String,
    onBack: () -> Unit,
    onOpenCall: (Boolean) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var observeJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(chatId) {
        if (VibeContainer.isInitialized()) {
            try {
                val gateway = withContext(Dispatchers.IO) {
                    VibeContainer.getGateway()
                }
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                gateway.messages.getRecentMessages(chatId, 50).collect { msgList ->
                    messages = msgList.map { mapMessage(it, timeFormat) }.reversed()
                    isLoading = false
                }

                observeJob = launch {
                    gateway.notifications.observeNewMessages().collect { newMsgs ->
                        val filtered = newMsgs.filter { it.chatId == chatId }
                        if (filtered.isNotEmpty()) {
                            val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            messages = messages + filtered.map { mapMessage(it, tf) }
                        }
                    }
                }
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки"
                isLoading = false
            }
        } else {
            isLoading = false
            error = "Bridge не инициализирован"
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
                                text = "в сети",
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
                            IconButton(onClick = { onOpenCall(false) }) {
                        Icon(Icons.Default.Call, "Call",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { onOpenCall(true) }) {
                        Icon(Icons.Default.Videocam, "Video",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { /* menu */ }) {
                        Icon(Icons.Default.MoreVert, "Menu",
                            tint = MaterialTheme.colorScheme.onBackground)
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
                        IconButton(onClick = { /* attachments */ }) {
                            Icon(Icons.Default.Add, "Attach",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text("Сообщение...",
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
                                    scope.launch {
                                        try {
                                            if (VibeContainer.isInitialized()) {
                                                VibeContainer.getGateway().messages.sendTextMessage(chatId, msg)
                                            }
                                        } catch (e: Exception) {
                                            VibeLogger.e("ChatScreen", "sendTextMessage failed", e)
                                        }
                                    }
                                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    messages = messages + ChatMessage(
                                        "tmp_${System.currentTimeMillis()}",
                                        msg, true, timeFormat.format(Date()), MessageStatus.SENT
                                    )
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Загрузка сообщений...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ошибка: $error",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Используются демо-данные",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    VibeChatBubble(
                        text = msg.text,
                        isOutgoing = msg.isOutgoing,
                        time = msg.time,
                        status = msg.status,
                        reactions = msg.reactions,
                        replyPreview = msg.replyPreview,
                        modifier = Modifier
                    )
                }
            }
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
        reactions = msg.reactions.map { BubbleReaction(it.emoji, it.count, it.isChosen) },
        replyPreview = null
    )
}


