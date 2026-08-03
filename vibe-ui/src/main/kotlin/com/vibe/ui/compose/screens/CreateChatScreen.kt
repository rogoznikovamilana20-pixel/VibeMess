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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibe.bridge.model.VibeUser
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.di.VibeContainer
import com.vibe.ui.i18n.VibeI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.ChatObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig

private enum class ChatTypeChoice(val coreType: Int) {
    CHANNEL(ChatObject.CHAT_TYPE_CHANNEL),
    SUPERGROUP(ChatObject.CHAT_TYPE_MEGAGROUP),
    GROUP(ChatObject.CHAT_TYPE_CHAT)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(ChatTypeChoice.CHANNEL) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<VibeUser>>(emptyList()) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var result by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        contacts = withContext(Dispatchers.IO) {
            runCatching { VibeContainer.getGateway().contacts.getContacts() }.getOrDefault(emptyList())
        }
    }

    val notificationCenter = NotificationCenter.getInstance(UserConfig.selectedAccount)
    val delegate = remember {
        object : NotificationCenter.NotificationCenterDelegate {
            override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
                if (id == NotificationCenter.chatDidCreated) {
                    creating = false
                    result = "ok"
                } else if (id == NotificationCenter.chatDidFailCreate) {
                    creating = false
                    result = "fail"
                }
            }
        }
    }
    DisposableEffect(Unit) {
        notificationCenter.addObserver(delegate, NotificationCenter.chatDidCreated)
        notificationCenter.addObserver(delegate, NotificationCenter.chatDidFailCreate)
        onDispose {
            notificationCenter.removeObserver(delegate, NotificationCenter.chatDidCreated)
            notificationCenter.removeObserver(delegate, NotificationCenter.chatDidFailCreate)
        }
    }

    LaunchedEffect(result) {
        when (result) {
            "ok" -> {
                Toast.makeText(context, VibeI18n.t("created_ok"), Toast.LENGTH_SHORT).show()
                onBack()
            }
            "fail" -> {
                Toast.makeText(context, VibeI18n.t("created_fail"), Toast.LENGTH_SHORT).show()
                result = null
            }
        }
    }

    val createEnabled = title.isNotBlank() &&
        (type != ChatTypeChoice.GROUP || selectedIds.isNotEmpty()) &&
        !creating

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (type == ChatTypeChoice.CHANNEL) VibeI18n.t("create_channel") else VibeI18n.t("create_group"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        enabled = createEnabled,
                        onClick = {
                            creating = true
                            scope.launch {
                                MessagesController.getInstance(UserConfig.selectedAccount)
                                    .createChat(
                                        title.trim(),
                                        java.util.ArrayList(selectedIds),
                                        if (type == ChatTypeChoice.GROUP) "" else description.trim(),
                                        type.coreType,
                                        false, null, null, -1, null
                                    )
                            }
                        }
                    ) {
                        Text(if (creating) VibeI18n.t("creating") else VibeI18n.t("create"))
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
            Spacer(modifier = Modifier.height(12.dp))

            TypeCard(
                icon = Icons.Default.Campaign,
                title = VibeI18n.t("channel_type"),
                subtitle = VibeI18n.t("channel_type_desc"),
                selected = type == ChatTypeChoice.CHANNEL,
                onClick = { type = ChatTypeChoice.CHANNEL }
            )
            TypeCard(
                icon = Icons.Default.Groups,
                title = VibeI18n.t("supergroup_type"),
                subtitle = VibeI18n.t("supergroup_type_desc"),
                selected = type == ChatTypeChoice.SUPERGROUP,
                onClick = { type = ChatTypeChoice.SUPERGROUP }
            )
            TypeCard(
                icon = Icons.Default.Person,
                title = VibeI18n.t("group_type"),
                subtitle = VibeI18n.t("group_type_desc"),
                selected = type == ChatTypeChoice.GROUP,
                onClick = { type = ChatTypeChoice.GROUP }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(VibeI18n.t("chat_name")) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (type != ChatTypeChoice.GROUP) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(VibeI18n.t("chat_description")) },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (contacts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    VibeI18n.t("select_members"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                contacts.forEach { contact ->
                    val isSelected = selectedIds.contains(contact.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) selectedIds.remove(contact.id)
                                else selectedIds.add(contact.id)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VibeAvatar(name = contact.firstName + " " + (contact.lastName ?: ""), size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.firstName + " " + (contact.lastName ?: ""),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!contact.username.isNullOrBlank()) {
                                Text(
                                    text = "@" + contact.username,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TypeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF8B5CF6).copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6)
                )
            }
        }
    }
}
