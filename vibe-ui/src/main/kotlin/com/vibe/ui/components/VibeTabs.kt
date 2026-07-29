package com.vibe.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.vibe.ui.theme.VibeColors
import com.vibe.ui.theme.VibeTheme

/**
 * Vibe Tabs / Segmented Control - Design System 1.0
 * Used for Personal/Work mode switching
 */
class VibeTabs @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var tabs = listOf<String>()
    private var selectedIndex = 0
    private var onTabSelected: ((Int) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val background = RectF()

    init {
        setupPaint()
    }

    private fun setupPaint() {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)

        indicatorPaint.color = VibeColors.Primary
    }

    fun setTabs(tabList: List<String>) {
        this.tabs = tabList
        requestLayout()
        invalidate()
    }

    fun setSelectedIndex(index: Int) {
        this.selectedIndex = index
        invalidate()
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        this.onTabSelected = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = VibeTheme.Spacing.xxl * 2
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (tabs.isEmpty()) return

        val cornerRadius = VibeTheme.Radius.md.toFloat()
        background.set(0f, 0f, width.toFloat(), height.toFloat())

        // Draw background
        paint.color = VibeColors.Surface
        canvas.drawRoundRect(background, cornerRadius, cornerRadius, paint)

        val tabWidth = width.toFloat() / tabs.size

        // Draw selected indicator
        val indicatorLeft = selectedIndex * tabWidth + VibeTheme.Spacing.xs
        val indicatorRight = (selectedIndex + 1) * tabWidth - VibeTheme.Spacing.xs
        val indicatorRect = RectF(indicatorLeft, VibeTheme.Spacing.xs.toFloat(), indicatorRight, height.toFloat() - VibeTheme.Spacing.xs)
        indicatorPaint.color = VibeColors.Primary
        canvas.drawRoundRect(indicatorRect, cornerRadius - VibeTheme.Spacing.xs, cornerRadius - VibeTheme.Spacing.xs, indicatorPaint)

        // Draw tab texts
        textPaint.textSize = 14f * resources.displayMetrics.density
        tabs.forEachIndexed { index, tab ->
            val textX = index * tabWidth + tabWidth / 2
            val textY = height / 2f + textPaint.textSize / 3

            textPaint.color = if (index == selectedIndex) VibeColors.TextPrimary else VibeColors.TextSecondary
            canvas.drawText(tab, textX, textY, textPaint)
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            val tabWidth = width.toFloat() / tabs.size
            val index = (event.x / tabWidth).toInt()
            if (index in tabs.indices && index != selectedIndex) {
                selectedIndex = index
                onTabSelected?.invoke(index)
                invalidate()
            }
        }
        return true
    }
}
