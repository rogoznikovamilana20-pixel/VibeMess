package com.vibe.ui.theme

import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView

/**
 * Vibe Theme System - Design System 1.0
 * Centralized theme management for consistent UI
 */
object VibeTheme {

    // Typography scale
    object Typography {
        private const val FONT_FAMILY = "sans-serif"

        fun displayLarge(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.BOLD)
            textView.setTextColor(VibeColors.TextPrimary)
        }

        fun displayMedium(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.BOLD)
            textView.setTextColor(VibeColors.TextPrimary)
        }

        fun headlineLarge(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.BOLD)
            textView.setTextColor(VibeColors.TextPrimary)
        }

        fun headlineMedium(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.BOLD)
            textView.setTextColor(VibeColors.TextPrimary)
        }

        fun titleLarge(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.BOLD)
            textView.setTextColor(VibeColors.TextPrimary)
        }

        fun titleMedium(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.BOLD)
            textView.setTextColor(VibeColors.TextPrimary)
        }

        fun bodyLarge(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.NORMAL)
            textView.setTextColor(VibeColors.TextPrimary)
        }

        fun bodyMedium(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.NORMAL)
            textView.setTextColor(VibeColors.TextSecondary)
        }

        fun bodySmall(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.NORMAL)
            textView.setTextColor(VibeColors.TextTertiary)
        }

        fun labelLarge(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.NORMAL)
            textView.setTextColor(VibeColors.TextPrimary)
        }

        fun labelMedium(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.NORMAL)
            textView.setTextColor(VibeColors.TextSecondary)
        }

        fun labelSmall(textView: TextView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            textView.typeface = Typeface.create(FONT_FAMILY, Typeface.NORMAL)
            textView.setTextColor(VibeColors.TextTertiary)
        }
    }

    // Spacing scale
    object Spacing {
        const val xs = 4
        const val sm = 8
        const val md = 12
        const val lg = 16
        const val xl = 24
        const val xxl = 32
        const val xxxl = 48
    }

    // Corner radius
    object Radius {
        const val sm = 8
        const val md = 12
        const val lg = 16
        const val xl = 24
        const val round = 999
    }

    // Elevation/Shadow
    object Elevation {
        const val none = 0f
        const val sm = 2f
        const val md = 4f
        const val lg = 8f
        const val xl = 16f
    }
}
