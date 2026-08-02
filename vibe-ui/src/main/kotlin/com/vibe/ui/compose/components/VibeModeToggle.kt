package com.vibe.ui.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class VibeMode(val label: String, val icon: ImageVector) {
    PERSONAL("Личное", Icons.Default.Favorite),
    WORK("Работа", Icons.Default.Work)
}

@Composable
fun VibeModeToggle(
    selectedMode: VibeMode,
    onModeSelected: (VibeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedBg = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
    ) {
        // Sliding indicator
        val offsetFraction = if (selectedMode == VibeMode.PERSONAL) 0f else 1f
        val offsetX by animateFloatAsState(
            targetValue = offsetFraction,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
            label = "toggleOffset"
        )

        val indicatorWidth = 0.5f
        Box(
            modifier = Modifier
                .fillMaxWidth(indicatorWidth)
                .padding(2.dp)
                .let { mod ->
                    if (offsetX == 0f) mod.align(Alignment.CenterStart)
                    else mod.align(Alignment.CenterEnd)
                }
                .clip(RoundedCornerShape(20.dp))
                .background(selectedBg)
        )

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VibeMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface
                                  else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "textColor"
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onModeSelected(mode) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                        tint = if (isSelected) {
                            when (mode) {
                                VibeMode.PERSONAL -> Color(0xFF8D2BFA)
                                VibeMode.WORK -> Color(0xFF10B6FA)
                            }
                        } else textColor
                    )
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
