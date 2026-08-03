package com.vibe.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.ui.compose.theme.Error
import com.vibe.ui.compose.theme.Success
import com.vibe.ui.compose.theme.VibeComponentShapes
import com.vibe.ui.compose.theme.Warning

enum class VibeBadgeVariant { Default, Success, Warning, Error }

@Composable
fun VibeBadge(
    text: String,
    variant: VibeBadgeVariant = VibeBadgeVariant.Default,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (variant) {
        VibeBadgeVariant.Default -> MaterialTheme.colorScheme.primary
        VibeBadgeVariant.Success -> Success
        VibeBadgeVariant.Warning -> Warning
        VibeBadgeVariant.Error -> Error
    }

    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
        modifier = modifier
            .clip(VibeComponentShapes.Badge)
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        maxLines = 1
    )
}

@Composable
fun VibeBadgeDot(
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(8.dp)
    ) {
        drawCircle(color = color)
    }
}
