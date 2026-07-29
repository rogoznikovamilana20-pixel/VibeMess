package com.vibe.ui.compose

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vibe.ui.call.CallUtils
import com.vibe.ui.call.VibeCallService
import com.vibe.ui.compose.navigation.Screen
import com.vibe.ui.compose.navigation.VibeNavHost
import com.vibe.ui.compose.navigation.rememberVibeNavigationState
import com.vibe.ui.compose.screens.AchievementsScreen
import com.vibe.ui.compose.screens.AuthScreen
import com.vibe.ui.compose.screens.CallScreen
import com.vibe.ui.compose.screens.ChatScreen
import com.vibe.ui.compose.screens.ContactsScreen
import com.vibe.ui.compose.screens.MainScreen
import com.vibe.ui.compose.screens.MarketplaceScreen
import com.vibe.ui.compose.screens.OnboardingScreen
import com.vibe.ui.compose.screens.ProfileScreen
import com.vibe.ui.compose.screens.SettingsAboutScreen
import com.vibe.ui.compose.screens.SettingsCallsScreen
import com.vibe.ui.compose.screens.SettingsEditProfileScreen
import com.vibe.ui.compose.screens.SettingsLanguageScreen
import com.vibe.ui.compose.screens.SettingsNotificationsScreen
import com.vibe.ui.compose.screens.SettingsPrivacyScreen
import com.vibe.ui.compose.screens.SettingsScreen
import com.vibe.ui.compose.screens.SettingsStorageScreen
import com.vibe.ui.compose.screens.SettingsThemeScreen
import com.vibe.ui.compose.screens.SplashScreen
import com.vibe.ui.compose.screens.TimelineScreen
import com.vibe.ui.compose.screens.WelcomeScreen
import com.vibe.ui.compose.theme.VibeTheme
import com.vibe.ui.data.ThemeManager
import com.vibe.ui.di.VibeContainer
import com.vibe.ui.network.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VibeApp() {
    val context = LocalContext.current
    val serverConfig = remember { ServerConfig(context) }
    val themeManager = remember { ThemeManager(context) }
    var isDarkTheme by remember { mutableStateOf(themeManager.isDarkTheme) }
    var incomingCallId by remember { mutableStateOf<String?>(null) }
    var incomingRoomId by remember { mutableStateOf<String?>(null) }
    var telegramUserId by remember { mutableStateOf<String?>(null) }
    var bridgeReady by remember { mutableStateOf(false) }
    var currentChatId by remember { mutableStateOf<Long?>(null) }
    var currentChatName by remember { mutableStateOf("") }

    val buildKey = com.vibe.ui.BuildConfig.AI_API_KEY
    if (buildKey.isNotBlank() && serverConfig.getAiApiKey().isBlank()) {
        serverConfig.setAiApiKey(buildKey)
        if (serverConfig.getAiProvider().isBlank()) {
            serverConfig.setAiProvider("openrouter")
        }
    }

    val isAuthenticated = serverConfig.isAuthenticated() || telegramUserId != null
    val startScreen = if (isAuthenticated) Screen.MAIN else Screen.WELCOME
    val navState = rememberVibeNavigationState(startScreen = startScreen)

    val userId = telegramUserId ?: run {
        val saved = CallUtils.getUserIdFromPrefs(context)
        if (saved.isNotEmpty()) saved else CallUtils.getUserId(context)
    }

    LaunchedEffect(Unit) {
        if (VibeContainer.isInitialized()) {
            try {
                val account = withContext(Dispatchers.IO) {
                    VibeContainer.getGateway().accounts.getCurrentAccount()
                }
                val tid = account.userId.toString()
                telegramUserId = tid
                serverConfig.setUserId(tid)
                serverConfig.setAuthenticated(true)
                bridgeReady = true
            } catch (e: Exception) {
                android.util.Log.w("VibeApp", "Failed to load Telegram account", e)
                bridgeReady = VibeContainer.isInitialized()
            }
        }
    }

    fun resolveUserId(): String {
        return try {
            if (VibeContainer.isInitialized()) {
                val account = VibeContainer.getGateway().accounts.getCurrentAccount()
                account.userId.toString()
            } else {
                CallUtils.getUserIdFromPrefs(context).ifEmpty { CallUtils.getUserId(context) }
            }
        } catch (e: Exception) {
            android.util.Log.w("VibeApp", "resolveUserId failed", e)
            CallUtils.getUserIdFromPrefs(context).ifEmpty { CallUtils.getUserId(context) }
        }
    }

    // Handle incoming call intent
    val intent = (context as? android.app.Activity)?.intent
    if (intent?.hasExtra("call_action") == true) {
        val action = intent.getStringExtra("call_action")
        if (action == "incoming_call") {
            incomingCallId = intent.getStringExtra("caller_id")
            incomingRoomId = intent.getStringExtra("room_id")
        }
    }

    VibeTheme(darkTheme = isDarkTheme) {
        VibeNavHost(navState = navState) { screen ->
            when (screen) {
                Screen.SPLASH -> SplashScreen(
                    onSplashComplete = {
                        navState.replaceWith(
                            if (isAuthenticated) Screen.MAIN else Screen.WELCOME
                        )
                    }
                )

                Screen.WELCOME -> WelcomeScreen(
                    onRegister = { navState.navigateTo(Screen.AUTH) },
                    onLogin = { navState.navigateTo(Screen.AUTH) }
                )

                Screen.AUTH -> AuthScreen(
                    onBack = { navState.goBack() },
                    onComplete = {
                        val uid = resolveUserId()
                        serverConfig.setUserId(uid)
                        serverConfig.setAuthenticated(true)
                        telegramUserId = uid
                        navState.replaceWith(Screen.MAIN)
                    }
                )

                Screen.ONBOARDING_INTERESTS -> OnboardingScreen(
                    onComplete = {
                        val uid = resolveUserId()
                        serverConfig.setUserId(uid)
                        serverConfig.setAuthenticated(true)
                        telegramUserId = uid
                        navState.replaceWith(Screen.MAIN)
                    }
                )
                Screen.ONBOARDING_MODE -> OnboardingScreen(
                    onComplete = {
                        val uid = resolveUserId()
                        serverConfig.setUserId(uid)
                        serverConfig.setAuthenticated(true)
                        telegramUserId = uid
                        navState.replaceWith(Screen.MAIN)
                    }
                )
                Screen.ONBOARDING_PROFILE -> OnboardingScreen(
                    onComplete = {
                        val uid = resolveUserId()
                        serverConfig.setUserId(uid)
                        serverConfig.setAuthenticated(true)
                        telegramUserId = uid
                        navState.replaceWith(Screen.MAIN)
                    }
                )

                Screen.MAIN -> MainScreen(
                    onOpenChat = { chatId, chatName ->
                        currentChatId = chatId
                        currentChatName = chatName
                        navState.navigateTo(Screen.CHAT)
                    },
                    onOpenProfile = { navState.navigateTo(Screen.PROFILE) },
                    onOpenSettings = { navState.navigateTo(Screen.SETTINGS) },
                    onOpenContacts = { navState.navigateTo(Screen.CONTACTS) },
                    onOpenTimeline = { navState.navigateTo(Screen.TIMELINE) },
                    onOpenMarketplace = { navState.navigateTo(Screen.MARKETPLACE) },
                    onOpenAchievements = { navState.navigateTo(Screen.ACHIEVEMENTS) }
                )

                Screen.CHAT -> currentChatId?.let { chatId ->
                    ChatScreen(
                        chatId = chatId,
                        chatName = currentChatName,
                        onBack = { navState.goBack() },
                        onOpenCall = { isVideo ->
                            navState.navigateTo(if (isVideo) Screen.CALL_VIDEO else Screen.CALL_AUDIO)
                        }
                    )
                }



                Screen.PROFILE -> ProfileScreen(
                    onBack = { navState.goBack() },
                    vibeId = userId
                )

                Screen.CONTACTS -> ContactsScreen(
                    onBack = { navState.goBack() },
                    onCallVibe = { contactUserId ->
                        incomingCallId = contactUserId
                        incomingRoomId = null
                        navState.navigateTo(Screen.CALL_AUDIO)
                    }
                )

                Screen.TIMELINE -> TimelineScreen(onBack = { navState.goBack() })
                Screen.MARKETPLACE -> MarketplaceScreen(onBack = { navState.goBack() })
                Screen.ACHIEVEMENTS -> AchievementsScreen(onBack = { navState.goBack() })

                Screen.SETTINGS -> SettingsScreen(
                    onBack = { navState.goBack() },
                    onEditProfile = { navState.navigateTo(Screen.SETTINGS_EDIT_PROFILE) },
                    onPrivacy = { navState.navigateTo(Screen.SETTINGS_PRIVACY) },
                    onNotifications = { navState.navigateTo(Screen.SETTINGS_NOTIFICATIONS) },
                    onTheme = { navState.navigateTo(Screen.SETTINGS_THEME) },
                    onLanguage = { navState.navigateTo(Screen.SETTINGS_LANGUAGE) },
                    onStorage = { navState.navigateTo(Screen.SETTINGS_STORAGE) },
                    onCalls = { navState.navigateTo(Screen.SETTINGS_CALLS) },
                    onAbout = { navState.navigateTo(Screen.SETTINGS_ABOUT) }
                )

                Screen.SETTINGS_EDIT_PROFILE -> SettingsEditProfileScreen(onBack = { navState.goBack() })
                Screen.SETTINGS_PRIVACY -> SettingsPrivacyScreen(onBack = { navState.goBack() })
                Screen.SETTINGS_NOTIFICATIONS -> SettingsNotificationsScreen(onBack = { navState.goBack() })
                Screen.SETTINGS_THEME -> SettingsThemeScreen(
                    onBack = { navState.goBack() },
                    onThemeChanged = { isDarkTheme = it }
                )
                Screen.SETTINGS_LANGUAGE -> SettingsLanguageScreen(onBack = { navState.goBack() })
                Screen.SETTINGS_STORAGE -> SettingsStorageScreen(onBack = { navState.goBack() })
                Screen.SETTINGS_CALLS -> SettingsCallsScreen(onBack = { navState.goBack() })
                Screen.SETTINGS_ABOUT -> SettingsAboutScreen(onBack = { navState.goBack() })

                Screen.CALL_AUDIO -> CallScreen(
                    isVideoCall = false,
                    contactName = currentChatName.ifEmpty { "Анна Смирнова" },
                    contactUserId = incomingCallId,
                    incomingRoomId = incomingRoomId,
                    onEndCall = {
                        incomingCallId = null
                        incomingRoomId = null
                        navState.goBack()
                    }
                )

                Screen.CALL_VIDEO -> CallScreen(
                    isVideoCall = true,
                    contactName = currentChatName.ifEmpty { "Анна Смирнова" },
                    contactUserId = incomingCallId,
                    incomingRoomId = incomingRoomId,
                    onEndCall = {
                        incomingCallId = null
                        incomingRoomId = null
                        navState.goBack()
                    }
                )
            }
        }
    }
}
