package com.vibe.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import com.vibe.ui.theme.VibeColors
import com.vibe.ui.theme.VibeTheme

/**
 * Vibe Input component - Design System 1.0
 * Supports: Text, Password, Search variants
 */
class VibeInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class InputType {
        TEXT,
        PASSWORD,
        SEARCH
    }

    private var inputType = InputType.TEXT
    private var hintText = ""
    private var inputText = ""
    private var isError = false
    private var errorMessage = ""

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val background = RectF()

    init {
        setupPaint()
    }

    private fun setupPaint() {
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)

        hintPaint.textAlign = Paint.Align.LEFT
        hintPaint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
    }

    fun setInputType(type: InputType) {
        this.inputType = type
        invalidate()
    }

    fun setHint(hint: String) {
        this.hintText = hint
        invalidate()
    }

    fun setText(text: String) {
        this.inputText = text
        invalidate()
    }

    fun setError(error: Boolean, message: String = "") {
        this.isError = error
        this.errorMessage = message
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = VibeTheme.Spacing.xxl * 3
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cornerRadius = VibeTheme.Radius.md.toFloat()
        background.set(0f, 0f, width.toFloat(), height.toFloat())

        // Draw background
        paint.color = if (isError) VibeColors.Error else VibeColors.Surface
        canvas.drawRoundRect(background, cornerRadius, cornerRadius, paint)

        // Draw border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (isError) 2f else 1f
        paint.color = if (isError) VibeColors.Error else VibeColors.Divider
        canvas.drawRoundRect(background, cornerRadius, cornerRadius, paint)
        paint.style = Paint.Style.FILL

        // Draw text or hint
        val textX = VibeTheme.Spacing.lg.toFloat()
        val textY = height / 2f

        if (inputText.isNotEmpty()) {
            textPaint.color = VibeColors.TextPrimary
            textPaint.textSize = 14f * resources.displayMetrics.density
            canvas.drawText(inputText, textX, textY + textPaint.textSize / 3, textPaint)
        } else {
            hintPaint.color = VibeColors.TextTertiary
            hintPaint.textSize = 14f * resources.displayMetrics.density
            canvas.drawText(hintText, textX, textY + hintPaint.textSize / 3, hintPaint)
        }
    }
}
