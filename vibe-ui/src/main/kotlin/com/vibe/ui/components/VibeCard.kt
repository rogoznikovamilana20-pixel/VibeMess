package com.vibe.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import com.vibe.ui.theme.VibeColors
import com.vibe.ui.theme.VibeTheme

/**
 * Vibe Card component - Design System 1.0
 * Container for content with consistent styling
 */
class VibeCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class CardType {
        DEFAULT,
        ELEVATED,
        OUTLINED
    }

    private var cardType = CardType.DEFAULT
    private val background = GradientDrawable()

    init {
        setupBackground()
    }

    private fun setupBackground() {
        background.setColor(VibeColors.Surface)
        background.cornerRadius = VibeTheme.Radius.lg.toFloat()
        setBackground(background)
    }

    fun setCardType(type: CardType) {
        this.cardType = type
        when (type) {
            CardType.DEFAULT -> {
                background.setColor(VibeColors.Surface)
                background.setStroke(0, android.graphics.Color.TRANSPARENT)
            }
            CardType.ELEVATED -> {
                background.setColor(VibeColors.Surface)
                background.setStroke(0, android.graphics.Color.TRANSPARENT)
                elevation = VibeTheme.Elevation.lg
            }
            CardType.OUTLINED -> {
                background.setColor(android.graphics.Color.TRANSPARENT)
                background.setStroke(1, VibeColors.Divider)
            }
        }
        invalidate()
    }
}
