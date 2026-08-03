package com.vibe.ui.compose.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibe.ui.i18n.VibeI18n
import com.vibe.ui.compose.components.ShowToast
import com.vibe.ui.compose.components.SkeletonMessageList
import com.vibe.ui.compose.components.AvatarPlaceholder
import com.vibe.ui.compose.components.FormattedText
import com.vibe.ui.feature.chat.SupabaseChatState
import com.vibe.ui.feature.chat.SupabaseChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseChatScreen(
    chatId: String,
    chatTitle: String,
    onBack: () -> Unit
) {
    val viewModel: SupabaseChatViewModel = viewModel(key = "chat_detail")
    val state by viewModel.state.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId, chatTitle)
        viewModel.startTypingPolling(chatId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTypingPolling()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(chatTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        val typingUsers by viewModel.typingUsers.collectAsState()
                        if (typingUsers.isNotEmpty()) {
                            Text(
                                VibeI18n.t("typing"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val replyMsg by viewModel.replyTo.collectAsState()
                replyMsg?.let { reply ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            VibeI18n.t("reply_prefix") + reply.content.take(40) + if (reply.content.length > 40) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.setReplyTo(null) }) {
                            Text("✕", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            viewModel.setTyping(chatId, it.isNotBlank())
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(VibeI18n.t("message") + "...", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    if (messageText.isNotBlank()) {
                        IconButton(onClick = {
                            val msg = messageText.trim()
                            if (msg.isNotEmpty()) {
                                viewModel.sendMessage(chatId, msg)
                                messageText = ""
                                viewModel.setTyping(chatId, false)
                                viewModel.setReplyTo(null)
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val s = state) {
            is SupabaseChatState.Messages -> {
                val myUserId = remember {
                    try {
                        val token = com.vibe.ui.network.ServerConfig(context).getAuthToken()
                        com.vibe.ui.network.SupabaseClient.getUserIdFromToken(token)
                    } catch (_: Exception) { "" }
                }

                LaunchedEffect(s.messages.size) {
                    if (s.messages.isNotEmpty()) {
                        listState.animateScrollToItem(s.messages.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var lastDate = ""
                    items(s.messages, key = { it.id }) { msg ->
                        val isOutgoing = msg.senderId == myUserId
                        val senderProfile = viewModel.getProfile(msg.senderId)
                        val msgDate = com.vibe.ui.compose.components.DateUtils.formatDate(msg.createdAt)

                        if (msgDate != lastDate) {
                            lastDate = msgDate
                            com.vibe.ui.compose.components.DateHeader(msg.createdAt)
                        }

                        var swipeOffset by remember { mutableStateOf(0f) }
                        val swipeThreshold = 100f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(msg.id) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            if (swipeOffset < -swipeThreshold) {
                                                viewModel.setReplyTo(msg)
                                            }
                                            swipeOffset = 0f
                                        },
                                        onHorizontalDrag = { _, dragAmount ->
                                            swipeOffset = (swipeOffset + dragAmount).coerceIn(-200f, 0f)
                                        }
                                    )
                                }
                        ) {
                            if (swipeOffset < -30f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Reply",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
                                ) {
                                    if (!isOutgoing) {
                                        if (senderProfile?.avatarUrl != null && senderProfile.avatarUrl != "null") {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(senderProfile.avatarUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            AvatarPlaceholder(
                                                name = senderProfile?.displayName ?: "?",
                                                size = 32
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.widthIn(max = 280.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isOutgoing) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    FormattedText(
                                        text = msg.content,
                                        color = if (isOutgoing) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                    }
                                    if (isOutgoing) Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                        }
                    }
                }
            }
            is SupabaseChatState.Loading -> {
                SkeletonMessageList(modifier = Modifier.padding(padding))
            }
            is SupabaseChatState.Error -> {
                ShowToast(s.message)
            }
            else -> {}
        }
    }
}
