package com.vibe.ui.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vibe.ui.e2e.ThreatLevel

/**
 * Threat level indicator for chat list.
 * Shows security status with color-coded icon.
 */
@Composable
fun ThreatIndicator(
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (threatLevel) {
        ThreatLevel.NONE -> Icons.Default.CheckCircle to Color(0xFF4CAF50) // Green
        ThreatLevel.LOW -> Icons.Default.CheckCircle to Color(0xFF8BC34A) // Light green
        ThreatLevel.MEDIUM -> Icons.Default.Warning to Color(0xFFFFC107) // Yellow
        ThreatLevel.HIGH -> Icons.Default.Error to Color(0xFFFF9800) // Orange
        ThreatLevel.CRITICAL -> Icons.Default.Error to Color(0xFFF44336) // Red
    }

    val animatedColor by animateColorAsState(targetValue = color)

    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(animatedColor.copy(alpha = 0.2f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Threat level: $threatLevel",
            modifier = Modifier.size(12.dp),
            tint = animatedColor
        )
    }
}

/**
 * Security badge for chat header.
 */
@Composable
fun SecurityBadge(
    isEncrypted: Boolean,
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier
) {
    val color = when {
        threatLevel == ThreatLevel.HIGH || threatLevel == ThreatLevel.CRITICAL -> 
            MaterialTheme.colorScheme.error
        isEncrypted -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val text = when {
        threatLevel == ThreatLevel.CRITICAL -> "УГРОЗА"
        threatLevel == ThreatLevel.HIGH -> "Подозрительно"
        threatLevel == ThreatLevel.MEDIUM -> "Внимание"
        isEncrypted -> "E2EE"
        else -> "Не зашифрован"
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * E2EE encryption indicator.
 */
@Composable
fun EncryptionIndicator(
    isEncrypted: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isEncrypted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
    
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = if (isEncrypted) "Зашифровано" else "Не зашифровано",
        modifier = modifier.size(14.dp),
        tint = color
    )
}
