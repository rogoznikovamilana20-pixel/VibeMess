package com.vibe.ui.compose.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class VaybikMood {
    WAVING, HAPPY, THINKING, SAD, CELEBRATE
}

@Composable
fun VaybikCharacter(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    mood: VaybikMood = VaybikMood.WAVING,
    showMessage: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vaybikFloat")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vaybikFloat"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = size.toPx() / 2
            val cy = size.toPx() / 2 + floatAnim * 4
            val r = size.toPx() * 0.35f

            // Body circle
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = r * 1.4f,
                center = Offset(cx, cy + r * 0.2f)
            )

            // Head
            drawCircle(
                color = primaryColor,
                radius = r,
                center = Offset(cx, cy)
            )

            // Eyes
            val eyeOffsetX = r * 0.25f
            val eyeOffsetY = r * 0.15f
            val eyeR = r * 0.08f

            when (mood) {
                VaybikMood.WAVING, VaybikMood.HAPPY -> {
                    drawCircle(Color.White, eyeR, Offset(cx - eyeOffsetX, cy - eyeOffsetY))
                    drawCircle(Color.White, eyeR, Offset(cx + eyeOffsetX, cy - eyeOffsetY))
                    drawCircle(Color(0xFF1A0F2E), eyeR * 0.5f, Offset(cx - eyeOffsetX, cy - eyeOffsetY))
                    drawCircle(Color(0xFF1A0F2E), eyeR * 0.5f, Offset(cx + eyeOffsetX, cy - eyeOffsetY))
                }
                VaybikMood.THINKING -> {
                    drawCircle(Color.White, eyeR, Offset(cx - eyeOffsetX, cy - eyeOffsetY))
                    drawCircle(Color.White, eyeR * 0.7f, Offset(cx + eyeOffsetX, cy - eyeOffsetY))
                    drawCircle(Color(0xFF1A0F2E), eyeR * 0.5f, Offset(cx - eyeOffsetX, cy - eyeOffsetY))
                }
                VaybikMood.SAD -> {
                    drawCircle(Color.White, eyeR * 0.7f, Offset(cx - eyeOffsetX, cy - eyeOffsetY))
                    drawCircle(Color.White, eyeR * 0.7f, Offset(cx + eyeOffsetX, cy - eyeOffsetY))
                }
                VaybikMood.CELEBRATE -> {
                    drawCircle(Color.White, eyeR * 1.2f, Offset(cx - eyeOffsetX, cy - eyeOffsetY))
                    drawCircle(Color.White, eyeR * 1.2f, Offset(cx + eyeOffsetX, cy - eyeOffsetY))
                    drawCircle(Color(0xFF1A0F2E), eyeR * 0.6f, Offset(cx - eyeOffsetX, cy - eyeOffsetY))
                    drawCircle(Color(0xFF1A0F2E), eyeR * 0.6f, Offset(cx + eyeOffsetX, cy - eyeOffsetY))
                }
            }

            // Smile
            when (mood) {
                VaybikMood.HAPPY, VaybikMood.CELEBRATE -> {
                    val smilePath = Path().apply {
                        moveTo(cx - r * 0.25f, cy + r * 0.1f)
                        cubicTo(cx - r * 0.1f, cy + r * 0.45f, cx + r * 0.1f, cy + r * 0.45f, cx + r * 0.25f, cy + r * 0.1f)
                    }
                    drawPath(smilePath, Color.White, style = Stroke(width = 2f))
                }
                VaybikMood.WAVING -> {
                    val smilePath = Path().apply {
                        moveTo(cx - r * 0.2f, cy + r * 0.1f)
                        cubicTo(cx - r * 0.05f, cy + r * 0.3f, cx + r * 0.05f, cy + r * 0.3f, cx + r * 0.2f, cy + r * 0.1f)
                    }
                    drawPath(smilePath, Color.White, style = Stroke(width = 2f))
                }
                VaybikMood.THINKING -> {
                    drawCircle(Color.White, r * 0.08f, Offset(cx - r * 0.15f, cy + r * 0.2f))
                }
                VaybikMood.SAD -> {
                    val sadPath = Path().apply {
                        moveTo(cx - r * 0.2f, cy + r * 0.25f)
                        cubicTo(cx - r * 0.05f, cy + r * 0.05f, cx + r * 0.05f, cy + r * 0.05f, cx + r * 0.2f, cy + r * 0.25f)
                    }
                    drawPath(sadPath, Color(0xFFA8A3B8), style = Stroke(width = 2f))
                }
            }

            // Waving hand
            if (mood == VaybikMood.WAVING) {
                val handAngle = floatAnim * 0.5f
                translate(left = cx + r * 0.5f, top = cy - r * 0.5f) {
                    val handPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(r * 0.3f, -r * 0.2f + handAngle * r * 0.2f)
                    }
                    drawPath(handPath, primaryColor, style = Stroke(width = 3f))
                }
            }

            // Sparkles for celebrate
            if (mood == VaybikMood.CELEBRATE) {
                val sparkleAlpha = (0.5f + floatAnim * 0.5f)
                drawCircle(Color(0xFFF59E0B).copy(alpha = sparkleAlpha), r * 0.1f, Offset(cx - r * 0.7f, cy - r * 0.8f))
                drawCircle(Color(0xFF8D2BFA).copy(alpha = sparkleAlpha), r * 0.08f, Offset(cx + r * 0.8f, cy - r * 0.6f))
                drawCircle(Color(0xFF10B6FA).copy(alpha = sparkleAlpha), r * 0.12f, Offset(cx + r * 0.1f, cy - r * 1.0f))
            }
        }
    }
}
