package com.vibe.ui.compose.screens

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.qrcode.QRCodeWriter
import com.vibe.bridge.model.VibeUser
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.compose.components.VibeButton
import com.vibe.ui.compose.components.VibeButtonSize
import com.vibe.ui.compose.components.VibeButtonVariant
import com.vibe.ui.data.AchievementManager
import com.vibe.ui.data.ProfileRepository
import com.vibe.ui.di.VibeContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val achievementEmoji = mapOf(
    AchievementManager.Id.FIRST_MESSAGE to "💬",
    AchievementManager.Id.FIRST_AI to "🤖",
    AchievementManager.Id.TEN_MESSAGES to "🗨️",
    AchievementManager.Id.FIRST_CALL to "📞",
    AchievementManager.Id.PROFILE_SET to "👤",
    AchievementManager.Id.FIRST_POST to "📰",
    AchievementManager.Id.FIRST_LISTING to "🛍️"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    isOwnProfile: Boolean = true,
    vibeId: String = "",
    onEditProfile: () -> Unit = {},
    onOpenVibePlus: () -> Unit = {},
    onOpenSparks: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profileRepo = remember { ProfileRepository(context) }
    val achievementManager = remember { AchievementManager(context) }
    var user by remember { mutableStateOf<VibeUser?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showQr by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (VibeContainer.isInitialized()) {
            try {
                val gateway = withContext(Dispatchers.IO) { VibeContainer.getGateway() }
                val account = withContext(Dispatchers.IO) { gateway.accounts.getCurrentAccount() }
                val u = withContext(Dispatchers.IO) { gateway.users.getUser(account.userId) }
                user = u
                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    val displayName = profileRepo.displayName
    val displayUsername = profileRepo.username
    val displayId = if (vibeId.isNotEmpty()) vibeId else profileRepo.vibeId
    val bio = profileRepo.bio
    val unlockedCount = achievementManager.getAll().count { it.second }
    val totalCount = achievementManager.allAchievements.size

    val shareLink = remember(displayUsername, displayId) {
        if (displayUsername.isNotBlank()) {
            "https://t.me/${displayUsername.trim().removePrefix("@")}"
        } else if (displayId.isNotBlank()) {
            "vibe://user/$displayId"
        } else null
    }

    val shareTarget = shareLink
    if (showQr && shareTarget != null) {
        QrShareDialog(
            name = displayName,
            link = shareTarget,
            onDismiss = { showQr = false },
            onShare = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareTarget)
                }
                context.startActivity(Intent.createChooser(send, "Поделиться профилем"))
                showQr = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Профиль", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = onEditProfile) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                        IconButton(onClick = {
                            if (shareLink != null) {
                                showQr = true
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Профиль ещё не готов к публикации",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Icon(Icons.Default.Share, "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Загрузка...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                VibeAvatar(
                    name = displayName,
                    size = 96.dp,
                    photoUrl = profileRepo.avatarPath.takeIf { it.isNotBlank() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = displayUsername,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val sparkState = com.vibe.ui.data.payment.SparkManager.state.collectAsState()

                if (sparkState.value.isVibePlus) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF8D2BFA), Color(0xFFB06BFF)))
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null,
                                    tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "VIBE+",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (sparkState.value.vibePlusExpiresAt > 0) {
                            Text(
                                text = "до ${java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                                    .format(java.util.Date(sparkState.value.vibePlusExpiresAt))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (displayId.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Vibe ID: $displayId",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!isOwnProfile) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        VibeButton(text = "Написать", onClick = {}, size = VibeButtonSize.MEDIUM)
                        VibeButton(text = "Позвонить", onClick = {},
                            variant = VibeButtonVariant.SECONDARY, size = VibeButtonSize.MEDIUM)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                            Icon(Icons.Default.WorkspacePremium, null,
                                tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Репутация",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ReputationBadge("Подтверждён", "✓", Color(0xFF4ADE80))
                            ReputationBadge("Активность", "Высокая", Color(0xFF8D2BFA))
                            ReputationBadge("Доверие", "85%", Color(0xFFF59E0B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSparks() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Искры",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            Text("${sparkState.value.balance} — покупка и вывод",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onOpenVibePlus) {
                            Icon(Icons.Default.AutoAwesome, "Vibe+",
                                tint = Color(0xFF8D2BFA))
                        }
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Badge, null,
                                tint = Color(0xFF8D2BFA), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Достижения",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            achievementManager.allAchievements.forEach { a ->
                                val unlocked = achievementManager.isUnlocked(a.id)
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (unlocked) MaterialTheme.colorScheme.surface
                                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unlocked) achievementEmoji[a.id] ?: "🎖️" else "🔒",
                                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$unlockedCount из $totalCount достижений получено",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ReputationBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QrShareDialog(
    name: String,
    link: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    var qr by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(link) {
        qr = withContext(Dispatchers.Default) {
            runCatching {
                QRCodeWriter().encode(link, 512, 512, null, null).asImageBitmap()
            }.getOrNull()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val bitmap = qr
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "QR",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Отсканируйте QR-код, чтобы открыть профиль",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onShare) { Text("Поделиться") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}
