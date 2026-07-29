package com.vibe.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.vibe.ui.theme.VibeColors
import com.vibe.ui.theme.VibeDimens

/**
 * Chat message bubble view.
 */
class VibeChatBubble @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VibeColors.MessageOutText
        textSize = VibeDimens.TEXT_SIZE_BODY * resources.displayMetrics.density
    }
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VibeColors.TextSecondary
        textSize = VibeDimens.TEXT_SIZE_CAPTION * resources.displayMetrics.density
    }
    private val bubblePath = Path()
    private val bubbleRect = RectF()

    private var isOutgoing = true
    private var messageText = ""
    private var timeText = ""
    private var showTail = true

    fun setMessage(text: String, isOutgoing: Boolean, time: String = "", tail: Boolean = true) {
        this.messageText = text
        this.isOutgoing = isOutgoing
        this.timeText = time
        this.showTail = tail
        textPaint.color = if (isOutgoing) VibeColors.MessageOutText else VibeColors.MessageInText
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec)
        val padding = VibeDimens.dp(VibeDimens.SPACE_SM)
        val bubblePadding = VibeDimens.dp(VibeDimens.SPACE_MD)

        val availWidth = maxWidth - padding * 2 - bubblePadding * 2
        val textWidth = textPaint.measureText(messageText).toInt()
        val wrappedWidth = minOf(textWidth, availWidth)

        val lines = wrapText(messageText, textPaint, availWidth.toFloat())
        val textHeight = lines.size * textPaint.textSize

        val timeWidth = timePaint.measureText(timeText).toInt()
        val contentHeight = textHeight + VibeDimens.dp(4) + timePaint.textSize

        val totalWidth = wrappedWidth + bubblePadding * 2 + padding * 2
        val totalHeight = contentHeight + bubblePadding * 2 + padding * 2

        setMeasuredDimension(totalWidth.toInt(), totalHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = VibeDimens.dp(VibeDimens.SPACE_SM).toFloat()
        val bubblePadding = VibeDimens.dp(VibeDimens.SPACE_MD).toFloat()
        val cornerRadius = VibeDimens.dp(VibeDimens.RADIUS_MD).toFloat()

        val bubbleColor = if (isOutgoing) VibeColors.MessageOut else VibeColors.MessageIn
        val bgColor = VibeColors.Background

        bubbleRect.set(
            padding,
            padding,
            width - padding,
            height - padding
        )

        // Draw bubble background
        paint.color = bubbleColor
        paint.style = Paint.Style.FILL
        bubblePath.reset()
        bubblePath.addRoundRect(bubbleRect, cornerRadius, cornerRadius, Path.Direction.CW)

        // Add tail
        if (showTail) {
            val tailSize = VibeDimens.dp(8).toFloat()
            if (isOutgoing) {
                bubblePath.moveTo(bubbleRect.right - cornerRadius, bubbleRect.bottom)
                bubblePath.lineTo(bubbleRect.right, bubbleRect.bottom + tailSize)
                bubblePath.lineTo(bubbleRect.right - cornerRadius, bubbleRect.bottom)
            } else {
                bubblePath.moveTo(bubbleRect.left + cornerRadius, bubbleRect.bottom)
                bubblePath.lineTo(bubbleRect.left, bubbleRect.bottom + tailSize)
                bubblePath.lineTo(bubbleRect.left + cornerRadius, bubbleRect.bottom)
            }
        }

        canvas.drawPath(bubblePath, paint)

        // Draw text
        val textX = bubbleRect.left + bubblePadding
        val textY = bubbleRect.top + bubblePadding + textPaint.textSize
        canvas.drawText(messageText, textX, textY, textPaint)

        // Draw time
        if (timeText.isNotEmpty()) {
            val timeX = bubbleRect.right - bubblePadding - timePaint.measureText(timeText)
            val timeY = bubbleRect.bottom - bubblePadding
            canvas.drawText(timeText, timeX, timeY, timePaint)
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
