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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibe.common.logging.VibeLogger
import com.vibe.bridge.model.VibeChat
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.compose.components.VibeMode
import com.vibe.ui.compose.components.VibeModeToggle
import com.vibe.ui.di.VibeContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val isMuted: Boolean = false
)

@Composable
fun MainScreen(
    onOpenChat: (chatId: Long, chatName: String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenGroups: () -> Unit = onOpenContacts,
    onOpenCalls: () -> Unit = {},
    onOpenFavorites: () -> Unit = {}
) {
    var selectedMode by remember { mutableStateOf(VibeMode.PERSONAL) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var chats by remember { mutableStateOf<List<ChatPreview>>(emptyList()) }
    var userName by remember { mutableStateOf("Андрей") }
    var userTag by remember { mutableStateOf("@andre") }
    var bridgeError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showNewChat by remember { mutableStateOf(false) }
    var newChatName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val filteredChats = if (searchQuery.isBlank()) chats
        else chats.filter { it.name.contains(searchQuery, ignoreCase = true) }

    val demoChats = remember {
        listOf(
            ChatPreview(1, "Анна Смирнова", "Привет! Как дела?", "12:30", 2, isOnline = true),
            ChatPreview(2, "Рабочий чат", "Дедлайн завтра", "11:45", 0),
            ChatPreview(3, "Максим", "Скинь фото", "Вчера", 5, isPinned = true),
            ChatPreview(4, "Семья", "Ужин в 19:00", "Вчера", 0, isMuted = true),
            ChatPreview(5, "Vibe Community", "Новый релиз 2.0!", "Пн", 12, isOnline = true)
        )
    }

    LaunchedEffect(Unit) {
        if (VibeContainer.isInitialized()) {
            try {
                val gateway = withContext(Dispatchers.IO) {
                    VibeContainer.getGateway()
                }
                val account = withContext(Dispatchers.IO) {
                    gateway.accounts.getCurrentAccount()
                }
                val user = withContext(Dispatchers.IO) {
                    gateway.users.getUser(account.userId)
                }
                user?.let {
                    userName = it.firstName + (it.lastName?.let { " $it" } ?: "")
                    userTag = "@${it.username ?: account.userId.toString()}"
                }

                val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val now = System.currentTimeMillis()

                scope.launch {
                    try {
                        gateway.chats.getActiveChats().collect { chatList ->
                            if (chatList.isNotEmpty()) {
                                chats = chatList.sortedByDescending { it.lastActivityDate }.map { chat ->
                                    val ts = chat.lastActivityDate
                                    val timeStr = when {
                                        ts == 0L -> ""
                                        now - ts < 86400000L -> timeFormat.format(Date(ts))
                                        else -> dateFormat.format(Date(ts))
                                    }
                                    ChatPreview(
                                        id = chat.id,
                                        name = chat.title,
                                        lastMessage = chat.lastMessage?.text ?: chat.draftText ?: "",
                                        time = timeStr,
                                        unreadCount = chat.unreadCount,
                                        isPinned = chat.isPinned,
                                        isMuted = chat.isMuted
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        VibeLogger.e("MainScreen", "getActiveChats failed", e)
                    }
                }
            } catch (e: Exception) {
                VibeLogger.e("MainScreen", "Bridge init/user load failed", e)
            }
        }

        withContext(Dispatchers.Default) { kotlinx.coroutines.delay(3000) }
        if (chats.isEmpty()) {
            chats = demoChats
        }
        isLoading = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                DrawerContent(
                    userName = userName,
                    userTag = userTag,
                    onProfile = onOpenProfile,
                    onSettings = onOpenSettings,
                    onContacts = onOpenContacts,
                    onTimeline = onOpenTimeline,
                    onMarketplace = onOpenMarketplace,
                    onAchievements = onOpenAchievements,
                    onGroups = onOpenGroups,
                    onCalls = onOpenCalls,
                    onFavorites = onOpenFavorites,
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

                        IconButton(onClick = { showSearch = !showSearch; searchQuery = "" }) {
                            Icon(
                                if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                "Search",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    VibeModeToggle(
                        selectedMode = selectedMode,
                        onModeSelected = { selectedMode = it },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск чатов...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showNewChat = true; newChatName = "" },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, "New chat")
                }
            }
        ) { padding ->
            if (filteredChats.isEmpty() && searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ничего не найдено",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (filteredChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        com.vibe.ui.compose.components.LoadingDots(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Загрузка...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(filteredChats, key = { it.id }) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = { onOpenChat(chat.id, chat.name) }
                        )
                    }
                }
            }
        }
    }

    if (showNewChat) {
        AlertDialog(
            onDismissRequest = { showNewChat = false },
            title = { Text("Новый чат") },
            text = {
                OutlinedTextField(
                    value = newChatName,
                    onValueChange = { newChatName = it },
                    placeholder = { Text("Имя контакта или ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newChatName.isNotBlank()) {
                        onOpenChat(-1, newChatName)
                        showNewChat = false
                    }
                }) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showNewChat = false }) { Text("Отмена") }
            }
        )
    }

    if (bridgeError != null) {
        AlertDialog(
            onDismissRequest = { bridgeError = null },
            title = { Text("Ошибка") },
            text = { Text(bridgeError ?: "") },
            confirmButton = {
                TextButton(onClick = { bridgeError = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun ChatListItem(
    chat: ChatPreview,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VibeAvatar(
                name = chat.name,
                size = 48.dp,
                hasStory = chat.isPinned,
                badgeCount = if (chat.unreadCount > 0) chat.unreadCount else null
            )

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
                            text = "🔇",
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
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onContacts: () -> Unit,
    onTimeline: () -> Unit,
    onMarketplace: () -> Unit,
    onAchievements: () -> Unit,
    onGroups: () -> Unit = onContacts,
    onCalls: () -> Unit = {},
    onFavorites: () -> Unit = {},
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
            VibeAvatar(name = userName, size = 56.dp)
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

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.outline)

        DrawerMenuItem(Icons.Default.Star, "Чаты", onClick = onClose)
        DrawerMenuItem(Icons.Default.Person, "Контакты", onClick = onContacts)
        DrawerMenuItem(Icons.Default.Groups, "Группы / Сообщества", onClick = onGroups)
        DrawerMenuItem(Icons.Default.Call, "Звонки", onClick = onCalls)
        DrawerMenuItem(Icons.Default.Timeline, "Vibe Timeline", onClick = onTimeline)
        DrawerMenuItem(Icons.Default.Store, "Marketplace", onClick = onMarketplace)
        DrawerMenuItem(Icons.Default.Badge, "Достижения", onClick = onAchievements)

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.outline)

        DrawerMenuItem(Icons.Default.Star, "Избранное", onClick = onFavorites)
        DrawerMenuItem(Icons.Default.Settings, "Настройки", onClick = onSettings)
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
