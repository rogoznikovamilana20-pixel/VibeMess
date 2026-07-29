package com.vibe.ui.theme

import android.content.res.Resources

/**
 * Vibe dimension constants.
 */
object VibeDimens {
    // Spacing
    const val SPACE_XS = 4
    const val SPACE_SM = 8
    const val SPACE_MD = 12
    const val SPACE_LG = 16
    const val SPACE_XL = 24
    const val SPACE_XXL = 32

    // Corner radius
    const val RADIUS_SM = 8
    const val RADIUS_MD = 12
    const val RADIUS_LG = 16
    const val RADIUS_XL = 24
    const val RADIUS_ROUND = 999

    // Avatar
    const val AVATAR_SIZE_SM = 32
    const val AVATAR_SIZE_MD = 44
    const val AVATAR_SIZE_LG = 56
    const val AVATAR_SIZE_XL = 80

    // Text
    const val TEXT_SIZE_CAPTION = 11
    const val TEXT_SIZE_BODY_SM = 13
    const val TEXT_SIZE_BODY = 15
    const val TEXT_SIZE_SUBTITLE = 17
    const val TEXT_SIZE_TITLE = 20
    const val TEXT_SIZE_HEADLINE = 24

    // Toolbar
    const val TOOLBAR_HEIGHT = 56

    // Bottom Nav
    const val BOTTOM_NAV_HEIGHT = 56

    // Input Bar
    const val INPUT_BAR_MIN_HEIGHT = 48

    // Helpers
    fun dp(value: Int): Int = (value * Resources.getSystem().displayMetrics.density).toInt()
}
