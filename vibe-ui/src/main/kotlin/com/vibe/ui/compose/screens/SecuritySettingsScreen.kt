package com.vibe.ui.compose.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibe.ui.e2e.AISecuritySummary
import com.vibe.ui.e2e.E2EEngine
import com.vibe.ui.e2e.ThreatLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    userId: String,
    contactId: String?,
    onBack: () -> Unit
) {
    var securitySummary by remember { mutableStateOf<AISecuritySummary?>(null) }

    LaunchedEffect(contactId) {
        if (contactId != null) {
            securitySummary = E2EEngine.getAISecuritySummary(userId, contactId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Безопасность") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // E2EE Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "End-to-End шифрование",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ваши сообщения зашифрованы алгоритмом AES-256-GCM с Double Ratchet. Даже мы не можем их прочитать.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Threat Level Card
            securitySummary?.let { summary ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = getThreatColor(summary.threatLevel).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                getThreatIcon(summary.threatLevel),
                                contentDescription = null,
                                tint = getThreatColor(summary.threatLevel),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Уровень угрозы",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    getThreatText(summary.threatLevel),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = getThreatColor(summary.threatLevel)
                                )
                            }
                        }

                        if (summary.threatDetails.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            summary.threatDetails.forEach { threat ->
                                Text(
                                    "• ${threat.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Biometric Profile Card
            securitySummary?.let { summary ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Биометрический профиль",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (summary.hasBiometricProfile) {
                                "Профиль создан. Уверенность: ${(summary.biometricConfidence * 100).toInt()}%"
                            } else {
                                "Профиль формируется. Нужно больше данных."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "AI анализирует ваш стиль печати для обнаружения подозрительной активности.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Key Rotation Card
            securitySummary?.let { summary ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Ротация ключей",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (summary.shouldRotateKey) {
                                "Рекомендуется ротация: ${summary.rotationUrgency}"
                            } else {
                                "Ключи актуальны"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "AI определяет оптимальный момент для ротации ключей на основе угроз и условий.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Recommendation Card
            securitySummary?.let { summary ->
                if (summary.recommendation != "Всё в порядке") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Рекомендация",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                summary.recommendation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getThreatColor(level: ThreatLevel): Color {
    return when (level) {
        ThreatLevel.NONE -> Color(0xFF4CAF50)
        ThreatLevel.LOW -> Color(0xFF8BC34A)
        ThreatLevel.MEDIUM -> Color(0xFFFFC107)
        ThreatLevel.HIGH -> Color(0xFFFF9800)
        ThreatLevel.CRITICAL -> Color(0xFFF44336)
    }
}

@Composable
private fun getThreatIcon(level: ThreatLevel): ImageVector {
    return when (level) {
        ThreatLevel.NONE, ThreatLevel.LOW -> Icons.Default.CheckCircle
        ThreatLevel.MEDIUM -> Icons.Default.Warning
        ThreatLevel.HIGH, ThreatLevel.CRITICAL -> Icons.Default.Error
    }
}

@Composable
private fun getThreatText(level: ThreatLevel): String {
    return when (level) {
        ThreatLevel.NONE -> "Без угроз"
        ThreatLevel.LOW -> "Низкий уровень"
        ThreatLevel.MEDIUM -> "Средний уровень"
        ThreatLevel.HIGH -> "Высокий уровень"
        ThreatLevel.CRITICAL -> "Критический уровень"
    }
}
