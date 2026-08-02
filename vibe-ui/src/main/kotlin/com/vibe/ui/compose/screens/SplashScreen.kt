package com.vibe.ui.compose.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.ui.compose.components.VibeLottieAnimation
import com.vibe.ui.i18n.VibeI18n
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val lottieAlpha = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "splash_aurora")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash_glow"
    )

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, animationSpec = tween(400))
        lottieAlpha.animateTo(1f, animationSpec = tween(500))

        delay(250)
        subtitleAlpha.animateTo(1f, animationSpec = tween(400))

        delay(900)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0F2E), MaterialTheme.colorScheme.background),
                    startY = 0f,
                    endY = 1000f
                )
            )
    ) {
        // Aurora glow blob (Energy Pulse)
        Box(
            modifier = Modifier
                .size(360.dp)
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF8B5CF6).copy(alpha = 0.5f), Color.Transparent)
                    ),
                    CircleShape
                )
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            // Energy Pulse orb (Lottie)
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale.value)
                    .alpha(lottieAlpha.value)
            ) {
                VibeLottieAnimation(
                    asset = "splash_pulse.json",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Vibe",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = VibeI18n.t("splash_tagline"),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFA8A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
    }
}
