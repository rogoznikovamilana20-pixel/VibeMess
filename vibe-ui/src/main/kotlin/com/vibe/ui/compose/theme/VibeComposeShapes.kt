package com.vibe.ui.compose.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val VibeShapes = Shapes(
    extraSmall = RoundedCornerShape(
        topStart = 10.dp,
        topEnd = 10.dp,
        bottomEnd = 10.dp,
        bottomStart = 4.dp
    ),
    small = RoundedCornerShape(
        topStart = 14.dp,
        topEnd = 14.dp,
        bottomEnd = 4.dp,
        bottomStart = 14.dp
    ),
    medium = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomEnd = 18.dp,
        bottomStart = 18.dp
    ),
    large = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 8.dp,
        bottomEnd = 28.dp,
        bottomStart = 28.dp
    ),
    extraLarge = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
        bottomEnd = 32.dp,
        bottomStart = 32.dp
    )
)
