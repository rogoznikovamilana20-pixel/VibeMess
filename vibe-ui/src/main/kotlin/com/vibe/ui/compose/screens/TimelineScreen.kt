package com.vibe.ui.compose.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Timeline
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.data.AchievementManager
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.ProfileRepository
import com.vibe.ui.data.db.entity.TimelinePostEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val scope = rememberCoroutineScope()

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
            FloatingActionButton(onClick = { showDialog = true }) {
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
            onDismissRequest = { showDialog = false; newPostText = "" },
            title = { Text("Новый пост") },
            text = {
                OutlinedTextField(
                    value = newPostText,
                    onValueChange = { newPostText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("О чём думаете?") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPostText.isNotBlank()) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.timelineDao().insertPost(TimelinePostEntity(
                                    content = newPostText.trim(),
                                    authorName = profileRepo.displayName,
                                    timestamp = System.currentTimeMillis()
                                ))
                                AchievementManager(context).unlock(AchievementManager.Id.FIRST_POST)
                            }
                        }
                    }
                    showDialog = false
                    newPostText = ""
                }) { Text("Опубликовать") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; newPostText = "" }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun TimelinePostCard(post: TimelinePostEntity, onLike: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d MMM HH:mm", Locale("ru")) }
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
            Spacer(modifier = Modifier.height(10.dp))
            Text(post.content,
                style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(10.dp))
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
