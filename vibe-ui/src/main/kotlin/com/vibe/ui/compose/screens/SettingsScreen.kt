package com.vibe.ui.compose.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.vibe.ui.i18n.VibeI18n
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onTheme: () -> Unit = {},
    onLanguage: () -> Unit = {},
    onStorage: () -> Unit = {},
    onCalls: () -> Unit = {},
    onMesh: () -> Unit = {},
    onAbout: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(VibeI18n.t("settings"), fontWeight = FontWeight.Bold) },
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
            Spacer(modifier = Modifier.height(8.dp))

            SettingsGroup(VibeI18n.t("account")) {
                ClickableSettingItem(Icons.Default.Person, VibeI18n.t("profile"), onClick = onEditProfile)
                ClickableSettingItem(Icons.Default.Lock, VibeI18n.t("privacy"), onClick = onPrivacy)
            }

            SettingsGroup(VibeI18n.t("settings")) {
                ClickableSettingItem(Icons.Default.Notifications, VibeI18n.t("notifications"), onClick = onNotifications)
                ClickableSettingItem(Icons.Default.DarkMode, VibeI18n.t("appearance"), onClick = onTheme)
                ClickableSettingItem(Icons.Default.Language, VibeI18n.t("language"), onClick = onLanguage)
                ClickableSettingItem(Icons.Default.Call, VibeI18n.t("calls"), onClick = onCalls)
                ClickableSettingItem(Icons.Default.Wifi, VibeI18n.t("mesh"), onClick = onMesh)
            }

            SettingsGroup(VibeI18n.t("data")) {
                ClickableSettingItem(Icons.Default.Storage, VibeI18n.t("storage"), onClick = onStorage)
            }

            SettingsGroup("AI") {
                ClickableSettingItem(
                    Icons.Default.AutoAwesome,
                    VibeI18n.t("ai_assistant"),
                    subtitle = VibeI18n.t("ai_assistant_desc"),
                    onClick = { showAiDialog = true }
                )
            }

            SettingsGroup(VibeI18n.t("info")) {
                ClickableSettingItem(Icons.Default.Info, VibeI18n.t("about"), onClick = onAbout)
                ClickableSettingItem(Icons.Default.Mail, VibeI18n.t("support"), onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:vibe.messenger@mail.ru")
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Vibe — обращение в поддержку")
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {}
                })
            }

            SettingsGroup(VibeI18n.t("danger_zone")) {
                ClickableSettingItem(
                    icon = Icons.Default.DeleteForever,
                    title = if (deleting) VibeI18n.t("deleting") else VibeI18n.t("delete_account"),
                    onClick = { if (!deleting) showDeleteDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = VibeI18n.t("app_version"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                text = VibeI18n.t("app_subtitle"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(VibeI18n.t("delete_account_confirm_title")) },
                text = {
                    Text(VibeI18n.t("delete_account_confirm_text"))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            deleting = true
                            scope.launch {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val serverConfig = com.vibe.ui.network.ServerConfig(context)
                                    val token = serverConfig.getAuthToken()
                                    val userId = serverConfig.getUserId()

                                    // Delete profile from Supabase
                                    if (token.isNotBlank() && userId.isNotBlank()) {
                                        com.vibe.ui.network.SupabaseClient.deleteProfile(
                                            com.vibe.ui.BuildConfig.SUPABASE_URL,
                                            com.vibe.ui.BuildConfig.SUPABASE_ANON_KEY,
                                            token,
                                            userId
                                        )
                                    }

                                    // Delete local Room DB account
                                    try {
                                        val db = com.vibe.ui.data.db.VibeDatabase.getDatabase(context)
                                        db.accountDao().deleteAll()
                                    } catch (_: Exception) {}

                                    // Clear all preferences
                                    serverConfig.clearAll()
                                    com.vibe.ui.e2e.SignalKeyManager(context).clear()
                                }
                                onLogout()
                            }
                        },
                        enabled = !deleting
                    ) {
                        Text(VibeI18n.t("delete"), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(VibeI18n.t("cancel"))
                    }
                }
            )
        }

        if (showAiDialog) {
            var aiApiKey by remember {
                mutableStateOf(
                    com.vibe.ui.network.ServerConfig(context).getAiApiKey()
                )
            }
            AlertDialog(
                onDismissRequest = { showAiDialog = false },
                title = { Text(VibeI18n.t("ai_assistant")) },
                text = {
                    Column {
                        Text(
                            VibeI18n.t("ai_api_key_hint"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = aiApiKey,
                            onValueChange = { aiApiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val serverConfig = com.vibe.ui.network.ServerConfig(context)
                        serverConfig.setAiApiKey(aiApiKey)
                        com.vibe.ui.ai.AurionManager.updateApiKey(aiApiKey)
                        showAiDialog = false
                    }) {
                        Text(VibeI18n.t("save"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAiDialog = false }) {
                        Text(VibeI18n.t("cancel"))
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp, start = 4.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun ClickableSettingItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
