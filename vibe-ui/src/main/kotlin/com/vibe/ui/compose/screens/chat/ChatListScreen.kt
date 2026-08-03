package com.vibe.ui.compose.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.ui.compose.components.ChatListItem
import com.vibe.ui.compose.components.VibeBottomNavigation
import com.vibe.ui.compose.components.BottomNavTab
import com.vibe.ui.compose.components.VibeTopBar
import com.vibe.ui.compose.theme.TextPrimaryDark
import com.vibe.ui.compose.theme.TextSecondaryDark
import com.vibe.ui.compose.theme.TextTertiaryDark
import com.vibe.ui.compose.theme.VibePrimary

@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onChatClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val mode by viewModel.mode.collectAsState()
    var selectedTab by remember { mutableStateOf(BottomNavTab.CHATS) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        VibeTopBar(
            title = "Vibe",
            onMenuClick = { /* TODO: drawer */ }
        )

        // Mode tabs: Personal / Work
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeTab(
                label = "Personal",
                isSelected = mode == ChatMode.PERSONAL,
                onClick = { viewModel.switchMode(ChatMode.PERSONAL) },
                modifier = Modifier.weight(1f)
            )
            ModeTab(
                label = "Work",
                isSelected = mode == ChatMode.WORK,
                onClick = { viewModel.switchMode(ChatMode.WORK) },
                modifier = Modifier.weight(1f)
            )
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when (val state = uiState) {
                is ChatListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        color = VibePrimary
                    )
                }
                is ChatListUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error",
                            color = TextPrimaryDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = TextSecondaryDark,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.loadChats() }) {
                            Text("Retry", color = VibePrimary)
                        }
                    }
                }
                is ChatListUiState.Success -> {
                    if (state.chats.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No chats yet",
                                color = TextPrimaryDark,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start a conversation by tapping the button below",
                                color = TextTertiaryDark,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(state.chats, key = { it.id }) { chat ->
                                ChatListItem(
                                    data = chat,
                                    onClick = { onChatClick(chat.id) }
                                )
                            }
                        }
                    }
                }
            }

            // FAB
            FloatingActionButton(
                onClick = onNewChatClick,
                containerColor = VibePrimary,
                contentColor = TextPrimaryDark,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New chat"
                )
            }
        }

        // Bottom navigation
        VibeBottomNavigation(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                selectedTab = tab
                // TODO: navigate to tab destination
            }
        )
    }
}

@Composable
private fun ModeTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(36.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) VibePrimary else TextTertiaryDark,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
