package com.vibe.ui.compose.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class Screen {
    SPLASH,
    WELCOME,
    AUTH,
    REGISTER,
    TOUR,
    ONBOARDING_INTERESTS,
    ONBOARDING_MODE,
    ONBOARDING_PROFILE,
    MAIN,
    SEARCH,
    CHAT,
    PROFILE,
    CONTACTS,
    TIMELINE,
    MARKETPLACE,
    ACHIEVEMENTS,
    SETTINGS,
    SETTINGS_EDIT_PROFILE,
    SETTINGS_PRIVACY,
    SETTINGS_NOTIFICATIONS,
    SETTINGS_THEME,
    SETTINGS_LANGUAGE,
    SETTINGS_STORAGE,
    SETTINGS_CALLS,
    SETTINGS_ABOUT,
    CHANNELS,
    CREATE_CHAT,
    CHANNEL_ADMIN,
    BOTS,
    BOT_CREATE,
    BOT_CHAT,
    BOT_ADMIN,
    VIBE_PLUS,
    SPARKS,
    PAYMENT_FLOW,
    MESH,
    CALL_CONTACTS,
    CALL_AUDIO,
    CALL_VIDEO
}

class VibeNavigationState(startScreen: Screen = Screen.SPLASH) {
    var currentScreen by mutableStateOf(startScreen)
        private set
    private val backStack = mutableListOf<Screen>()

    val canGoBack: Boolean get() = backStack.isNotEmpty()

    fun navigateTo(screen: Screen) {
        backStack.add(currentScreen)
        currentScreen = screen
    }

    fun goBack(): Boolean {
        if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeAt(backStack.size - 1)
            return true
        }
        return false
    }

    fun popToRoot() {
        backStack.clear()
        currentScreen = Screen.MAIN
    }

    fun replaceWith(screen: Screen) {
        backStack.clear()
        currentScreen = screen
    }
}

@Composable
fun rememberVibeNavigationState(startScreen: Screen = Screen.SPLASH): VibeNavigationState {
    return remember(startScreen) { VibeNavigationState(startScreen) }
}

@Composable
fun VibeNavHost(
    navState: VibeNavigationState,
    screenContent: @Composable (Screen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // NOTE: screens are rendered directly instead of AnimatedContent.
        // material3 1.2.x Scaffold (ScaffoldLayoutWithMeasureFix) corrupts the
        // Compose slot table (ArrayIndexOutOfBoundsException: length=0; index=-5)
        // when two Scaffold screens compose simultaneously during a transition.
        screenContent(navState.currentScreen)
    }
}
