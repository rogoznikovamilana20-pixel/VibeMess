package com.vibe.ui.compose.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
    ONBOARDING_INTERESTS,
    ONBOARDING_MODE,
    ONBOARDING_PROFILE,
    MAIN,
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
            currentScreen = backStack.removeLast()
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
        AnimatedContent(
            targetState = navState.currentScreen,
            transitionSpec = {
            when {
                targetState.ordinal > initialState.ordinal -> {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
                }
                else -> {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it / 3 }
                }
            }
        },
        label = "VibeNav"
    ) { screen ->
            screenContent(screen)
        }
    }
}
