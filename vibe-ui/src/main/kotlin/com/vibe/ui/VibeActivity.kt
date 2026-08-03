package com.vibe.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibe.ui.compose.components.BottomNavTab
import com.vibe.ui.compose.screens.chat.ChatListScreen
import com.vibe.ui.compose.screens.chat.ChatListViewModel
import com.vibe.ui.compose.screens.placeholder.AurionScreen
import com.vibe.ui.compose.screens.placeholder.CallsScreen
import com.vibe.ui.compose.screens.placeholder.ProfileScreen
import com.vibe.ui.compose.theme.VibeTheme
import com.vibe.ui.di.VibeContainer
import com.vibe.ui.security.SecureKeyManager
import org.telegram.messenger.ApplicationLoader

class VibeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        try {
            VibeAppContext.init(this)
            VibeContainer.bindContext(this)
            ApplicationLoader.postInitApplication()
        } catch (e: Throwable) {
            android.util.Log.e("VibeActivity", "Core init failed", e)
        }
        try {
            VibeContainer.initialize()
        } catch (e: Exception) {
            android.util.Log.e("VibeActivity", "Container init failed", e)
        }
        try {
            SecureKeyManager(this)
        } catch (e: Exception) {
            android.util.Log.e("VibeActivity", "SecureKeyManager init failed", e)
        }

        window.decorView.setBackgroundColor(0xFF0C081A.toInt())

        setContent {
            VibeTheme(darkTheme = true) {
                VibeMainNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            VibeContainer.destroy()
        }
    }
}

@Composable
private fun VibeMainNavHost() {
    val navController = rememberNavController()
    val chatListViewModel = remember { ChatListViewModel() }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chats",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("chats") {
                ChatListScreen(
                    viewModel = chatListViewModel,
                    onChatClick = { chatId ->
                        // TODO: navigate to chat detail
                    },
                    onNewChatClick = {
                        // TODO: navigate to create chat
                    }
                )
            }
            composable("calls") {
                CallsScreen(
                    onTabSelected = { tab ->
                        val route = when (tab) {
                            BottomNavTab.CHATS -> "chats"
                            BottomNavTab.CALLS -> "calls"
                            BottomNavTab.AURION -> "aurion"
                            BottomNavTab.PROFILE -> "profile"
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("aurion") {
                AurionScreen(
                    onTabSelected = { tab ->
                        val route = when (tab) {
                            BottomNavTab.CHATS -> "chats"
                            BottomNavTab.CALLS -> "calls"
                            BottomNavTab.AURION -> "aurion"
                            BottomNavTab.PROFILE -> "profile"
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onTabSelected = { tab ->
                        val route = when (tab) {
                            BottomNavTab.CHATS -> "chats"
                            BottomNavTab.CALLS -> "calls"
                            BottomNavTab.AURION -> "aurion"
                            BottomNavTab.PROFILE -> "profile"
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
