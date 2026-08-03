package com.vibe.ui.compose.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonLine(
    width: Float = 1f,
    height: Int = 16,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.Gray.copy(alpha = 0.2f),
            Color.Gray.copy(alpha = 0.4f),
            Color.Gray.copy(alpha = 0.2f)
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth(width)
            .height(height.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(shimmerBrush)
    )
}

@Composable
fun SkeletonChatItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonLine(width = 0.15f, height = 48, modifier = Modifier.weight(0.15f))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonLine(width = 0.6f, height = 16)
            SkeletonLine(width = 0.9f, height = 14)
        }
    }
}

@Composable
fun SkeletonMessageBubble(isOutgoing: Boolean = false, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        if (!isOutgoing) {
            SkeletonLine(width = 0.12f, height = 32, modifier = Modifier.weight(0.12f))
            Spacer(modifier = Modifier.width(8.dp))
        }
        SkeletonLine(
            width = 0.5f,
            height = 36,
            modifier = Modifier.weight(0.5f)
        )
        if (isOutgoing) Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
fun SkeletonChatList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(5) {
            SkeletonChatItem()
        }
    }
}

@Composable
fun SkeletonMessageList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(6) { i ->
            SkeletonMessageBubble(isOutgoing = i % 3 == 0)
        }
    }
}
