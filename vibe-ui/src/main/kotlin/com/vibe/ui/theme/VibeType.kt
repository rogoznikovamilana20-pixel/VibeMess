package com.vibe.ui.theme

import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView

/**
 * Vibe typography system.
 */
object VibeType {
    const val FONT_FAMILY = "sans-serif"

    fun applyCaption(textView: TextView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, VibeDimens.TEXT_SIZE_CAPTION.toFloat())
        textView.typeface = Typeface.create(FONT_FAMILY, Typeface.NORMAL)
    }

    fun applyBodySmall(textView: TextView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, VibeDimens.TEXT_SIZE_BODY_SM.toFloat())
        textView.typeface = Typeface.create(FONT_FAMILY, Typeface.NORMAL)
    }

    fun applyBody(textView: TextView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, VibeDimens.TEXT_SIZE_BODY.toFloat())
        textView.typeface = Typeface.create(FONT_FAMILY, Typeface.NORMAL)
    }

    fun applyBodyBold(textView: TextView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, VibeDimens.TEXT_SIZE_BODY.toFloat())
        textView.typeface = Typeface.create(FONT_FAMILY, Typeface.BOLD)
    }

    fun applySubtitle(textView: TextView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, VibeDimens.TEXT_SIZE_SUBTITLE.toFloat())
        textView.typeface = Typeface.create(FONT_FAMILY, Typeface.BOLD)
    }

    fun applyTitle(textView: TextView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, VibeDimens.TEXT_SIZE_TITLE.toFloat())
        textView.typeface = Typeface.create(FONT_FAMILY, Typeface.BOLD)
    }

    fun applyHeadline(textView: TextView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, VibeDimens.TEXT_SIZE_HEADLINE.toFloat())
        textView.typeface = Typeface.create(FONT_FAMILY, Typeface.BOLD)
    }
}
