package com.vibe.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.ui.compose.theme.VibePrimary
import com.vibe.ui.compose.theme.VibePurpleLight

private val avatarColors = listOf(
    Color(0xFF8D2BFA) to Color(0xFFB06BFF),
    Color(0xFF10B6FA) to Color(0xFF0EA5E9),
    Color(0xFF4ADE80) to Color(0xFF22C55E),
    Color(0xFFF59E0B) to Color(0xFFF97316),
    Color(0xFFEF4444) to Color(0xFFF97316),
    Color(0xFF14B8A6) to Color(0xFF06B6D4),
    Color(0xFFEC4899) to Color(0xFFF472B6),
    Color(0xFF6366F1) to Color(0xFF818CF8)
)

@Composable
fun AvatarPlaceholder(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    val initials = remember(name) {
        val parts = name.trim().split(" ")
        when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            name.isNotEmpty() -> name.first().uppercaseChar().toString()
            else -> "?"
        }
    }

    val colorIndex = remember(name) {
        name.hashCode().let { if (it < 0) -it else it } % avatarColors.size
    }
    val (color1, color2) = avatarColors[colorIndex]

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.linearGradient(listOf(color1, color2))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = (size / 2.5).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
