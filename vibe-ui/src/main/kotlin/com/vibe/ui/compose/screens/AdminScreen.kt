package com.vibe.ui.compose.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Unpublished
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibe.ui.BuildConfig
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.network.AdminLog
import com.vibe.ui.network.AdminStats
import com.vibe.ui.network.ServerConfig
import com.vibe.ui.i18n.VibeI18n
import com.vibe.ui.network.SupabaseClient
import kotlinx.coroutines.launch

data class AdminUser(
    val id: String,
    val displayName: String,
    val role: String,
    val isOnline: Boolean,
    val isBanned: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val serverConfig = remember { ServerConfig(context) }
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var stats by remember { mutableStateOf(AdminStats()) }
    var logs by remember { mutableStateOf<List<AdminLog>>(emptyList()) }
    var myRole by remember { mutableStateOf("user") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showRoleDialog by remember { mutableStateOf<AdminUser?>(null) }
    var showBanDialog by remember { mutableStateOf<AdminUser?>(null) }
    var banReason by remember { mutableStateOf("") }

    val isSuperAdmin = myRole == "super_admin"
    val isAdmin = myRole == "admin" || myRole == "super_admin"

    fun loadData() {
        scope.launch {
            var token = serverConfig.getAuthToken()
            val userId = serverConfig.getUserId()
            
            // Try to refresh token if blank
            if (token.isBlank()) {
                serverConfig.refreshSessionIfNeeded()
                token = serverConfig.getAuthToken()
            }
            
            if (token.isBlank()) {
                error = VibeI18n.t("admin_auth_required")
                loading = false
                return@launch
            }

            val myProfile = SupabaseClient.getProfile(
                BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token, userId
            )
            myRole = myProfile?.role ?: "user"

            if (!isAdmin) {
                error = VibeI18n.t("admin_no_rights")
                loading = false
                return@launch
            }

            val allProfiles = SupabaseClient.getAllProfiles(
                BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token
            )
            users = allProfiles.map { p ->
                AdminUser(
                    id = p.id,
                    displayName = p.displayName,
                    role = p.role,
                    isOnline = p.isOnline,
                    isBanned = p.isBanned
                )
            }

            stats = SupabaseClient.getStats(
                BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token
            )

            logs = SupabaseClient.getAdminLogs(
                BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token
            )

            loading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(VibeI18n.t("admin_panel"), fontWeight = FontWeight.Bold) },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(VibeI18n.t("loading"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Security, null, modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(error ?: "", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // Tabs
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                            text = { Text(VibeI18n.t("admin_users")) },
                            icon = { Icon(Icons.Default.Group, null, modifier = Modifier.size(18.dp)) })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                            text = { Text(VibeI18n.t("admin_stats")) },
                            icon = { Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(18.dp)) })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                            text = { Text(VibeI18n.t("admin_journal")) },
                            icon = { Icon(Icons.Default.Security, null, modifier = Modifier.size(18.dp)) })
                    }

                    when (selectedTab) {
                        0 -> UsersTab(users, searchQuery, { searchQuery = it }, isSuperAdmin, isAdmin,
                            onPromote = { user -> showRoleDialog = user },
                            onBan = { user -> showBanDialog = user },
                            onUnban = { user ->
                                scope.launch {
                                    val token = serverConfig.getAuthToken()
                                    SupabaseClient.unbanUser(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token, user.id)
                                    SupabaseClient.logAdminAction(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token,
                                        "unban", user.id, user.displayName)
                                    loadData()
                                }
                            }
                        )
                        1 -> StatsTab(stats)
                        2 -> LogsTab(logs)
                    }
                }
            }
        }
    }

    // Promote/Demote dialog
    showRoleDialog?.let { user ->
        val newRole = if (user.role == "admin") "user" else "admin"
        val actionText = if (user.role == "admin") VibeI18n.t("admin_remove_admin") else VibeI18n.t("admin_make_admin")
        AlertDialog(
            onDismissRequest = { showRoleDialog = null },
            title = { Text(actionText) },
            text = { Text("$actionText ${VibeI18n.t("admin_user_of")} ${user.displayName}?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val token = serverConfig.getAuthToken()
                        SupabaseClient.updateUserRole(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token, user.id, newRole)
                        SupabaseClient.logAdminAction(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token,
                            if (newRole == "admin") "promote" else "demote", user.id, user.displayName)
                        loadData()
                    }
                    showRoleDialog = null
                }) { Text(actionText) }
            },
            dismissButton = { TextButton(onClick = { showRoleDialog = null }) { Text(VibeI18n.t("cancel")) } }
        )
    }

    // Ban dialog
    showBanDialog?.let { user ->
        AlertDialog(
            onDismissRequest = { showBanDialog = null; banReason = "" },
            title = { Text(if (user.isBanned) "${VibeI18n.t("admin_unban")}?" else "${VibeI18n.t("admin_ban")}?") },
            text = {
                if (user.isBanned) {
                    Text("${VibeI18n.t("admin_unban_user")} ${user.displayName}?")
                } else {
                    Column {
                        Text("${VibeI18n.t("admin_ban_user")} ${user.displayName}?")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = banReason,
                            onValueChange = { banReason = it },
                            label = { Text(VibeI18n.t("admin_reason_optional")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val token = serverConfig.getAuthToken()
                        if (user.isBanned) {
                            SupabaseClient.unbanUser(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token, user.id)
                            SupabaseClient.logAdminAction(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token,
                                "unban", user.id, user.displayName)
                        } else {
                            SupabaseClient.banUser(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token, user.id, banReason)
                            SupabaseClient.logAdminAction(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, token,
                                "ban", user.id, user.displayName)
                        }
                        loadData()
                    }
                    showBanDialog = null
                    banReason = ""
                }) { Text(if (user.isBanned) VibeI18n.t("admin_unban") else VibeI18n.t("admin_ban")) }
            },
            dismissButton = { TextButton(onClick = { showBanDialog = null; banReason = "" }) { Text(VibeI18n.t("cancel")) } }
        )
    }
}

@Composable
private fun UsersTab(
    users: List<AdminUser>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isSuperAdmin: Boolean,
    isAdmin: Boolean,
    onPromote: (AdminUser) -> Unit,
    onBan: (AdminUser) -> Unit,
    onUnban: (AdminUser) -> Unit
) {
    val filtered = users.filter {
        searchQuery.isBlank() || it.displayName.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text(VibeI18n.t("admin_search")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = {},
                label = { Text("${VibeI18n.t("all")} (${users.size})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
            FilterChip(selected = false, onClick = {},
                label = { Text("${VibeI18n.t("admin_online")} (${users.count { it.isOnline }})") })
            FilterChip(selected = false, onClick = {},
                label = { Text("${VibeI18n.t("admin_admins")} (${users.count { it.role != "user" }})") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (user.isBanned) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            VibeAvatar(name = user.displayName, size = 40.dp)
                            if (user.isOnline) {
                                Box(modifier = Modifier.align(Alignment.BottomEnd).size(10.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(user.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                if (user.isBanned) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(VibeI18n.t("admin_banned_label"), style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                when (user.role) {
                                    "super_admin" -> VibeI18n.t("admin_role_creator")
                                    "admin" -> VibeI18n.t("admin_role_admin_name")
                                    else -> VibeI18n.t("admin_role_user_name")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when (user.role) {
                                    "super_admin" -> MaterialTheme.colorScheme.tertiary
                                    "admin" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        if (user.role == "super_admin") {
                            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isSuperAdmin && user.role != "super_admin") {
                                    IconButton(onClick = { onPromote(user) }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            if (user.role == "admin") Icons.Default.Person else Icons.Default.Security,
                                            contentDescription = if (user.role == "admin") VibeI18n.t("admin_remove_admin") else VibeI18n.t("admin_make_admin"),
                                            modifier = Modifier.size(18.dp),
                                            tint = if (user.role == "admin") MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (isAdmin && user.role != "super_admin") {
                                    IconButton(onClick = {
                                        if (user.isBanned) onUnban(user) else onBan(user)
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            if (user.isBanned) Icons.Default.Unpublished else Icons.Default.Block,
                                            contentDescription = if (user.isBanned) VibeI18n.t("admin_unban") else VibeI18n.t("admin_ban"),
                                            modifier = Modifier.size(18.dp),
                                            tint = if (user.isBanned) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsTab(stats: AdminStats) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(VibeI18n.t("admin_stats"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(VibeI18n.t("admin_total"), "${stats.totalUsers}", Modifier.weight(1f))
            StatCard(VibeI18n.t("admin_online"), "${stats.onlineUsers}", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(VibeI18n.t("admin_admins"), "${stats.adminCount}", Modifier.weight(1f))
            StatCard(VibeI18n.t("admin_banned"), "${stats.bannedCount}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun LogsTab(logs: List<AdminLog>) {
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(VibeI18n.t("admin_no_records"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(logs, key = { it.id }) { log ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (log.action) {
                            "ban" -> Icons.Default.Block
                            "unban" -> Icons.Default.Unpublished
                            "promote" -> Icons.Default.Security
                            "demote" -> Icons.Default.Person
                            else -> Icons.Default.Security
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when (log.action) {
                            "ban" -> MaterialTheme.colorScheme.error
                            "unban" -> MaterialTheme.colorScheme.primary
                            "promote" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when (log.action) {
                                "ban" -> VibeI18n.t("admin_log_banned")
                                "unban" -> VibeI18n.t("admin_log_unbanned")
                                "promote" -> VibeI18n.t("admin_log_promoted")
                                "demote" -> VibeI18n.t("admin_log_demoted")
                                else -> log.action
                            },
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium
                        )
                        log.targetName?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(log.createdAt.take(16), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
