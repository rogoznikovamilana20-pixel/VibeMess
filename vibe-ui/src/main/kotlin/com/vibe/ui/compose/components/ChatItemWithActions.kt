package com.vibe.ui.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibe.ui.focus.FocusSpace
import com.vibe.ui.i18n.VibeI18n

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItemWithActions(
    title: String,
    lastMessage: String,
    space: FocusSpace,
    threatLevel: com.vibe.ui.e2e.ThreatLevel = com.vibe.ui.e2e.ThreatLevel.NONE,
    onClick: () -> Unit,
    onMoveToPersonal: () -> Unit,
    onMoveToWork: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog = androidx.compose.runtime.remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDialog.value = true }
            ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (threatLevel != com.vibe.ui.e2e.ThreatLevel.NONE) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ThreatIndicator(threatLevel = threatLevel)
                    }
                }
                if (lastMessage.isNotBlank()) {
                    Text(
                        lastMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            SpaceChip(space = space)
        }
    }

    if (showDialog.value) {
        ChatActionDialog(
            currentSpace = space,
            onMoveToPersonal = { onMoveToPersonal(); showDialog.value = false },
            onMoveToWork = { onMoveToWork(); showDialog.value = false },
            onRename = { onRename(); showDialog.value = false },
            onDelete = { onDelete(); showDialog.value = false },
            onDismiss = { showDialog.value = false }
        )
    }
}

@Composable
private fun SpaceChip(space: FocusSpace) {
    val (icon, color) = when (space) {
        FocusSpace.PERSONAL -> Icons.Default.Person to MaterialTheme.colorScheme.primary
        FocusSpace.WORK -> Icons.Default.BusinessCenter to MaterialTheme.colorScheme.secondary
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                space.label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun ChatActionDialog(
    currentSpace: FocusSpace,
    onMoveToPersonal: () -> Unit,
    onMoveToWork: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(VibeI18n.t("chat_actions_title")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentSpace != FocusSpace.PERSONAL) {
                    ActionRow(
                        icon = Icons.Default.Person,
                        label = VibeI18n.t("send_to_personal"),
                        onClick = onMoveToPersonal
                    )
                }
                if (currentSpace != FocusSpace.WORK) {
                    ActionRow(
                        icon = Icons.Default.BusinessCenter,
                        label = VibeI18n.t("send_to_work"),
                        onClick = onMoveToWork
                    )
                }
                ActionRow(
                    icon = Icons.Default.Edit,
                    label = VibeI18n.t("rename_chat"),
                    onClick = onRename
                )
                ActionRow(
                    icon = Icons.Default.Delete,
                    label = VibeI18n.t("delete"),
                    onClick = onDelete
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(VibeI18n.t("cancel"))
            }
        }
    )
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
