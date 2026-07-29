package com.vibe.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.vibe.ui.theme.VibeColors
import com.vibe.ui.theme.VibeDimens

/**
 * Circular avatar view with initials fallback.
 */
class VibeAvatar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VibeColors.TextPrimary
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    private var avatarSize = VibeDimens.AVATAR_SIZE_MD
    private var initials = ""
    private var backgroundColor = VibeColors.Primary
    private var showOnlineIndicator = false
    private var isOnline = false

    fun setAvatar(name: String, size: Int = VibeDimens.AVATAR_SIZE_MD) {
        this.avatarSize = size
        this.initials = getInitials(name)
        this.backgroundColor = getColorForName(name)
        requestLayout()
        invalidate()
    }

    fun setOnline(online: Boolean) {
        this.isOnline = online
        this.showOnlineIndicator = true
        invalidate()
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex())
        return when {
            parts.size >= 2 -> "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}"
            parts.isNotEmpty() -> parts[0].firstOrNull()?.toString() ?: ""
            else -> ""
        }.uppercase()
    }

    private fun getColorForName(name: String): Int {
        val colors = intArrayOf(
            VibeColors.Primary,
            VibeColors.PrimaryLight,
            VibeColors.AccentBlue,
            VibeColors.Info,
            VibeColors.Success,
            VibeColors.Warning
        )
        return colors[Math.abs(name.hashCode()) % colors.size]
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = VibeDimens.dp(avatarSize)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = width.toFloat()
        val padding = size * 0.05f

        rect.set(padding, padding, size - padding, size - padding)

        // Background circle
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, size / 2, size / 2, paint)

        // Initials
        textPaint.textSize = size * 0.35f
        val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(initials, rect.centerX(), textY, textPaint)

        // Online indicator
        if (showOnlineIndicator) {
            val indicatorSize = size * 0.25f
            val indicatorX = size - indicatorSize / 2 - padding
            val indicatorY = size - indicatorSize / 2 - padding

            paint.color = VibeColors.Background
            paint.style = Paint.Style.FILL
            canvas.drawCircle(indicatorX, indicatorY, indicatorSize / 2 + 2f, paint)

            paint.color = if (isOnline) VibeColors.Online else VibeColors.Offline
            canvas.drawCircle(indicatorX, indicatorY, indicatorSize / 2, paint)
        }
    }
}
