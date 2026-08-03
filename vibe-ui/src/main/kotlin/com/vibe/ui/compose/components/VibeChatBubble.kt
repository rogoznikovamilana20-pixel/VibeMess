package com.vibe.ui.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BubbleReaction(
    val emoji: String,
    val count: Int,
    val isSelected: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VibeChatBubble(
    text: String,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier,
    time: String = "",
    status: MessageStatus = MessageStatus.SENT,
    reactions: List<BubbleReaction> = emptyList(),
    replyPreview: String? = null,
    mediaPreview: @Composable (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val bubbleColor by animateColorAsState(
        targetValue = if (isOutgoing) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant,
        label = "bubbleColor"
    )
    val textColor = if (isOutgoing) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
    val timeColor = if (isOutgoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onLongClick = onLongClick, onClick = {})
                } else Modifier
            ),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        if (!isOutgoing) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Reply preview
            if (replyPreview != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(if (isOutgoing) Color(0xFF6B1FCC) else Color(0xFF2A2645))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "Reply",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOutgoing) Color(0xFFB06BFF) else Color(0xFF7C3AED),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = replyPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = timeColor,
                            maxLines = 2
                        )
                    }
                }
            }

            // Main bubble
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    mediaPreview?.invoke()

                    if (text.isNotEmpty()) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Time + status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelSmall,
                            color = timeColor,
                            fontSize = 11.sp
                        )
                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = when (status) {
                                    MessageStatus.SENDING -> Icons.Default.Check
                                    MessageStatus.SENT -> Icons.Default.Check
                                    MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                    MessageStatus.READ -> Icons.Default.DoneAll
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (status == MessageStatus.READ) Color(0xFF4ADE80) else timeColor
                            )
                        }
                    }
                }
            }

            // Reactions strip
            if (reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    reactions.forEach { reaction ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (reaction.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${reaction.emoji} ${reaction.count}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (isOutgoing) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ
}
