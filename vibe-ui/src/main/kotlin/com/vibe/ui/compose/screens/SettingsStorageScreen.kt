package com.vibe.ui.compose.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.i18n.VibeI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsStorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = VibeDatabase.getDatabase(context)

    var dbSize by remember { mutableLongStateOf(0L) }
    var mediaSize by remember { mutableLongStateOf(0L) }
    var cacheSize by remember { mutableLongStateOf(0L) }
    var totalSize by remember { mutableLongStateOf(0L) }
    var cacheCleared by remember { mutableStateOf(false) }

    fun refreshSizes() {
        scope.launch {
            val (dbSzVal, mediaSzVal, cacheSzVal) = withContext(Dispatchers.IO) {
                val dbFile = context.getDatabasePath("vibe_database")
                val dbSz = dbFile?.length() ?: 0L
                val cacheDir = context.cacheDir
                val cacheSz = cacheDir?.let { dir -> dir.walkTopDown().sumOf { it.length() } } ?: 0L
                val filesDir = context.filesDir
                val mediaSz = filesDir?.let { dir ->
                    dir.walkTopDown().filter { it.extension in listOf("jpg", "png", "gif", "mp4", "mp3", "ogg") }
                        .sumOf { it.length() }
                } ?: 0L
                Triple(dbSz, mediaSz, cacheSz)
            }
            dbSize = dbSzVal
            mediaSize = mediaSzVal
            cacheSize = cacheSzVal
            totalSize = dbSzVal + mediaSzVal + cacheSzVal
        }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes Б"
            bytes < 1024 * 1024 -> "${bytes / 1024} КБ"
            else -> "%.1f МБ".format(bytes.toDouble() / (1024 * 1024))
        }
    }

    refreshSizes()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(VibeI18n.t("storage"), fontWeight = FontWeight.Bold) },
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

            Text(
                VibeI18n.t("memory_usage"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StorageRow(Icons.Default.Storage, VibeI18n.t("database"), formatSize(dbSize))
                    Spacer(modifier = Modifier.height(8.dp))
                    StorageRow(Icons.Default.Image, VibeI18n.t("media_files"), formatSize(mediaSize))
                    Spacer(modifier = Modifier.height(12.dp))
                    val totalMb = if (totalSize > 0) (totalSize.toFloat() / (50 * 1024 * 1024)).coerceAtMost(1f) else 0f
                    LinearProgressIndicator(
                        progress = { totalMb },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF8B5CF6),
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${(totalMb * 100).toInt()}${VibeI18n.t("percent_used")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                VibeI18n.t("cache"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Кэш приложения",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatSize(cacheSize),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (cacheCleared) {
                        Text(
                            "Кэш очищен",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4ADE80)
                        )
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        context.cacheDir?.let { dir ->
                                            dir.listFiles()?.forEach { file ->
                                                try {
                                                    if (file.isDirectory) {
                                                        file.listFiles()?.forEach { child ->
                                                            try { child.delete() } catch (_: Exception) {}
                                                        }
                                                        try { file.delete() } catch (_: Exception) {}
                                                    } else {
                                                        try { file.delete() } catch (_: Exception) {}
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    }
                                    cacheCleared = true
                                    refreshSizes()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(VibeI18n.t("clear_cache"), color = Color(0xFFEF4444))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                VibeI18n.t("data"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Управление данными приложения",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Для полной очистки данных используйте Настройки → Приложения → Vibe → Очистить данные",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageRow(icon: ImageVector, label: String, size: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF8D2BFA))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(size, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
