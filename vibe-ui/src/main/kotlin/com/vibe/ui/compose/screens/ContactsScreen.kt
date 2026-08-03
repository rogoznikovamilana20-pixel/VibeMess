package com.vibe.ui.compose.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vibe.bridge.model.VibeUser
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.ContactEntity
import com.vibe.ui.di.VibeContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhoneContact(
    val id: Long,
    val name: String,
    val phone: String
)

data class ResolvedContact(
    val contact: PhoneContact,
    val vibeUser: VibeUser?
)

fun normalizePhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when {
        digits.startsWith("8") && digits.length == 11 -> "7" + digits.drop(1)
        digits.startsWith("7") && digits.length == 11 -> digits
        digits.length == 10 -> "7" + digits
        else -> digits
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onBack: () -> Unit,
    onCallVibe: (contactUserId: String, contactName: String, isVideo: Boolean) -> Unit = { _, _, _ -> },
    onOpenChat: (userId: Long, userName: String) -> Unit = { _, _ -> },
    title: String = "Контакты"
) {
    val context = LocalContext.current
    val contacts = remember { mutableStateListOf<PhoneContact>() }
    val resolved = remember { mutableStateListOf<ResolvedContact>() }
    val filtered = remember { mutableStateListOf<ResolvedContact>() }
    var searchQuery by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }
    var vibeUsersCount by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) loadContacts(context, contacts)
    }

    LaunchedEffect(Unit) {
        val check = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        if (check == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
            loadContacts(context, contacts)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(contacts.toList()) {
        if (contacts.isNotEmpty()) {
            resolving = true
            resolveContacts(context, contacts, resolved)
            vibeUsersCount = resolved.count { it.vibeUser != null }
            resolving = false
        }
    }

    LaunchedEffect(resolved.toList(), searchQuery) {
        filtered.clear()
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            filtered.addAll(resolved)
        } else {
            filtered.addAll(resolved.filter {
                it.contact.name.lowercase().contains(query) ||
                    it.contact.phone.contains(query) ||
                    (it.vibeUser?.username?.lowercase()?.contains(query) == true)
            })
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
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
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск контактов") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (resolved.isNotEmpty() && !resolving) {
                Text(
                    text = "$vibeUsersCount из ${resolved.size} контактов в Vibe",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (!hasPermission) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ContactPhone, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Нет доступа к контактам",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (contacts.isEmpty() || resolving) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(if (resolving) "Связываем с аккаунтами..." else "Загрузка контактов...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Контакты не найдены",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered, key = { it.contact.id }) { rc ->
                        ContactItem(
                            contact = rc.contact,
                            vibeUser = rc.vibeUser,
                            onOpenChat = {
                                rc.vibeUser?.let { user ->
                                    onOpenChat(user.id, rc.contact.name)
                                }
                            },
                            onVibeCall = {
                                rc.vibeUser?.let { user ->
                                    onCallVibe(user.id.toString(), rc.contact.name, false)
                                }
                            },
                            onVibeVideoCall = {
                                rc.vibeUser?.let { user ->
                                    onCallVibe(user.id.toString(), rc.contact.name, true)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactItem(
    contact: PhoneContact,
    vibeUser: VibeUser?,
    onOpenChat: () -> Unit,
    onVibeCall: () -> Unit,
    onVibeVideoCall: () -> Unit
) {
    val callTint = if (vibeUser != null) Color(0xFF8D2BFA) else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = vibeUser != null, onClick = onOpenChat)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VibeAvatar(name = contact.name, size = 44.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (vibeUser != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF4ADE80))
                    )
                }
            }
            if (contact.phone.isNotEmpty()) {
                Text(
                    text = if (vibeUser != null) contact.phone else "${contact.phone} · не в Vibe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (vibeUser == null) {
                Text(
                    text = "не в Vibe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (vibeUser?.username != null) {
                Text(
                    text = "@${vibeUser.username}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8D2BFA)
                )
            }
        }
        Icon(
            Icons.Filled.Call, if (vibeUser != null) "Звонок" else "Недоступно",
            tint = callTint,
            modifier = Modifier
                .size(22.dp)
                .clickable(enabled = vibeUser != null, onClick = onVibeCall)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Icon(
            Icons.Filled.Videocam, if (vibeUser != null) "Видеозвонок" else "Недоступно",
            tint = callTint,
            modifier = Modifier
                .size(22.dp)
                .clickable(enabled = vibeUser != null, onClick = onVibeVideoCall)
        )
    }
}

private suspend fun resolveContacts(
    context: android.content.Context,
    phoneContacts: List<PhoneContact>,
    out: MutableList<ResolvedContact>
) {
    withContext(Dispatchers.IO) {
        val users = runCatching {
            if (VibeContainer.isInitialized()) {
                VibeContainer.getGateway().contacts.getContacts()
            } else emptyList()
        }.getOrDefault(emptyList())

        val byPhone = users.mapNotNull { user ->
            user.phone?.let { normalizePhone(it) to user }
        }.toMap()

        val resolved = phoneContacts.map { pc ->
            ResolvedContact(pc, byPhone[normalizePhone(pc.phone)])
        }
        val usersToStore = resolved.mapNotNull { it.vibeUser }.distinctBy { it.id }.map { user ->
            ContactEntity(
                id = user.id,
                firstName = user.firstName,
                lastName = user.lastName,
                username = user.username,
                phone = user.phone,
                isBot = user.isBot,
                isPremium = user.isPremium,
                avatarPhotoId = user.avatar?.photoId
            )
        }
        if (usersToStore.isNotEmpty()) {
            runCatching {
                VibeDatabase.getDatabase(context).contactDao().insertContacts(usersToStore)
            }
        }
        out.clear()
        out.addAll(resolved)
    }
}

private fun loadContacts(context: android.content.Context, list: MutableList<PhoneContact>) {
    list.clear()
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null, null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
    )
    cursor?.use {
        val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val seen = mutableSetOf<Long>()
        while (it.moveToNext()) {
            val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
            if (seen.contains(id)) continue
            seen.add(id)
            val phone = if (phoneIdx >= 0) it.getString(phoneIdx) ?: "" else ""
            val rawName = if (nameIdx >= 0) it.getString(nameIdx) else null
            val name = rawName?.takeIf { it.isNotBlank() }
                ?: phone.takeIf { it.isNotBlank() }
                ?: "Без имени"
            list.add(PhoneContact(id, name, phone))
        }
    }
}
