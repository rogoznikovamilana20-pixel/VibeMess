package com.vibe.ui.compose.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val VibeShapes = Shapes(
    // Chat bubble — outgoing
    extraSmall = RoundedCornerShape(
        topStart = 10.dp, topEnd = 10.dp,
        bottomEnd = 10.dp, bottomStart = 4.dp
    ),
    // Chat bubble — incoming
    small = RoundedCornerShape(
        topStart = 14.dp, topEnd = 14.dp,
        bottomEnd = 4.dp, bottomStart = 14.dp
    ),
    // Buttons: 14dp
    medium = RoundedCornerShape(14.dp),
    // Cards: 16dp
    large = RoundedCornerShape(16.dp),
    // Bottom sheet: 24dp
    extraLarge = RoundedCornerShape(24.dp)
)

// Convenience shapes for components
object VibeComponentShapes {
    val Button = RoundedCornerShape(14.dp)
    val Input = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(16.dp)
    val Badge = RoundedCornerShape(10.dp)
    val BottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val Avatar = CircleShape
}
