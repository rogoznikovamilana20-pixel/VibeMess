package com.vibe.ui.compose.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.compose.components.VideoPlayer
import com.vibe.ui.data.AchievementManager
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.ProfileRepository
import com.vibe.ui.data.db.entity.TimelinePostEntity
import com.vibe.ui.i18n.VibeI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val profileRepo = remember { ProfileRepository(context) }
    val db = remember { VibeDatabase.getDatabase(context) }
    val posts by db.timelineDao().getAllPosts().collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var newPostText by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf("text") }
    val scope = rememberCoroutineScope()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            val mimeType = context.contentResolver.getType(uri) ?: ""
            selectedMediaType = when {
                mimeType.startsWith("video/") -> "video"
                mimeType == "image/gif" -> "gif"
                mimeType.startsWith("image/") -> "photo"
                else -> "photo"
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Лента", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedMediaUri = null
                selectedMediaType = "text"
                newPostText = ""
                showDialog = true
            }) {
                Icon(Icons.Default.Add, "Новый пост")
            }
        }
    ) { padding ->
        if (posts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Timeline, null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Пока нет постов",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Нажмите + чтобы создать первый пост",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    TimelinePostCard(post = post, onLike = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.timelineDao().setLiked(
                                    post.id,
                                    !post.isLiked,
                                    if (post.isLiked) -1 else 1
                                )
                            }
                        }
                    })
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                newPostText = ""
                selectedMediaUri = null
                selectedMediaType = "text"
            },
            title = { Text(VibeI18n.t("new_post")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPostText,
                        onValueChange = { newPostText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(VibeI18n.t("post_placeholder")) },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedMediaUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(selectedMediaUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (selectedMediaType == "video") {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = "Видео",
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(48.dp),
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedMediaUri = null
                                    selectedMediaType = "text"
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Убрать",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.Image, "Фото",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.VideoOnly
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.Videocam, "Видео",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPostText.isNotBlank() || selectedMediaUri != null) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val mediaPath = selectedMediaUri?.let { uri ->
                                    copyMediaToStorage(context, uri, selectedMediaType)
                                }
                                db.timelineDao().insertPost(TimelinePostEntity(
                                    content = newPostText.trim(),
                                    authorName = profileRepo.displayName,
                                    timestamp = System.currentTimeMillis(),
                                    imageUri = mediaPath,
                                    mediaType = if (mediaPath != null) selectedMediaType else "text"
                                ))
                                AchievementManager(context).unlock(AchievementManager.Id.FIRST_POST)
                            }
                        }
                    }
                    showDialog = false
                    newPostText = ""
                    selectedMediaUri = null
                    selectedMediaType = "text"
                }) { Text(VibeI18n.t("publish")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    newPostText = ""
                    selectedMediaUri = null
                    selectedMediaType = "text"
                }) {
                    Text(VibeI18n.t("cancel"))
                }
            }
        )
    }
}

private fun copyMediaToStorage(context: android.content.Context, uri: Uri, mediaType: String): String? {
    return try {
        val dir = File(context.filesDir, "timeline_media")
        dir.mkdirs()
        val ext = when (mediaType) {
            "video" -> "mp4"
            "gif" -> "gif"
            else -> "jpg"
        }
        val file = File(dir, "${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        android.util.Log.e("TimelineScreen", "Failed to copy media", e)
        null
    }
}

@Composable
private fun TimelinePostCard(post: TimelinePostEntity, onLike: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d MMM HH:mm", Locale("ru")) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author header
            Row(verticalAlignment = Alignment.CenterVertically) {
                VibeAvatar(name = post.authorName, size = 36.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text(dateFormat.format(Date(post.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Content text
            if (post.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(post.content, style = MaterialTheme.typography.bodyMedium)
            }

            // Media
            if (post.imageUri != null && post.mediaType != "text") {
                Spacer(modifier = Modifier.height(10.dp))
                if (post.mediaType == "video") {
                    VideoPlayer(
                        videoPath = post.imageUri,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(post.imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Like button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = post.id != 0L, onClick = onLike)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Нравится",
                        modifier = Modifier.size(18.dp),
                        tint = if (post.isLiked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${post.likes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (post.isLiked) Color(0xFFEF4444)
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
