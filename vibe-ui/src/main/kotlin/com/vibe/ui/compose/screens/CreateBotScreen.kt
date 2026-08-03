package com.vibe.ui.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.vibe.ui.data.bot.BotCommand
import com.vibe.ui.data.bot.BotRepository
import com.vibe.ui.data.bot.BotScriptRule
import com.vibe.ui.i18n.VibeI18n
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBotScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isAi by remember { mutableStateOf(true) }
    var systemPrompt by remember { mutableStateOf("") }
    var commandsText by remember { mutableStateOf("") }
    var scriptText by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    val commands = commandsText.lineSequence()
        .mapNotNull { line ->
            val parts = line.split("—", "-", ":").map { it.trim() }
            if (parts.size >= 2 && parts[0].isNotBlank()) {
                BotCommand(parts[0].removePrefix("/"), parts.drop(1).joinToString(" — "))
            } else null
        }.toList()

    val scripts = scriptText.lineSequence()
        .mapNotNull { line ->
            val idx = line.indexOf('=')
            if (idx > 0) {
                BotScriptRule(
                    keyword = line.substring(0, idx).trim(),
                    response = line.substring(idx + 1).trim()
                )
            } else null
        }.toList()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(VibeI18n.t("create_bot"), fontWeight = FontWeight.Bold) },
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(VibeI18n.t("bot_name")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(VibeI18n.t("bot_username")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(VibeI18n.t("bot_description")) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = VibeI18n.t("bot_mode"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                RadioButton(selected = isAi, onClick = { isAi = true })
                Icon(
                    Icons.Default.AutoAwesome,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(VibeI18n.t("bot_ai_mode"))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !isAi, onClick = { isAi = false })
                Icon(
                    Icons.Default.Code,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(VibeI18n.t("bot_script_mode"))
            }

            if (isAi) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text(VibeI18n.t("bot_prompt")) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = scriptText,
                    onValueChange = { scriptText = it },
                    label = { Text(VibeI18n.t("bot_script_hint")) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = commandsText,
                onValueChange = { commandsText = it },
                label = { Text(VibeI18n.t("bot_commands_hint")) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = {
                    if (creating) return@OutlinedButton
                    creating = true
                    scope.launch {
                        val repo = BotRepository(context)
                        val result = repo.createBot(
                            name = name,
                            username = username,
                            description = description,
                            systemPrompt = systemPrompt,
                            commands = commands,
                            scripts = scripts,
                            isAi = isAi
                        )
                        creating = false
                        result.onSuccess {
                            Toast.makeText(context, VibeI18n.t("bot_created"), Toast.LENGTH_SHORT).show()
                            onCreated()
                        }.onFailure { e ->
                            Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (creating) VibeI18n.t("creating") else VibeI18n.t("create_bot"))
            }
        }
    }
}
