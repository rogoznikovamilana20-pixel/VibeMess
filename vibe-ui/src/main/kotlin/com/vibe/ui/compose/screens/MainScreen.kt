package com.vibe.ui.compose.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibe.ui.compose.components.LoadingDots
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.compose.components.VibeMode
import com.vibe.ui.compose.components.VibeModeToggle
import com.vibe.ui.data.ProfileRepository
import com.vibe.ui.feature.chatlist.ChatSectionsUiState
import com.vibe.ui.feature.chatlist.ChatSectionsViewModel
import com.vibe.ui.i18n.VibeI18n
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatPreview(
    val id: Long,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int,
    val isOnline: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val avatarUrl: String? = null,
    val sectionColor: Color = Color(0xFF8D2BFA)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenChat: (chatId: Long, chatName: String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenGroups: () -> Unit = {},
    onOpenCalls: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenBots: () -> Unit = {},
    onSwitchAccount: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenCreateChat: () -> Unit = {}
) {
    val viewModel: ChatSectionsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val showHint by viewModel.showHint.collectAsState()
    val classification by viewModel.classification.collectAsState()

    val context = LocalContext.current
    val profileRepo = remember { ProfileRepository(context) }

    var selectedMode by remember { mutableStateOf(VibeMode.PERSONAL) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var sectionSheetChat by remember { mutableStateOf<ChatPreview?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val now = remember { System.currentTimeMillis() }

    val chats = when (val s = state) {
        is ChatSectionsUiState.Success -> {
            s.chats.map { chat ->
                val ts = chat.lastMessageTime ?: 0L
                val timeStr = when {
                    ts == 0L -> ""
                    now - ts < 86400000L -> timeFormat.format(Date(ts))
                    else -> dateFormat.format(Date(ts))
                }
                ChatPreview(
                    id = chat.id,
                    name = chat.title,
                    lastMessage = chat.lastMessageText ?: chat.draftText ?: "",
                    time = timeStr,
                    unreadCount = chat.unreadCount,
                    isPinned = chat.isPinned,
                    isMuted = chat.isMuted,
                    avatarUrl = chat.avatarPath,
                    sectionColor = if (chat.isPersonal) Color(0xFF8D2BFA) else Color(0xFF10B6FA)
                )
            }
        }
        else -> emptyList()
    }

    val userName = when (val s = state) {
        is ChatSectionsUiState.Success -> s.userName
        else -> ""
    }

    val userTag = when (val s = state) {
        is ChatSectionsUiState.Success -> s.userTag
        else -> ""
    }

    val isLoading = state is ChatSectionsUiState.Loading
    val isSuccess = state is ChatSectionsUiState.Success

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                DrawerContent(
                    userName = userName,
                    userTag = userTag,
                    photoUrl = profileRepo.avatarPath.takeIf { it.isNotBlank() },
                    onProfile = onOpenProfile,
                    onSettings = onOpenSettings,
                    onContacts = onOpenContacts,
                    onTimeline = onOpenTimeline,
                    onMarketplace = onOpenMarketplace,
                    onAchievements = onOpenAchievements,
                    onGroups = onOpenGroups,
                    onCalls = onOpenCalls,
                    onFavorites = onOpenFavorites,
                    onBots = onOpenBots,
                    onSwitchAccount = onSwitchAccount,
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu",
                                tint = MaterialTheme.colorScheme.onBackground)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vibe",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Default.Search, "Search",
                                tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    VibeModeToggle(
                        selectedMode = selectedMode,
                        onModeSelected = {
                            selectedMode = it
                            viewModel.selectMode(it == VibeMode.PERSONAL)
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onOpenCreateChat,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, "New chat")
                }
            }
        ) { padding ->
            when {
                state is ChatSectionsUiState.Error && !isSuccess -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Ошибка загрузки",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.load() }) {
                                Text("Повторить")
                            }
                        }
                    }
                }
                isLoading && chats.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LoadingDots(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Загрузка...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                chats.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет чатов",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        if (showHint) {
                            item(key = "section_hint") {
                                SectionHintBanner(
                                    onClassify = { viewModel.classifyChats() },
                                    onDismiss = { viewModel.dismissHint() }
                                )
                            }
                        }
                        items(chats, key = { it.id }) { chat ->
                            ChatListItem(
                                chat = chat,
                                onClick = { onOpenChat(chat.id, chat.name) },
                                onLongClick = { sectionSheetChat = chat }
                            )
                        }
                    }
                }
            }
        }
    }

    val sheetChat = sectionSheetChat
    if (sheetChat != null) {
        ModalBottomSheet(
            onDismissRequest = { sectionSheetChat = null },
            sheetState = sheetState
        ) {
            Text(
                text = sheetChat.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Перенести в раздел:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = {
                    viewModel.overrideChat(sheetChat.id, isPersonal = true)
                    sectionSheetChat = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Личное",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF8D2BFA),
                    modifier = Modifier.padding(16.dp)
                )
            }
            Surface(
                onClick = {
                    viewModel.overrideChat(sheetChat.id, isPersonal = false)
                    sectionSheetChat = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Работа",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF10B6FA),
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val preview = classification
    if (preview != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClassification() },
            title = { Text("Разложить чаты на разделы?") },
            text = {
                Text(
                    "Aurion определил разделы для ${preview.changes.size} чатов: " +
                        "${preview.personalCount} личных и ${preview.workCount} рабочих. " +
                        "Остальные оставим как есть."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.applyClassification() }) {
                    Text("Применить")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClassification() }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatListItem(
    chat: ChatPreview,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                VibeAvatar(
                    name = chat.name,
                    size = 48.dp,
                    photoUrl = chat.avatarUrl,
                    hasStory = chat.isPinned,
                    badgeCount = if (chat.unreadCount > 0) chat.unreadCount else null
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .background(color = chat.sectionColor, shape = RoundedCornerShape(5.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = chat.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.isMuted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "\uD83D\uDD07",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    userName: String,
    userTag: String,
    photoUrl: String? = null,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onContacts: () -> Unit,
    onTimeline: () -> Unit,
    onMarketplace: () -> Unit,
    onAchievements: () -> Unit,
    onGroups: () -> Unit = onContacts,
    onCalls: () -> Unit = {},
    onFavorites: () -> Unit = {},
    onBots: () -> Unit = {},
    onSwitchAccount: () -> Unit = {},
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxSize()
            .padding(top = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onProfile)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VibeAvatar(name = userName, size = 56.dp, photoUrl = photoUrl)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = userTag,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DrawerMenuItem(Icons.Default.Person, "Сменить аккаунт", onClick = onSwitchAccount)

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.outline)

        DrawerMenuItem(Icons.Default.Star, VibeI18n.t("chats"), onClick = onClose)
        DrawerMenuItem(Icons.Default.Person, VibeI18n.t("contacts"), onClick = onContacts)
        DrawerMenuItem(Icons.Default.Groups, VibeI18n.t("groups"), onClick = onGroups)
        DrawerMenuItem(Icons.Default.SmartToy, VibeI18n.t("bots"), onClick = onBots)
        DrawerMenuItem(Icons.Default.Call, VibeI18n.t("calls"), onClick = onCalls)
        DrawerMenuItem(Icons.Default.Timeline, VibeI18n.t("feed"), onClick = onTimeline)
        DrawerMenuItem(Icons.Default.Store, VibeI18n.t("marketplace"), onClick = onMarketplace)
        DrawerMenuItem(Icons.Default.Badge, VibeI18n.t("achievements"), onClick = onAchievements)

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.outline)

        DrawerMenuItem(Icons.Default.Star, VibeI18n.t("favorites"), onClick = onFavorites)
        DrawerMenuItem(Icons.Default.Settings, VibeI18n.t("settings"), onClick = onSettings)
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionHintBanner(
    onClassify: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Разложить чаты на разделы?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Aurion разделит их на «Личные» и «Рабочие» — вы сможете переопределить любой чат.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Не сейчас") }
                TextButton(onClick = onClassify) { Text("Разложить") }
            }
        }
    }
}
