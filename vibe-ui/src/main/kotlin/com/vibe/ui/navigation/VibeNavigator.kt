package com.vibe.ui.navigation

import com.vibe.bridge.model.VibeChat

/**
 * Navigation events for Vibe screens.
 */
sealed class VibeScreen {
    data object ChatList : VibeScreen()
    data class ChatView(val chat: VibeChat) : VibeScreen()
    data object Profile : VibeScreen()
    data object Settings : VibeScreen()
}

/**
 * Centralized navigation controller for Vibe UI.
 */
class VibeNavigator {
    private var currentScreen: VibeScreen = VibeScreen.ChatList
    private val listeners = mutableListOf<(VibeScreen) -> Unit>()

    fun navigate(screen: VibeScreen) {
        currentScreen = screen
        listeners.forEach { it(screen) }
    }

    fun addListener(listener: (VibeScreen) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (VibeScreen) -> Unit) {
        listeners.remove(listener)
    }

    fun getCurrentScreen(): VibeScreen = currentScreen
}
