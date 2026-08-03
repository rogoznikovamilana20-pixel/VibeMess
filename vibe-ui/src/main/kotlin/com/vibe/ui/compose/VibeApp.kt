package com.vibe.ui.compose

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.vibe.ui.compose.screens.BotAdminScreen
import com.vibe.ui.compose.screens.BotCatalogScreen
import com.vibe.ui.compose.screens.BotChatScreen
import com.vibe.ui.compose.screens.CallScreen
import com.vibe.ui.compose.screens.ChannelAdminScreen
import com.vibe.ui.compose.screens.ChannelsScreen
import com.vibe.ui.compose.screens.ChatScreen
import com.vibe.ui.compose.screens.ContactsScreen
import com.vibe.ui.compose.screens.CreateBotScreen
import com.vibe.ui.compose.screens.CreateChatScreen
import com.vibe.ui.compose.screens.GuidedTourScreen
import com.vibe.ui.compose.screens.PaymentFlowScreen
import com.vibe.ui.compose.screens.SparksScreen
import com.vibe.ui.compose.screens.VibePlusScreen
import com.vibe.ui.compose.screens.MainScreen
import com.vibe.ui.compose.screens.MarketplaceScreen
import com.vibe.ui.compose.screens.MeshScreen
import com.vibe.ui.compose.screens.OnboardingScreen
import com.vibe.ui.compose.screens.OnboardingScreenNew
import com.vibe.ui.compose.screens.ProfileScreen
import com.vibe.ui.compose.screens.RegisterScreen
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
import com.vibe.ui.compose.screens.SearchMessagesScreen
import com.vibe.ui.compose.screens.SupabaseChatScreen
import com.vibe.ui.compose.screens.SupabaseMainScreen
import com.vibe.ui.compose.screens.SupabaseSearchScreen
import com.vibe.ui.compose.screens.SecuritySettingsScreen
import com.vibe.ui.compose.screens.AdminScreen
import com.vibe.ui.compose.screens.TimelineScreen
import com.vibe.ui.compose.screens.WelcomeScreen
import com.vibe.ui.compose.theme.VibeTheme
import com.vibe.ui.ai.AurionManager
import com.vibe.ui.data.ThemeManager
import com.vibe.ui.di.VibeContainer
import com.vibe.ui.i18n.VibeI18n
import com.vibe.ui.network.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VibeApp() {
    val context = LocalContext.current

    // Global crash handler — prevents crash loops
    LaunchedEffect(Unit) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("VibeApp", "Uncaught exception in ${thread.name}", throwable)
            // Report to Crashlytics if available
            try {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                    .recordException(throwable)
            } catch (_: Exception) {}
            // Let the default handler show the crash dialog
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    val serverConfig = remember { ServerConfig(context) }
    val themeManager = remember { ThemeManager(context) }
    // Observe language changes to trigger recomposition
    @Suppress("UNUSED_VARIABLE")
    val _langObserver = com.vibe.ui.i18n.VibeI18n.currentCode
    com.vibe.ui.i18n.VibeI18n.setLanguage(serverConfig.getAppLanguageCode())
    var isDarkTheme by remember { mutableStateOf(themeManager.isDarkTheme) }
    var incomingCallId by remember { mutableStateOf<String?>(null) }
    var incomingRoomId by remember { mutableStateOf<String?>(null) }
    var callContactId by remember { mutableStateOf<String?>(null) }
    var callContactName by remember { mutableStateOf("") }
    var currentAdminChatId by remember { mutableStateOf<Long?>(null) }
    var telegramUserId by remember { mutableStateOf<String?>(null) }
    var bridgeReady by remember { mutableStateOf(false) }
    var currentChatId by remember { mutableStateOf<Long?>(null) }
    var currentChatName by remember { mutableStateOf("") }
    var currentScrollToMessageId by remember { mutableStateOf<Long?>(null) }
    var currentBotId by remember { mutableStateOf<Long?>(null) }
    var currentBotName by remember { mutableStateOf("") }
    var currentPaymentItem by remember { mutableStateOf("") }
    var supabaseChatId by remember { mutableStateOf<String?>(null) }
    var supabaseChatTitle by remember { mutableStateOf("") }

    val buildKey = com.vibe.ui.BuildConfig.AI_API_KEY
    if (buildKey.isNotBlank() && serverConfig.getAiApiKey().isBlank()) {
        serverConfig.setAiApiKey(buildKey)
    }
    if (serverConfig.getAiProvider().isBlank()) {
        serverConfig.setAiProvider("zvenoai")
    }
    val aiKey = serverConfig.getAiApiKey()
    if (aiKey.isNotBlank()) {
        AurionManager.updateApiKey(aiKey)
    }

    if (!VibeContainer.isInitialized()) {
        try {
            VibeContainer.initialize()
        } catch (e: Exception) {
            android.util.Log.e("VibeApp", "VibeContainer init failed", e)
        }
    }

    val isAuthenticated = serverConfig.isAuthenticated()
    val tourCompleted = serverConfig.isTourCompleted()
    val startScreen = when {
        !isAuthenticated -> Screen.WELCOME
        !tourCompleted -> Screen.TOUR
        else -> Screen.SUPABASE_MAIN
    }
    val navState = rememberVibeNavigationState(startScreen = startScreen)

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val userId = telegramUserId ?: run {
        val saved = CallUtils.getUserIdFromPrefs(context)
        if (saved.isNotEmpty()) saved else CallUtils.getUserId(context)
    }

    // Online status: set online when composable is active, offline when disposed
    val onlineScope = rememberCoroutineScope()

    // Refresh session on startup
    LaunchedEffect(Unit) {
        if (serverConfig.isAuthenticated()) {
            serverConfig.refreshSessionIfNeeded()
        }
        // Initialize E2EE engine
        try {
            com.vibe.ui.e2e.E2EEngine.init(context)
        } catch (e: Exception) {
            android.util.Log.e("VibeApp", "E2EEngine init failed", e)
        }
        try {
            com.vibe.ui.focus.FocusModeManager.init(context)
        } catch (e: Exception) {
            android.util.Log.e("VibeApp", "FocusModeManager init failed", e)
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        if (serverConfig.isAuthenticated()) {
            onlineScope.launch(Dispatchers.IO) {
                com.vibe.ui.network.SupabaseClient.setOnline(
                    com.vibe.ui.BuildConfig.SUPABASE_URL,
                    com.vibe.ui.BuildConfig.SUPABASE_ANON_KEY,
                    serverConfig.getAuthToken(),
                    true
                )
            }
        }
        onDispose {
            if (serverConfig.isAuthenticated()) {
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    com.vibe.ui.network.SupabaseClient.setOnline(
                        com.vibe.ui.BuildConfig.SUPABASE_URL,
                        com.vibe.ui.BuildConfig.SUPABASE_ANON_KEY,
                        serverConfig.getAuthToken(),
                        false
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!VibeContainer.isInitialized()) {
            try {
                VibeContainer.bindContext(context)
                VibeContainer.initialize()
            } catch (e: Exception) {
                android.util.Log.e("VibeApp", "VibeContainer init in LaunchedEffect failed", e)
            }
        }
        try {
            com.vibe.ui.data.bot.BotService.start(context.applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("VibeApp", "BotService start failed", e)
        }
        try {
            com.vibe.ui.data.sync.ChatSyncService.start()
        } catch (e: Exception) {
            android.util.Log.e("VibeApp", "ChatSyncService start failed", e)
        }
        if (VibeContainer.isInitialized()) {
            try {
                val account = withContext(Dispatchers.IO) {
                    VibeContainer.getGateway().accounts.getCurrentAccount()
                }
                if (account.userId > 0L) {
                    val tid = account.userId.toString()
                    telegramUserId = tid
                    CallUtils.setUserId(context, tid)
                    serverConfig.setUserId(tid)
                    serverConfig.setAuthenticated(true)
                    bridgeReady = true
                    VibeCallService.start(context.applicationContext, tid)
                } else {
                    // No real Telegram session — the local Vibe account is the identity.
                    // Keep the stored authentication; never kick the user out of MAIN.
                    bridgeReady = true
                    val localId = serverConfig.getUserId().ifBlank { serverConfig.getVibeId() }
                    if (localId.isNotBlank()) {
                        CallUtils.setUserId(context, localId)
                        VibeCallService.start(context.applicationContext, localId)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("VibeApp", "Failed to load Telegram account", e)
                bridgeReady = VibeContainer.isInitialized()
            }
        }
    }

    fun resolveUserId(): String {
        val local = serverConfig.getUserId().ifBlank { serverConfig.getVibeId() }
        return try {
            if (VibeContainer.isInitialized()) {
                val account = VibeContainer.getGateway().accounts.getCurrentAccount()
                if (account.userId > 0L) {
                    val uid = account.userId.toString()
                    CallUtils.setUserId(context, uid)
                    return uid
                }
            }
            if (local.isNotBlank()) local
            else CallUtils.getUserIdFromPrefs(context).ifEmpty { CallUtils.getUserId(context) }
        } catch (e: Exception) {
            android.util.Log.w("VibeApp", "resolveUserId failed", e)
            local.ifBlank { CallUtils.getUserIdFromPrefs(context).ifEmpty { CallUtils.getUserId(context) } }
        }
    }

    // Handle incoming call intent
    val intent = (context as? android.app.Activity)?.intent
    if (intent?.hasExtra("call_action") == true) {
        val action = intent.getStringExtra("call_action")
        if (action == "incoming_call" || action == "answer") {
            incomingCallId = intent.getStringExtra("caller_id")
            incomingRoomId = intent.getStringExtra("room_id")
        }
    }

    LaunchedEffect(incomingRoomId) {
        if (incomingRoomId != null &&
            navState.currentScreen != Screen.CALL_AUDIO &&
            navState.currentScreen != Screen.CALL_VIDEO
        ) {
            navState.navigateTo(Screen.CALL_AUDIO)
        }
    }

    VibeTheme(darkTheme = isDarkTheme) {
        VibeNavHost(navState = navState) { screen ->
            when (screen) {
                Screen.SPLASH -> SplashScreen(
                    onSplashComplete = {
                        navState.replaceWith(
                            if (isAuthenticated) Screen.SUPABASE_MAIN else Screen.WELCOME
                        )
                    }
                )

                Screen.WELCOME -> WelcomeScreen(
                    onRegister = { navState.navigateTo(Screen.REGISTER) },
                    onLogin = { navState.navigateTo(Screen.AUTH) }
                )

                Screen.REGISTER -> RegisterScreen(
                    onBack = { navState.goBack() },
                    onComplete = { vibeId ->
                        serverConfig.setVibeId(vibeId)
                        serverConfig.setAuthenticated(true)
                        serverConfig.setTourCompleted(false)
                        navState.replaceWith(Screen.ONBOARDING_NEW)
                    }
                )

                Screen.AUTH -> AuthScreen(
                    onBack = { navState.goBack() },
                    onComplete = {
                        val uid = resolveUserId()
                        serverConfig.setUserId(uid)
                        serverConfig.setAuthenticated(true)
                        telegramUserId = uid
                        navState.replaceWith(
                            if (serverConfig.isTourCompleted()) Screen.MAIN else Screen.TOUR
                        )
                    }
                )

                Screen.TOUR -> GuidedTourScreen(
                    onComplete = {
                        serverConfig.setTourCompleted(true)
                        navState.replaceWith(Screen.SUPABASE_MAIN)
                    }
                )

                Screen.ONBOARDING_NEW -> OnboardingScreenNew(
                    onComplete = {
                        serverConfig.setTourCompleted(true)
                        navState.replaceWith(Screen.SUPABASE_MAIN)
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
                        currentScrollToMessageId = null
                        navState.navigateTo(Screen.CHAT)
                    },
                    onOpenSearch = { navState.navigateTo(Screen.SEARCH) },
                    onOpenProfile = { navState.navigateTo(Screen.PROFILE) },
                    onOpenSettings = { navState.navigateTo(Screen.SETTINGS) },
                    onOpenContacts = { navState.navigateTo(Screen.CONTACTS) },
                    onOpenTimeline = { navState.navigateTo(Screen.TIMELINE) },
                    onOpenMarketplace = { navState.navigateTo(Screen.MARKETPLACE) },
                    onOpenAchievements = { navState.navigateTo(Screen.ACHIEVEMENTS) },
                    onOpenCalls = {
                        incomingCallId = null
                        incomingRoomId = null
                        callContactId = null
                        callContactName = ""
                        navState.navigateTo(Screen.CALL_CONTACTS)
                    },
                    onOpenFavorites = {
                        val ownId = (telegramUserId ?: resolveUserId()).toLongOrNull() ?: 0L
                        if (ownId > 0L) {
                            currentChatId = ownId
                            currentChatName = "Избранное"
                            currentScrollToMessageId = null
                            navState.navigateTo(Screen.CHAT)
                        } else {
                            navState.navigateTo(Screen.CONTACTS)
                        }
                    },
                    onOpenGroups = { navState.navigateTo(Screen.CHANNELS) },
                    onOpenBots = { navState.navigateTo(Screen.BOTS) },
                    onOpenCreateChat = { navState.navigateTo(Screen.CONTACTS) }
                )

                Screen.SEARCH -> SearchMessagesScreen(
                    onBack = {
                        currentScrollToMessageId = null
                        navState.goBack()
                    },
                    onOpenChat = { chatId, chatName ->
                        currentChatId = chatId
                        currentChatName = chatName
                        currentScrollToMessageId = null
                        navState.navigateTo(Screen.CHAT)
                    },
                    onOpenMessage = { chatId, chatName, messageId ->
                        currentChatId = chatId
                        currentChatName = chatName
                        currentScrollToMessageId = messageId
                        navState.navigateTo(Screen.CHAT)
                    }
                )

                Screen.CHAT -> {
                    val chatId = currentChatId
                    if (chatId != null) {
                        ChatScreen(
                            chatId = chatId,
                            chatName = currentChatName,
                            scrollToMessageId = currentScrollToMessageId,
                            onBack = {
                                currentScrollToMessageId = null
                                navState.goBack()
                            },
                            onOpenCall = { isVideo ->
                                callContactId = currentChatId?.toString()
                                callContactName = currentChatName
                                incomingCallId = null
                                incomingRoomId = null
                                navState.navigateTo(if (isVideo) Screen.CALL_VIDEO else Screen.CALL_AUDIO)
                            }
                        )
                    } else {
                        androidx.compose.material3.Text("Ошибка: чат не выбран")
                    }
                }



                Screen.PROFILE -> ProfileScreen(
                    onBack = { navState.goBack() },
                    vibeId = userId,
                    onEditProfile = { navState.navigateTo(Screen.SETTINGS_EDIT_PROFILE) },
                    onOpenVibePlus = { navState.navigateTo(Screen.VIBE_PLUS) },
                    onOpenSparks = { navState.navigateTo(Screen.SPARKS) }
                )

                Screen.CONTACTS -> ContactsScreen(
                    onBack = { navState.goBack() },
                    onOpenChat = { userId, userName ->
                        currentChatId = userId
                        currentChatName = userName
                        currentScrollToMessageId = null
                        navState.navigateTo(Screen.CHAT)
                    },
                    onCallVibe = { contactUserId, contactName, isVideo ->
                        callContactId = contactUserId
                        callContactName = contactName
                        incomingCallId = null
                        incomingRoomId = null
                        navState.navigateTo(if (isVideo) Screen.CALL_VIDEO else Screen.CALL_AUDIO)
                    }
                )

                Screen.CALL_CONTACTS -> ContactsScreen(
                    onBack = { navState.goBack() },
                    onCallVibe = { contactUserId, contactName, isVideo ->
                        callContactId = contactUserId
                        callContactName = contactName
                        incomingCallId = null
                        incomingRoomId = null
                        navState.navigateTo(if (isVideo) Screen.CALL_VIDEO else Screen.CALL_AUDIO)
                    },
                    title = "Выберите контакт"
                )

                Screen.CHANNELS -> ChannelsScreen(
                    onBack = { navState.goBack() },
                    onOpenChat = { chatId, chatName ->
                        currentChatId = chatId
                        currentChatName = chatName
                        currentScrollToMessageId = null
                        navState.navigateTo(Screen.CHAT)
                    },
                    onOpenAdmin = { adminChatId ->
                        currentAdminChatId = adminChatId
                        navState.navigateTo(Screen.CHANNEL_ADMIN)
                    },
                    onOpenCreateChat = { navState.navigateTo(Screen.CREATE_CHAT) }
                )

                Screen.CREATE_CHAT -> CreateChatScreen(onBack = { navState.goBack() })

                Screen.BOTS -> BotCatalogScreen(
                    onBack = { navState.goBack() },
                    onOpenBot = { botId, botName ->
                        currentBotId = botId
                        currentBotName = botName
                        navState.navigateTo(Screen.BOT_CHAT)
                    },
                    onAdmin = { botId ->
                        currentBotId = botId
                        navState.navigateTo(Screen.BOT_ADMIN)
                    },
                    onCreateBot = { navState.navigateTo(Screen.BOT_CREATE) }
                )

                Screen.BOT_CREATE -> CreateBotScreen(
                    onBack = { navState.goBack() },
                    onCreated = { navState.goBack() }
                )

                Screen.BOT_CHAT -> {
                    val botId = currentBotId
                    if (botId != null) {
                        BotChatScreen(
                            botId = botId,
                            botName = currentBotName,
                            onBack = { navState.goBack() }
                        )
                    } else {
                        androidx.compose.material3.Text("Ошибка: бот не выбран")
                    }
                }

                Screen.BOT_ADMIN -> {
                    val botId = currentBotId
                    if (botId != null) {
                        BotAdminScreen(
                            botId = botId,
                            onBack = { navState.goBack() },
                            onDeleted = { navState.goBack() }
                        )
                    } else {
                        androidx.compose.material3.Text("Ошибка: бот не выбран")
                    }
                }

                Screen.VIBE_PLUS -> VibePlusScreen(
                    onBack = { navState.goBack() },
                    onPay = { itemType ->
                        currentPaymentItem = itemType
                        navState.navigateTo(Screen.PAYMENT_FLOW)
                    }
                )

                Screen.SPARKS -> SparksScreen(
                    onBack = { navState.goBack() },
                    onPay = { itemType ->
                        currentPaymentItem = itemType
                        navState.navigateTo(Screen.PAYMENT_FLOW)
                    }
                )

                Screen.PAYMENT_FLOW -> {
                    if (currentPaymentItem.isNotBlank()) {
                        PaymentFlowScreen(
                            itemType = currentPaymentItem,
                            onBack = { navState.goBack() }
                        )
                    } else {
                        androidx.compose.material3.Text("Ошибка: товар не выбран")
                    }
                }

                Screen.CHANNEL_ADMIN -> {
                    val adminChatId = currentAdminChatId
                    if (adminChatId != null) {
                        ChannelAdminScreen(
                            chatId = adminChatId,
                            onBack = { navState.goBack() },
                            onOpenChat = { chatId, chatName ->
                                currentChatId = chatId
                                currentChatName = chatName
                                currentScrollToMessageId = null
                                navState.navigateTo(Screen.CHAT)
                            }
                        )
                    } else {
                        androidx.compose.material3.Text("Ошибка: канал не выбран")
                    }
                }

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
                    onMesh = { navState.navigateTo(Screen.MESH) },
                    onAbout = { navState.navigateTo(Screen.SETTINGS_ABOUT) },
                    onLogout = {
                        navState.replaceWith(Screen.WELCOME)
                    }
                )

                Screen.MESH -> MeshScreen(onBack = { navState.goBack() })

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
                    contactName = callContactName.ifEmpty { currentChatName.ifEmpty { "Абонент" } },
                    contactUserId = callContactId ?: incomingCallId,
                    incomingRoomId = incomingRoomId,
                    onEndCall = {
                        incomingCallId = null
                        incomingRoomId = null
                        callContactId = null
                        callContactName = ""
                        navState.goBack()
                    }
                )

                Screen.CALL_VIDEO -> CallScreen(
                    isVideoCall = true,
                    contactName = callContactName.ifEmpty { currentChatName.ifEmpty { "Абонент" } },
                    contactUserId = callContactId ?: incomingCallId,
                    incomingRoomId = incomingRoomId,
                    onEndCall = {
                        incomingCallId = null
                        incomingRoomId = null
                        callContactId = null
                        callContactName = ""
                        navState.goBack()
                    }
                )

                Screen.SUPABASE_MAIN -> SupabaseMainScreen(
                    onOpenChat = { chatId, chatTitle ->
                        supabaseChatId = chatId
                        supabaseChatTitle = chatTitle
                        navState.navigateTo(Screen.SUPABASE_CHAT)
                    },
                    onOpenSettings = { navState.navigateTo(Screen.SETTINGS) },
                    onOpenSearch = { navState.navigateTo(Screen.SUPABASE_SEARCH) },
                    onOpenProfile = { navState.navigateTo(Screen.PROFILE) },
                    onOpenContacts = { navState.navigateTo(Screen.CONTACTS) },
                    onOpenCalls = {
                        incomingCallId = null
                        incomingRoomId = null
                        callContactId = null
                        callContactName = ""
                        navState.navigateTo(Screen.CALL_CONTACTS)
                    },
                    onOpenBots = { navState.navigateTo(Screen.BOTS) },
                    onOpenTimeline = { navState.navigateTo(Screen.TIMELINE) },
                    onOpenMarketplace = { navState.navigateTo(Screen.MARKETPLACE) },
                    onOpenAdmin = { navState.navigateTo(Screen.ADMIN) }
                )

                Screen.SUPABASE_CHAT -> SupabaseChatScreen(
                    chatId = supabaseChatId ?: "",
                    chatTitle = supabaseChatTitle,
                    onBack = { navState.goBack() }
                )

                Screen.SUPABASE_SEARCH -> SupabaseSearchScreen(
                    onBack = { navState.goBack() },
                    onOpenChat = { chatId, chatTitle ->
                        supabaseChatId = chatId
                        supabaseChatTitle = chatTitle
                        navState.navigateTo(Screen.SUPABASE_CHAT)
                    }
                )

                Screen.SECURITY_SETTINGS -> SecuritySettingsScreen(
                    userId = serverConfig.getUserId(),
                    contactId = supabaseChatId,
                    onBack = { navState.goBack() }
                )

                Screen.ADMIN -> AdminScreen(
                    onBack = { navState.goBack() }
                )
            }
        }
    }
}
