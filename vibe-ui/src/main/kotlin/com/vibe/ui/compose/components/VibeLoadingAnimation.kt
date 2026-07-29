package com.vibe.ui.compose.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    dotCount: Int = 3,
    dotSize: Dp = Dp.Unspecified,
    color: Color = Color(0xFF8D2BFA)
) {
    val transition = rememberInfiniteTransition(label = "dots")
    val dots = List(dotCount) { index ->
        val delay = index * 200
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delay, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot$index"
        )
    }

    val dotDp = if (dotSize == Dp.Unspecified) 8.dp else dotSize
    Canvas(modifier = modifier.size((dotCount * 16).dp, 24.dp)) {
        val dotRadius = dotDp.toPx() / 2
        val spacing = dotDp.toPx() * 2
        val totalWidth = (dotCount - 1) * spacing
        val startX = (size.width - totalWidth) / 2
        dots.forEachIndexed { index, alpha ->
            drawCircle(
                color = color.copy(alpha = alpha.value),
                radius = dotRadius,
                center = Offset(startX + index * spacing, size.height / 2)
            )
        }
    }
}

@Composable
fun PulseRing(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF8D2BFA),
    ringCount: Int = 3
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val rings = List(ringCount) { index ->
        val delay = index * 400
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, delayMillis = delay, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring$index"
        )
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2
        rings.forEach { progress ->
            val radius = maxRadius * progress.value
            val alpha = (1f - progress.value) * 0.4f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}
