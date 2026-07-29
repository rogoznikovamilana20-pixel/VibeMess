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
 * Vibe Chip component - Design System 1.0
 * Selectable tag/filter chip
 */
class VibeChip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var chipText = ""
    private var isSelected = false
    private var onChipClick: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val background = GradientDrawable()

    init {
        setupPaint()
    }

    private fun setupPaint() {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
    }

    fun setText(text: String) {
        this.chipText = text
        invalidate()
    }

    fun setSelectedState(selected: Boolean) {
        this.isSelected = selected
        updateColors()
        invalidate()
    }

    fun setOnChipClickListener(listener: () -> Unit) {
        this.onChipClick = listener
    }

    private fun updateColors() {
        if (isSelected) {
            background.setColor(VibeColors.Primary)
            textPaint.color = VibeColors.TextPrimary
        } else {
            background.setColor(VibeColors.Surface)
            textPaint.color = VibeColors.TextSecondary
            background.setStroke(1, VibeColors.Divider)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = VibeTheme.Spacing.xl * 2
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        updateColors()

        background.cornerRadius = VibeTheme.Radius.round.toFloat()
        background.setBounds(0, 0, width, height)
        background.draw(canvas)

        textPaint.textSize = 12f * resources.displayMetrics.density
        val textY = height / 2f + textPaint.textSize / 3
        canvas.drawText(chipText, width / 2f, textY, textPaint)
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            isSelected = !isSelected
            onChipClick?.invoke()
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }
}
