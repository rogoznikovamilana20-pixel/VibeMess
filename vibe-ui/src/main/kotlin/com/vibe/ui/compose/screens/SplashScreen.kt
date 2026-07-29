package com.vibe.ui.compose.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val rotate = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1: Initial scale + rotate in
        scale.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        rotate.animateTo(360f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, animationSpec = tween(400))

        delay(200)
        // Phase 2: Subtitle fade in
        subtitleAlpha.animateTo(1f, animationSpec = tween(400))

        // Phase 3: Pulse animation
        for (i in 1..2) {
            pulseScale.animateTo(1.08f, animationSpec = tween(200, easing = FastOutSlowInEasing))
            pulseScale.animateTo(1f, animationSpec = tween(200, easing = FastOutSlowInEasing))
        }

        delay(300)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF120820),
                        Color(0xFF1A0F2E),
                        MaterialTheme.colorScheme.background
                    ),
                    center = androidx.compose.ui.geometry.Offset(0.5f, 0.45f),
                    radius = 1.3f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo with pulse and rotation
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale.value * pulseScale.value)
                    .alpha(alpha.value)
                    .rotate(rotate.value)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8D2BFA),
                                Color(0xFFB06BFF),
                                Color(0xFF8D2BFA)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(1f, 1f)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "V",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App name with gradient text effect
            Text(
                text = "Vibe",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            Text(
                text = "Общение. AI. Люди.\nВсё в одном пространстве.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFA8A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Animated loading dots
            LoadingDots(alpha = subtitleAlpha.value)
        }
    }
}

@Composable
private fun LoadingDots(alpha: Float) {
    val dotCount = 3
    Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        modifier = Modifier.alpha(alpha)
    ) {
        (0 until dotCount).forEach { index ->
            val anim = remember { Animatable(0f) }
            LaunchedEffect(key1 = index) {
                delay((index * 150L).toLong())
                while (true) {
                    anim.animateTo(1f, animationSpec = tween(600, delayMillis = 0))
                    anim.animateTo(0f, animationSpec = tween(600, delayMillis = 0))
                }
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(0.5f + 0.5f * anim.value)
                    .alpha(0.3f + 0.7f * anim.value)
                    .background(Color(0xFF8D2BFA), CircleShape)
            )
        }
    }
}