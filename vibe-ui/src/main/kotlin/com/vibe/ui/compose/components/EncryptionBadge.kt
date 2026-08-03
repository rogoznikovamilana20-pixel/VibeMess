package com.vibe.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
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
import com.vibe.ui.compose.theme.Info
import com.vibe.ui.compose.theme.Success

enum class EncryptionLevel { PQ_E2EE, E2EE, NONE }

@Composable
fun EncryptionBadge(
    level: EncryptionLevel,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when (level) {
        EncryptionLevel.PQ_E2EE -> Triple(Icons.Default.Shield, Success, "PQ E2EE")
        EncryptionLevel.E2EE -> Triple(Icons.Default.Lock, Info, "E2EE")
        EncryptionLevel.NONE -> Triple(Icons.Default.LockOpen, Error, "Unencrypted")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
