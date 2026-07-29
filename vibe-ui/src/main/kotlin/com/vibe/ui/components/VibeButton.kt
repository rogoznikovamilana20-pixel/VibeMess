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
 * Vibe Button component - Design System 1.0
 * Supports: Primary, Secondary, Ghost, Outline variants
 */
class VibeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Variant {
        PRIMARY,
        SECONDARY,
        GHOST,
        OUTLINE
    }

    enum class Size {
        SMALL,
        MEDIUM,
        LARGE
    }

    private var variant = Variant.PRIMARY
    private var size = Size.MEDIUM
    private var buttonText = ""
    private var isEnabled = true

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val background = GradientDrawable()

    init {
        setupPaint()
    }

    private fun setupPaint() {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    }

    fun setVariant(variant: Variant) {
        this.variant = variant
        updateColors()
        invalidate()
    }

    fun setSize(size: Size) {
        this.size = size
        requestLayout()
        invalidate()
    }

    fun setText(text: String) {
        this.buttonText = text
        invalidate()
    }

    override fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        alpha = if (enabled) 1f else 0.5f
        invalidate()
    }

    private fun updateColors() {
        when (variant) {
            Variant.PRIMARY -> {
                background.setColor(VibeColors.Primary)
                textPaint.color = VibeColors.TextPrimary
            }
            Variant.SECONDARY -> {
                background.setColor(VibeColors.Surface)
                textPaint.color = VibeColors.TextPrimary
            }
            Variant.GHOST -> {
                background.setColor(android.graphics.Color.TRANSPARENT)
                textPaint.color = VibeColors.Primary
            }
            Variant.OUTLINE -> {
                background.setColor(android.graphics.Color.TRANSPARENT)
                background.setStroke(2, VibeColors.Primary)
                textPaint.color = VibeColors.Primary
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val height = when (size) {
            Size.SMALL -> VibeTheme.Spacing.xl * 2
            Size.MEDIUM -> VibeTheme.Spacing.xxl * 2
            Size.LARGE -> VibeTheme.Spacing.xxxl
        }

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(widthSize, 300)
            else -> 300
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cornerRadius = when (size) {
            Size.SMALL -> VibeTheme.Radius.sm.toFloat()
            Size.MEDIUM -> VibeTheme.Radius.md.toFloat()
            Size.LARGE -> VibeTheme.Radius.lg.toFloat()
        }

        background.cornerRadius = cornerRadius
        background.setBounds(0, 0, width, height)
        background.draw(canvas)

        updateColors()

        textPaint.textSize = when (size) {
            Size.SMALL -> 12f * resources.displayMetrics.density
            Size.MEDIUM -> 14f * resources.displayMetrics.density
            Size.LARGE -> 16f * resources.displayMetrics.density
        }

        val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(buttonText, width / 2f, textY, textPaint)
    }
}
