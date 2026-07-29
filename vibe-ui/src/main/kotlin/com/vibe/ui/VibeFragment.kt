package com.vibe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.bridge.model.VibeChat
import com.vibe.ui.adapters.ChatListAdapter
import com.vibe.ui.di.VibeContainer
import com.vibe.ui.navigation.VibeNavigator
import com.vibe.ui.navigation.VibeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main Vibe fragment - the 5th tab in MainTabsActivity.
 */
class VibeFragment : Fragment() {

    private lateinit var navigator: VibeNavigator
    private lateinit var chatListAdapter: ChatListAdapter
    private var allChats: List<VibeChat> = emptyList()

    private lateinit var screenContainer: LinearLayout
    private lateinit var toolbarTitle: TextView
    private lateinit var navChats: LinearLayout
    private lateinit var navAurion: LinearLayout
    private lateinit var navProfile: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigator = VibeNavigator()
        VibeContainer.initialize()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.vibe_fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        screenContainer = view.findViewById(R.id.vibe_screen_container)
        toolbarTitle = view.findViewById(R.id.vibe_toolbar_title)
        navChats = view.findViewById(R.id.nav_chats)
        navAurion = view.findViewById(R.id.nav_aurion)
        navProfile = view.findViewById(R.id.nav_profile)

        setupBottomNav()
        showChatList()
    }

    private fun setupBottomNav() {
        navChats.setOnClickListener {
            setActiveTab(navChats)
            showChatList()
        }

        navAurion.setOnClickListener {
            setActiveTab(navAurion)
            showAurionChat()
        }

        navProfile.setOnClickListener {
            setActiveTab(navProfile)
            showProfile()
        }
    }

    private fun setActiveTab(activeTab: LinearLayout) {
        val tabs = listOf(navChats, navAurion, navProfile)
        val labels = listOf("Чаты", "Aurion", "Профиль")
        val activeIndex = tabs.indexOf(activeTab)

        tabs.forEachIndexed { index, tab ->
            val label = tab.findViewById<TextView>(tab.childCount - 1)
            val icon = tab.findViewById<android.widget.ImageView>(0)

            if (index == activeIndex) {
                label?.setTextColor(0xFF7A4DFF.toInt())
                icon?.setColorFilter(0xFF7A4DFF.toInt())
            } else {
                label?.setTextColor(0xFFB0B0B0.toInt())
                icon?.setColorFilter(0xFFB0B0B0.toInt())
            }
        }
    }

    private fun showChatList() {
        toolbarTitle.text = "Vibe"

        val chatListView = LayoutInflater.from(requireContext())
            .inflate(R.layout.vibe_screen_chat_list, screenContainer, false)
        screenContainer.removeAllViews()
        screenContainer.addView(chatListView)

        val recyclerView = chatListView.findViewById<RecyclerView>(R.id.chat_list_recycler)
        chatListAdapter = ChatListAdapter { chat ->
            navigator.navigate(VibeScreen.ChatView(chat))
            showChatView(chat)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = chatListAdapter

        loadChats()
    }

    private fun loadChats() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val gateway = VibeContainer.getGateway()
                gateway.chats.getActiveChats().collect { chats ->
                    withContext(Dispatchers.Main) {
                        allChats = chats
                        chatListAdapter.submitList(chats)
                    }
                }
            } catch (e: Exception) {
                com.vibe.common.logging.VibeLogger.e("VibeFragment", "Operation failed", e)
            }
        }
    }

    private fun showChatView(chat: VibeChat) {
        toolbarTitle.text = chat.title

        val chatView = LayoutInflater.from(requireContext())
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
            showChatList()
        }

        val messageAdapter = com.vibe.ui.adapters.MessageListAdapter()
        messagesRecycler.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
            reverseLayout = true
        }
        messagesRecycler.adapter = messageAdapter

        // Load messages
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val gateway = VibeContainer.getGateway()
                gateway.messages.getRecentMessages(chat.id).collect { messages ->
                    withContext(Dispatchers.Main) {
                        messageAdapter.submitList(messages)
                    }
                }
            } catch (e: Exception) {
                com.vibe.common.logging.VibeLogger.e("VibeFragment", "Operation failed", e)
            }
        }

        // Send message
        val sendMessage = {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val gateway = VibeContainer.getGateway()
                        gateway.messages.sendTextMessage(chat.id, text)
                        withContext(Dispatchers.Main) {
                            messageInput.text.clear()
                        }
                    } catch (e: Exception) {
                        com.vibe.common.logging.VibeLogger.e("VibeFragment", "Operation failed", e)
                    }
                }
            }
        }

        btnSend.setOnClickListener { sendMessage() }
        messageInput.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }

    private fun showAurionChat() {
        toolbarTitle.text = "Aurion AI"

        val chatView = LayoutInflater.from(requireContext())
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
            showChatList()
        }
    }

    private fun showProfile() {
        toolbarTitle.text = "Профиль"

        val profileView = LayoutInflater.from(requireContext())
            .inflate(R.layout.vibe_screen_profile, screenContainer, false)
        screenContainer.removeAllViews()
        screenContainer.addView(profileView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        VibeContainer.destroy()
    }
}
