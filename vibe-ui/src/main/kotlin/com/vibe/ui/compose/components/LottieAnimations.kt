package com.vibe.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun VibeLottieAnimation(
    asset: String,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    isPlaying: Boolean = true,
    speed: Float = 1f,
    contentScale: ContentScale = ContentScale.Fit
) {
    val composition = rememberLottieComposition(LottieCompositionSpec.Asset(asset))
    LottieAnimation(
        composition = composition.value,
        modifier = modifier,
        iterations = iterations,
        isPlaying = isPlaying && composition.value != null,
        speed = speed,
        contentScale = contentScale
    )
}
