package com.vibe.ui.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VibeAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    photoUrl: String? = null,
    isOnline: Boolean? = null,
    hasStory: Boolean = false,
    badgeCount: Int? = null
) {
    val initials = remember(name) {
        val parts = name.trim().split("\\s+".toRegex())
        when {
            parts.size >= 2 -> "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}"
            parts.isNotEmpty() -> parts[0].firstOrNull()?.toString() ?: ""
            else -> "?"
        }.uppercase()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = remember(name, primaryColor) {
        val colors = listOf(
            primaryColor,
            Color(0xFF10B6FA),
            Color(0xFFEC4899),
            Color(0xFF4ADE80),
            Color(0xFFF59E0B),
            Color(0xFF0EA5E9)
        )
        colors[Math.abs(name.hashCode()) % colors.size]
    }

    val textSize = size.value * 0.35f

    Box(
        modifier = modifier.size(size + if (hasStory) 4.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (hasStory) {
            val storyColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.size(size + 4.dp)) {
                drawCircle(
                    color = storyColor,
                    radius = size.toPx() / 2 + 2.dp.toPx()
                )
            }
        }

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                // Placeholder for Coil/Glide integration
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = textSize.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = textSize.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Online indicator
        if (isOnline != null) {
            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF4ADE80) else Color(0xFF6B6580))
            )
        }

        // Badge
        if (badgeCount != null && badgeCount > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
