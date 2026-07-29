package com.vibe.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.common.performance.PerformanceMonitor
import com.vibe.ui.R
import com.vibe.ui.adapters.ChatListAdapter
import com.vibe.ui.components.VibeTabs
import com.vibe.ui.di.VibeContainer
import com.vibe.ui.monitoring.AnalyticsManager
import com.vibe.ui.theme.VibeAnimations
import kotlinx.coroutines.launch

/**
 * Main Screen - chat list with Personal/Work modes
 */
class MainActivity : AppCompatActivity() {

    private lateinit var modeTabs: VibeTabs
    private lateinit var chatListAdapter: ChatListAdapter
    private var isPersonalMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        PerformanceMonitor.trackExecution("MainActivity.onCreate") {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.vibe_screen_main)

            // Track screen view
            AnalyticsManager.getInstance(this).trackScreenView("MainActivity")

            VibeContainer.initialize()

            modeTabs = findViewById(R.id.mode_tabs)
            val btnMenu = findViewById<ImageButton>(R.id.btn_menu)
            val chatListRecycler = findViewById<RecyclerView>(R.id.chat_list_recycler)
            val fabNewChat = findViewById<android.widget.ImageButton>(R.id.fab_new_chat)

            // Setup mode tabs
            modeTabs.setTabs(listOf("Личное", "Работа"))
            modeTabs.setOnTabSelectedListener { index ->
                isPersonalMode = index == 0
                AnalyticsManager.getInstance(this).trackEvent("mode_changed", mapOf("mode" to if (index == 0) "personal" else "work"))
                loadChats()
            }

            // Setup chat list
            chatListAdapter = ChatListAdapter { chat ->
                // Open chat
                AnalyticsManager.getInstance(this).trackChatInteraction(chat.id, "chat_opened")
            }
            chatListRecycler.layoutManager = LinearLayoutManager(this)
            chatListRecycler.adapter = chatListAdapter

            // Setup menu button - open side menu
            btnMenu.setOnClickListener {
                AnalyticsManager.getInstance(this).trackEvent("menu_opened")
                startActivity(Intent(this, SideMenuActivity::class.java))
            }

            // Setup FAB
            fabNewChat.setOnClickListener {
                AnalyticsManager.getInstance(this).trackEvent("new_chat_requested")
                // Show new chat options
            }

            // Load initial chats
            loadChats()

            // Animate entrance
            VibeAnimations.fadeIn(chatListRecycler, 300)
        }
    }

    private fun loadChats() {
        // Load chats from bridge using coroutines
        lifecycleScope.launch {
            try {
                val gateway = VibeContainer.getGateway()
                gateway.chats.getActiveChats().collect { chats ->
                    // Note: Mode filtering (Personal/Work) can be implemented based on chat type or custom logic
                    // For now, showing all chats
                    chatListAdapter.submitList(chats)
                }
            } catch (e: Exception) {
                // Handle error gracefully - show empty state or error message
                chatListAdapter.submitList(emptyList())
                android.util.Log.e("MainActivity", "Error loading chats", e)
            }
        }
    }
}
