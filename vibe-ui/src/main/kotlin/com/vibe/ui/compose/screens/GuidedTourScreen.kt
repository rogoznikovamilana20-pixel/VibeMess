package com.vibe.ui.compose.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vibe.ui.compose.components.VibeLottieAnimation
import com.vibe.ui.i18n.VibeI18n
import kotlinx.coroutines.launch

private data class TourSlide(
    val asset: String,
    val accent: Color,
    val titleKey: String,
    val descKey: String,
    val chips: List<String>
)

private val slides = listOf(
    TourSlide(
        asset = "tour_chat.json",
        accent = Color(0xFF8D2BFA),
        titleKey = "tour_chat_title",
        descKey = "tour_chat_desc",
        chips = listOf("@aurion", "AI-ответы", "Перевод")
    ),
    TourSlide(
        asset = "tour_channels.json",
        accent = Color(0xFF0EA5E9),
        titleKey = "tour_channels_title",
        descKey = "tour_channels_desc",
        chips = listOf("Каналы", "Боты-платформа", "Токены")
    ),
    TourSlide(
        asset = "tour_sparks.json",
        accent = Color(0xFFF59E0B),
        titleKey = "tour_sparks_title",
        descKey = "tour_sparks_desc",
        chips = listOf("Искры", "Vibe+", "Маркетплейс")
    ),
    TourSlide(
        asset = "tour_mesh.json",
        accent = Color(0xFF14B8A6),
        titleKey = "tour_mesh_title",
        descKey = "tour_mesh_desc",
        chips = listOf("Wi-Fi Direct", "BLE", "Офлайн")
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GuidedTourScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == slides.size - 1
    val infinite = rememberInfiniteTransition(label = "aurora")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val currentAccent by animateFloatAsState(
        targetValue = pagerState.currentPage.toFloat(),
        animationSpec = tween(durationMillis = 350),
        label = "accent"
    )
    val accentColor = lerpAccent(currentAccent)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0F2E), MaterialTheme.colorScheme.background),
                    startY = 0f,
                    endY = 900f
                )
            )
    ) {
        // Aurora blobs
        Box(
            modifier = Modifier
                .size(320.dp)
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(
                        listOf(accentColor.copy(alpha = 0.5f), Color.Transparent)
                    ),
                    CircleShape
                )
                .align(Alignment.TopCenter)
                .padding(top = 120.dp)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onComplete) {
                    Text(VibeI18n.t("skip"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                TourSlideContent(slide = slides[page])
            }

            // Progress pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                slides.indices.forEach { i ->
                    val active = i == pagerState.currentPage
                    val pillWidth by animateFloatAsState(
                        targetValue = if (active) 28f else 8f,
                        animationSpec = tween(durationMillis = 280),
                        label = "pill$i"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(pillWidth.dp, 8.dp)
                            .background(
                                if (active) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isLast) {
                        onComplete()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor
                )
            ) {
                Text(
                    text = if (isLast) VibeI18n.t("tour_start") else VibeI18n.t("next"),
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun TourSlideContent(slide: TourSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        listOf(slide.accent.copy(alpha = 0.28f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            VibeLottieAnimation(
                asset = slide.asset,
                modifier = Modifier.size(240.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = VibeI18n.t(slide.titleKey),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = VibeI18n.t(slide.descKey),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            slide.chips.forEach { chip ->
                Box(
                    modifier = Modifier
                        .background(
                            slide.accent.copy(alpha = 0.14f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = slide.accent
                    )
                }
            }
        }
    }
}

private fun lerpAccent(position: Float): Color {
    val clamped = position.coerceIn(0f, slides.size - 1f)
    val from = slides[clamped.toInt()]
    val to = slides[(clamped.toInt() + 1).coerceAtMost(slides.size - 1)]
    val fraction = clamped - clamped.toInt()
    return Color(
        red = from.accent.red + (to.accent.red - from.accent.red) * fraction,
        green = from.accent.green + (to.accent.green - from.accent.green) * fraction,
        blue = from.accent.blue + (to.accent.blue - from.accent.blue) * fraction,
        alpha = 1f
    )
}
