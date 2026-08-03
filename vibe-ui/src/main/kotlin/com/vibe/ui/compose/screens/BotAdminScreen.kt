package com.vibe.ui.compose.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibe.ui.data.bot.BotEngine
import com.vibe.ui.data.bot.BotRepository
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.BotEntity
import com.vibe.ui.i18n.VibeI18n
import com.vibe.ui.compose.components.VibeAvatar
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotAdminScreen(
    botId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { VibeDatabase.getDatabase(context) }
    val repository = remember { BotRepository(context) }

    var bot by remember { mutableStateOf<BotEntity?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showDescDialog by remember { mutableStateOf(false) }
    var showPromptDialog by remember { mutableStateOf(false) }
    var showCommandsDialog by remember { mutableStateOf(false) }
    var showScriptDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    var descDraft by remember { mutableStateOf("") }
    var promptDraft by remember { mutableStateOf("") }
    var commandsDraft by remember { mutableStateOf("") }
    var scriptDraft by remember { mutableStateOf("") }

    LaunchedEffect(botId) {
        bot = withContext(Dispatchers.IO) { db.botDao().getById(botId) }
        bot?.let {
            nameDraft = it.name
            descDraft = it.description
            promptDraft = it.systemPrompt
            commandsDraft = BotEngine.parseCommands(it.commandsJson)
                .joinToString("\n") { c -> "${c.command} — ${c.description}" }
            scriptDraft = BotEngine.parseScripts(it.scriptRepliesJson)
                .joinToString("\n") { s -> "${s.keyword}=${s.response}" }
        }
    }

    val current = bot
    if (current == null) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(VibeI18n.t("bot_admin"), fontWeight = FontWeight.Bold) },
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
            Text(
                "…",
                modifier = Modifier.padding(padding).padding(24.dp)
            )
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(VibeI18n.t("bot_admin"), fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VibeAvatar(name = current.avatarInitial.ifBlank { current.name }, size = 64.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = current.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (current.isAi) Icons.Default.AutoAwesome else Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (current.isAi) Color(0xFF8D2BFA) else Color(0xFF0EA5E9)
                        )
                    }
                    Text(
                        text = "@${current.username}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (current.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = current.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = VibeI18n.t("bot_settings"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            AdminRow(icon = Icons.Default.Edit, title = VibeI18n.t("bot_name"), value = current.name) {
                showNameDialog = true
            }
            AdminRow(icon = Icons.Default.Edit, title = VibeI18n.t("bot_description"), value = current.description) {
                showDescDialog = true
            }
            if (current.isAi) {
                AdminRow(
                    icon = Icons.Default.AutoAwesome,
                    title = VibeI18n.t("bot_prompt"),
                    value = current.systemPrompt.ifBlank { "…" }
                ) {
                    showPromptDialog = true
                }
            } else {
                AdminRow(
                    icon = Icons.Default.Code,
                    title = VibeI18n.t("bot_script_mode"),
                    value = scriptDraft.ifBlank { "…" }
                ) {
                    showScriptDialog = true
                }
            }
            AdminRow(icon = Icons.Default.Code, title = VibeI18n.t("bot_commands"), value = commandsDraft.ifBlank { "…" }) {
                showCommandsDialog = true
            }

            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (current.token.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("bot_token", current.token))
                                Toast.makeText(context, VibeI18n.t("token_copied"), Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(VibeI18n.t("bot_token"), fontWeight = FontWeight.Bold)
                        Text(
                            text = current.token.ifBlank { VibeI18n.t("bot_token_local") },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PowerSettingsNew, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(VibeI18n.t("bot_enabled"), fontWeight = FontWeight.Bold)
                        Text(
                            text = VibeI18n.t(if (current.isEnabled) "bot_on" else "bot_off"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = current.isEnabled,
                        onCheckedChange = { enabled ->
                            val updated = current.copy(isEnabled = enabled)
                            bot = updated
                            scope.launch { repository.toggleBot(updated, enabled) }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteConfirm = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = VibeI18n.t("bot_delete"),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(VibeI18n.t("bot_name")) },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    val updated = current.copy(name = nameDraft.trim())
                    bot = updated
                    scope.launch { repository.updateBot(updated) }
                }) { Text(VibeI18n.t("save")) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text(VibeI18n.t("cancel")) }
            }
        )
    }

    if (showDescDialog) {
        AlertDialog(
            onDismissRequest = { showDescDialog = false },
            title = { Text(VibeI18n.t("bot_description")) },
            text = {
                OutlinedTextField(
                    value = descDraft,
                    onValueChange = { descDraft = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDescDialog = false
                    val updated = current.copy(description = descDraft.trim())
                    bot = updated
                    scope.launch { repository.updateBot(updated) }
                }) { Text(VibeI18n.t("save")) }
            },
            dismissButton = {
                TextButton(onClick = { showDescDialog = false }) { Text(VibeI18n.t("cancel")) }
            }
        )
    }

    if (showPromptDialog) {
        AlertDialog(
            onDismissRequest = { showPromptDialog = false },
            title = { Text(VibeI18n.t("bot_prompt")) },
            text = {
                OutlinedTextField(
                    value = promptDraft,
                    onValueChange = { promptDraft = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPromptDialog = false
                    val updated = current.copy(systemPrompt = promptDraft)
                    bot = updated
                    scope.launch { repository.updateBot(updated) }
                }) { Text(VibeI18n.t("save")) }
            },
            dismissButton = {
                TextButton(onClick = { showPromptDialog = false }) { Text(VibeI18n.t("cancel")) }
            }
        )
    }

    if (showCommandsDialog) {
        AlertDialog(
            onDismissRequest = { showCommandsDialog = false },
            title = { Text(VibeI18n.t("bot_commands")) },
            text = {
                OutlinedTextField(
                    value = commandsDraft,
                    onValueChange = { commandsDraft = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCommandsDialog = false
                    val commands = commandsDraft.lineSequence().mapNotNull { line ->
                        val parts = line.split("—", "-", ":").map { it.trim() }
                        if (parts.size >= 2 && parts[0].isNotBlank()) {
                            com.vibe.ui.data.bot.BotCommand(
                                parts[0].removePrefix("/"),
                                parts.drop(1).joinToString(" — ")
                            )
                        } else null
                    }.toList()
                    val updated = current.copy(commandsJson = BotEngine.commandsToJson(commands))
                    bot = updated
                    scope.launch { repository.updateBot(updated) }
                }) { Text(VibeI18n.t("save")) }
            },
            dismissButton = {
                TextButton(onClick = { showCommandsDialog = false }) { Text(VibeI18n.t("cancel")) }
            }
        )
    }

    if (showScriptDialog) {
        AlertDialog(
            onDismissRequest = { showScriptDialog = false },
            title = { Text(VibeI18n.t("bot_script_mode")) },
            text = {
                OutlinedTextField(
                    value = scriptDraft,
                    onValueChange = { scriptDraft = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showScriptDialog = false
                    val scripts = scriptDraft.lineSequence().mapNotNull { line ->
                        val idx = line.indexOf('=')
                        if (idx > 0) {
                            com.vibe.ui.data.bot.BotScriptRule(
                                line.substring(0, idx).trim(),
                                line.substring(idx + 1).trim()
                            )
                        } else null
                    }.toList()
                    val updated = current.copy(scriptRepliesJson = BotEngine.scriptsToJson(scripts))
                    bot = updated
                    scope.launch { repository.updateBot(updated) }
                }) { Text(VibeI18n.t("save")) }
            },
            dismissButton = {
                TextButton(onClick = { showScriptDialog = false }) { Text(VibeI18n.t("cancel")) }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(VibeI18n.t("bot_delete")) },
            text = { Text(VibeI18n.t("bot_delete_confirm")) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        repository.deleteBot(current)
                        onDeleted()
                    }
                }) {
                    Text(
                        VibeI18n.t("delete"),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(VibeI18n.t("cancel")) }
            }
        )
    }
}

@Composable
private fun AdminRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
