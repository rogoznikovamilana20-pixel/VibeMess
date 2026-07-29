package com.vibe.ui.theme

import android.graphics.Color

/**
 * Vibe color palette - Design System 1.0
 * Based on official design specification
 */
object VibeColors {
    // Brand - Primary palette
    val Primary = Color.parseColor("#8D2BFA")      // Main Vibe purple
    val PrimaryDark = Color.parseColor("#6B1FCC")   // Darker purple
    val PrimaryLight = Color.parseColor("#B06BFF")  // Lighter purple
    val PrimarySurface = Color.parseColor("#1A0F2E") // Purple surface

    // Accent colors
    val AccentBlue = Color.parseColor("#10B6FA")     // Blue accent
    val AccentPink = Color.parseColor("#EC4899")     // Pink accent
    val AccentGold = Color.parseColor("#F59E0B")     // Gold/Orange accent
    val AccentGreen = Color.parseColor("#4ADE80")    // Green accent

    // Background - Dark theme
    val Background = Color.parseColor("#0C0B1A")     // Main background
    val BackgroundAlt = Color.parseColor("#100E1F")  // Alternative background
    val Surface = Color.parseColor("#161429")        // Card surface
    val SurfaceVariant = Color.parseColor("#1E1B33") // Variant surface
    val SurfaceHighlight = Color.parseColor("#2A2645") // Highlighted surface

    // Text
    val TextPrimary = Color.parseColor("#FFFFFF")    // Primary text
    val TextSecondary = Color.parseColor("#A8A3B8")  // Secondary text
    val TextTertiary = Color.parseColor("#6B6580")   // Tertiary text
    val TextInverse = Color.parseColor("#0C0B1A")   // Inverse text

    // Status colors
    val Success = Color.parseColor("#4ADE80")        // Success green
    val Warning = Color.parseColor("#F59E0B")        // Warning gold
    val Error = Color.parseColor("#EF4444")          // Error red
    val Info = Color.parseColor("#10B6FA")           // Info blue

    // Chat bubbles
    val MessageOut = Color.parseColor("#8D2BFA")     // Outgoing message
    val MessageIn = Color.parseColor("#1E1B33")      // Incoming message
    val MessageOutText = Color.parseColor("#FFFFFF") // Outgoing text
    val MessageInText = Color.parseColor("#FFFFFF")  // Incoming text

    // Aurion AI
    val Aurion = Color.parseColor("#8D2BFA")         // Aurion purple
    val AurionGlow = Color.parseColor("#1A0F2E")     // Aurion glow

    // Vibe Styles
    val StyleDefault = Color.parseColor("#8D2BFA")   // Default style
    val StyleNeon = Color.parseColor("#10B6FA")      // Neon style
    val StyleOcean = Color.parseColor("#0EA5E9")     // Ocean style
    val StyleForest = Color.parseColor("#22C55E")    // Forest style
    val StyleSunset = Color.parseColor("#F97316")    // Sunset style

    // Reputation
    val ReputationLow = Color.parseColor("#6B6580")  // Low reputation
    val ReputationMid = Color.parseColor("#F59E0B")  // Mid reputation
    val ReputationHigh = Color.parseColor("#8D2BFA") // High reputation

    // Искра (currency)
    val Spark = Color.parseColor("#F59E0B")          // Искра gold
    val SparkGlow = Color.parseColor("#FDE68A")      // Искра glow

    // Divider
    val Divider = Color.parseColor("#2A2645")        // Divider line

    // Ripple
    val Ripple = Color.parseColor("#20FFFFFF")       // Ripple effect

    // Online status
    val Online = Color.parseColor("#4ADE80")         // Online green
    val Offline = Color.parseColor("#6B6580")        // Offline gray

    // Work mode accent
    val WorkAccent = Color.parseColor("#10B6FA")     // Work mode blue

    // Personal mode accent
    val PersonalAccent = Color.parseColor("#EC4899") // Personal mode pink
}
