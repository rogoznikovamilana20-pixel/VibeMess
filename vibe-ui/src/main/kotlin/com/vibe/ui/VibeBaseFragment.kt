package com.vibe.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.bridge.model.VibeChat
import com.vibe.ui.adapters.ChatListAdapter
import com.vibe.ui.adapters.MessageListAdapter
import com.vibe.ui.di.VibeContainer
import com.vibe.ui.navigation.VibeNavigator
import com.vibe.ui.navigation.VibeScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.ui.ActionBar.BaseFragment

/**
 * BaseFragment wrapper for Vibe UI.
 * Integrates with Telegram's fragment system while hosting Vibe's custom UI.
 */
class VibeBaseFragment(args: Bundle) : BaseFragment(args) {

    private lateinit var navigator: VibeNavigator
    private lateinit var chatListAdapter: ChatListAdapter
    private var allChats: List<VibeChat> = emptyList()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var screenContainer: LinearLayout
    private lateinit var toolbarTitle: TextView
    private lateinit var navChats: LinearLayout
    private lateinit var navAurion: LinearLayout
    private lateinit var navProfile: LinearLayout

    override fun onFragmentCreate(): Boolean {
        navigator = VibeNavigator()
        VibeContainer.initialize()
        return super.onFragmentCreate()
    }

    override fun createView(context: Context): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.vibe_fragment_main, null, false) as ViewGroup

        screenContainer = view.findViewById(R.id.vibe_screen_container)
        toolbarTitle = view.findViewById(R.id.vibe_toolbar_title)
        navChats = view.findViewById(R.id.nav_chats)
        navAurion = view.findViewById(R.id.nav_aurion)
        navProfile = view.findViewById(R.id.nav_profile)

        setupBottomNav()
        showChatList(context)

        fragmentView = view
        return view
    }

    private fun setupBottomNav() {
        navChats.setOnClickListener {
            setActiveTab(navChats)
            fragmentView?.context?.let { showChatList(it) }
        }

        navAurion.setOnClickListener {
            setActiveTab(navAurion)
            fragmentView?.context?.let { showAurionChat(it) }
        }

        navProfile.setOnClickListener {
            setActiveTab(navProfile)
            fragmentView?.context?.let { showProfile(it) }
        }
    }

    private fun setActiveTab(activeTab: LinearLayout) {
        val tabs = listOf(navChats, navAurion, navProfile)

        tabs.forEach { tab ->
            val label = tab.findViewById<TextView>(tab.childCount - 1)
            val icon = tab.findViewById<android.widget.ImageView>(0)

            if (tab == activeTab) {
                label?.setTextColor(0xFF7A4DFF.toInt())
                icon?.setColorFilter(0xFF7A4DFF.toInt())
            } else {
                label?.setTextColor(0xFFB0B0B0.toInt())
                icon?.setColorFilter(0xFFB0B0B0.toInt())
            }
        }
    }

    private fun showChatList(context: Context) {
        toolbarTitle.text = "Vibe"

        val chatListView = LayoutInflater.from(context)
            .inflate(R.layout.vibe_screen_chat_list, screenContainer, false)
        screenContainer.removeAllViews()
        screenContainer.addView(chatListView)

        val recyclerView = chatListView.findViewById<RecyclerView>(R.id.chat_list_recycler)
        chatListAdapter = ChatListAdapter { chat ->
            navigator.navigate(VibeScreen.ChatView(chat))
            fragmentView?.context?.let { showChatView(it, chat) }
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = chatListAdapter

        loadChats()
    }

    private fun loadChats() {
        scope.launch {
            try {
                val gateway = VibeContainer.getGateway()
                gateway.chats.getActiveChats().collect { chats ->
                    withContext(Dispatchers.Main) {
                        allChats = chats
                        chatListAdapter.submitList(chats)
                    }
                }
            } catch (e: Exception) {
                com.vibe.common.logging.VibeLogger.e("VibeBaseFragment", "Operation failed", e)
            }
        }
    }

    private fun showChatView(context: Context, chat: VibeChat) {
        toolbarTitle.text = chat.title

        val chatView = LayoutInflater.from(context)
            .inflate(R.layout.vibe_screen_chat_view, screenContainer, false)
        screenContainer.removeAllViews()
        screenContainer.addView(chatView)

        val headerName = chatView.findViewById<TextView>(R.id.header_name)
        val headerStatus = chatView.findViewById<TextView>(R.id.header_status)
        val btnBack = chatView.findViewById<ImageButton>(R.id.btn_back)
        val messagesRecycler = chatView.findViewById<RecyclerView>(R.id.messages_recycler)
        val messageInput = chatView.findViewById<android.widget.EditText>(R.id.message_input)
        val btnSend = chatView.findViewById<ImageButton>(R.id.btn_send)

        headerName.text = chat.title
        headerStatus.text = if (chat.type == VibeChat.ChatType.PRIVATE) "в сети" else "${chat.unreadCount} участников"

        btnBack.setOnClickListener {
            navigator.navigate(VibeScreen.ChatList)
            showChatList(context)
        }

        val messageAdapter = MessageListAdapter()
        messagesRecycler.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        messagesRecycler.adapter = messageAdapter

        scope.launch(Dispatchers.IO) {
            try {
                val gateway = VibeContainer.getGateway()
                gateway.messages.getRecentMessages(chat.id).collect { messages ->
                    withContext(Dispatchers.Main) {
                        messageAdapter.submitList(messages)
                    }
                }
            } catch (e: Exception) {
                com.vibe.common.logging.VibeLogger.e("VibeBaseFragment", "Operation failed", e)
            }
        }

        val sendMessage = {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                scope.launch {
                    try {
                        val gateway = VibeContainer.getGateway()
                        gateway.messages.sendTextMessage(chat.id, text)
                        withContext(Dispatchers.Main) {
                            messageInput.text.clear()
                        }
                    } catch (e: Exception) {
                        com.vibe.common.logging.VibeLogger.e("VibeBaseFragment", "Operation failed", e)
                    }
                }
            }
        }

        btnSend.setOnClickListener { sendMessage() }
    }

    private fun showAurionChat(context: Context) {
        toolbarTitle.text = "Aurion AI"

        val chatView = LayoutInflater.from(context)
            .inflate(R.layout.vibe_screen_chat_view, screenContainer, false)
        screenContainer.removeAllViews()
        screenContainer.addView(chatView)

        val headerName = chatView.findViewById<TextView>(R.id.header_name)
        val headerStatus = chatView.findViewById<TextView>(R.id.header_status)
        val btnBack = chatView.findViewById<ImageButton>(R.id.btn_back)

        headerName.text = "Aurion"
        headerStatus.text = "AI-помощник"

        btnBack.setOnClickListener {
            navigator.navigate(VibeScreen.ChatList)
            showChatList(context)
        }
    }

    private fun showProfile(context: Context) {
        toolbarTitle.text = "Профиль"

        val profileView = LayoutInflater.from(context)
            .inflate(R.layout.vibe_screen_profile, screenContainer, false)
        screenContainer.removeAllViews()
        screenContainer.addView(profileView)
    }

    override fun onFragmentDestroy() {
        super.onFragmentDestroy()
        scope.cancel()
        VibeContainer.destroy()
    }
}
