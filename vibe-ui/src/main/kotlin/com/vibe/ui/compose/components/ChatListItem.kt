package com.vibe.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.ui.compose.theme.Online
import com.vibe.ui.compose.theme.TextSecondaryDark
import com.vibe.ui.compose.theme.TextTertiaryDark
import com.vibe.ui.compose.theme.VibeComponentShapes

data class ChatListItemData(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val avatarUrl: String? = null,
    val avatarInitial: String = name.firstOrNull()?.uppercase() ?: "?"
)

@Composable
fun ChatListItem(
    data: ChatListItemData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online indicator
        Box {
            VibeAvatar(
                name = data.name,
                size = 44.dp,
                photoUrl = data.avatarUrl
            )
            if (data.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(Color(0xFF0F0D1A))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(Online)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + last message
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = data.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = data.lastMessage,
                color = TextSecondaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Timestamp + unread badge
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = data.timestamp,
                color = TextTertiaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
            if (data.unreadCount > 0) {
                VibeBadge(
                    text = if (data.unreadCount > 99) "99+" else data.unreadCount.toString(),
                    variant = VibeBadgeVariant.Default,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
