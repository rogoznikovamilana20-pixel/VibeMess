package com.vibe.ui.compose.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VibePrimary,
    onPrimary = TextPrimaryDark,
    primaryContainer = VibePurpleSurface,
    onPrimaryContainer = VibePurpleLight,
    secondary = AccentBlue,
    onSecondary = TextPrimaryDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextSecondaryDark,
    tertiary = VibePurple,
    onTertiary = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = VibePrimary,
    error = Error,
    onError = TextPrimaryDark,
    outline = DividerDark,
    outlineVariant = SurfaceHighlightDark,
    scrim = Scrim
)

private val LightColorScheme = lightColorScheme(
    primary = VibePrimary,
    onPrimary = TextPrimaryDark,
    primaryContainer = SurfaceHighlightLight,
    onPrimaryContainer = VibePurpleDark,
    secondary = AccentBlue,
    onSecondary = TextPrimaryDark,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = TextSecondaryLight,
    tertiary = VibePurple,
    onTertiary = TextPrimaryDark,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = VibePrimary,
    error = Error,
    onError = TextPrimaryDark,
    outline = DividerLight,
    outlineVariant = SurfaceHighlightLight,
    scrim = Scrim
)

enum class VibeStyle(val label: String) {
    DEFAULT("Default"),
    NEON("Neon"),
    OCEAN("Ocean"),
    FOREST("Forest"),
    SUNSET("Sunset")
}

@Composable
fun VibeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: VibeStyle = VibeStyle.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VibeTypography,
        shapes = VibeShapes,
        content = content
    )
}
