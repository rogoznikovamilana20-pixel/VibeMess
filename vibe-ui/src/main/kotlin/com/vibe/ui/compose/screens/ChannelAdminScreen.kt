package com.vibe.ui.compose.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.i18n.VibeI18n
import org.telegram.messenger.ChatObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelAdminScreen(
    chatId: Long,
    onBack: () -> Unit,
    onOpenChat: (chatId: Long, chatName: String) -> Unit
) {
    val context = LocalContext.current
    val controller = remember { MessagesController.getInstance(UserConfig.selectedAccount) }
    val coreChatId = -chatId
    var refreshKey by remember { mutableIntStateOf(0) }
    var titleDraft by remember { mutableStateOf("") }
    var descriptionDraft by remember { mutableStateOf("") }
    var usernameDraft by remember { mutableStateOf("") }
    var showTitleDialog by remember { mutableStateOf(false) }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val chat = remember(chatId, refreshKey) { controller.getChat(coreChatId) }
    val chatFull = remember(chatId, refreshKey) { controller.getChatFull(coreChatId) }

    LaunchedEffect(chatId) {
        controller.loadFullChat(coreChatId, 0, true)
    }

    val isChannel = chat != null && ChatObject.isChannel(chat)
    val isMegagroup = chat != null && chat.megagroup
    val isCreator = chat != null && chat.creator
    val count = chat?.participants_count ?: 0
    val countLabel = when {
        isChannel && !isMegagroup -> "$count ${VibeI18n.t("subscribers")}"
        isChannel -> "$count ${VibeI18n.t("members")}"
        else -> "$count ${VibeI18n.t("members")}"
    }
    val typeLabel = when {
        isChannel && !isMegagroup -> VibeI18n.t("channel_type")
        isChannel -> VibeI18n.t("supergroup_type")
        else -> VibeI18n.t("group_type")
    }
    val username = chat?.username?.takeIf { it.isNotBlank() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(VibeI18n.t("admin"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VibeAvatar(name = chat?.title ?: "?", size = 64.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chat?.title ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = typeLabel + if (count > 0) " · $countLabel" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (chatFull?.about?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = chatFull.about,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                AdminRow(Icons.Default.ModeEdit, VibeI18n.t("edit_title")) {
                    titleDraft = chat?.title ?: ""
                    showTitleDialog = true
                }
                if (isChannel) {
                    DividerItem()
                    AdminRow(Icons.Default.Description, VibeI18n.t("edit_description")) {
                        descriptionDraft = chatFull?.about ?: ""
                        showDescriptionDialog = true
                    }
                }
                DividerItem()
                AdminRow(
                    if (username == null) Icons.Default.Public else Icons.Default.Link,
                    if (username == null) VibeI18n.t("make_public") else "@$username"
                ) {
                    usernameDraft = username ?: ""
                    showUsernameDialog = true
                }
                DividerItem()
                AdminRow(Icons.Default.Chat, VibeI18n.t("open_chat")) {
                    onOpenChat(chatId, chat?.title ?: "—")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                AdminRow(Icons.Default.ExitToApp, VibeI18n.t("leave_chat"), danger = true) {
                    showLeaveConfirm = true
                }
                if (isCreator) {
                    DividerItem()
                    AdminRow(Icons.Default.Delete, VibeI18n.t("delete_chat"), danger = true) {
                        showDeleteConfirm = true
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showTitleDialog) {
        EditTextDialog(
            title = VibeI18n.t("edit_title"),
            value = titleDraft,
            onValueChange = { titleDraft = it },
            onDismiss = { showTitleDialog = false },
            onConfirm = {
                showTitleDialog = false
                if (titleDraft.isNotBlank()) {
                    controller.changeChatTitle(coreChatId, titleDraft.trim())
                    refreshKey++
                }
            }
        )
    }

    if (showDescriptionDialog) {
        EditTextDialog(
            title = VibeI18n.t("edit_description"),
            value = descriptionDraft,
            onValueChange = { descriptionDraft = it },
            onDismiss = { showDescriptionDialog = false },
            onConfirm = {
                showDescriptionDialog = false
                controller.updateChatAbout(coreChatId, descriptionDraft.trim(), null)
                refreshKey++
            }
        )
    }

    if (showUsernameDialog) {
        EditTextDialog(
            title = VibeI18n.t("make_public"),
            value = usernameDraft,
            onValueChange = { usernameDraft = it.replace(" ", "").replace("@", "") },
            onDismiss = { showUsernameDialog = false },
            onConfirm = {
                showUsernameDialog = false
                if (usernameDraft.isNotBlank()) {
                    controller.updateChannelUserName(
                        null,
                        coreChatId,
                        usernameDraft.trim(),
                        {
                            Toast.makeText(context, VibeI18n.t("created_ok"), Toast.LENGTH_SHORT).show()
                            refreshKey++
                        },
                        {
                            Toast.makeText(context, VibeI18n.t("created_fail"), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )
    }

    if (showLeaveConfirm) {
        ConfirmDialog(
            text = VibeI18n.t("leave_chat_confirm"),
            onDismiss = { showLeaveConfirm = false },
            onConfirm = {
                showLeaveConfirm = false
                leaveOrDeleteChat(controller, coreChatId, forceDelete = false)
                onBack()
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            text = VibeI18n.t("delete_chat_confirm"),
            danger = true,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                leaveOrDeleteChat(controller, coreChatId, forceDelete = true)
                onBack()
            }
        )
    }
}

private fun leaveOrDeleteChat(controller: MessagesController, coreChatId: Long, forceDelete: Boolean) {
    val self = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser()
    controller.deleteParticipantFromChat(coreChatId, self, null, forceDelete, false)
}

@Composable
private fun DividerItem() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

@Composable
private fun AdminRow(
    icon: ImageVector,
    title: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (danger) MaterialTheme.colorScheme.error else Color(0xFF8D2BFA),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditTextDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(VibeI18n.t("save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(VibeI18n.t("cancel")) }
        }
    )
}

@Composable
private fun ConfirmDialog(
    text: String,
    danger: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (danger) VibeI18n.t("delete_chat") else VibeI18n.t("leave_chat")) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    if (danger) VibeI18n.t("delete") else VibeI18n.t("exit"),
                    color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(VibeI18n.t("cancel")) }
        }
    )
}
