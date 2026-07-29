package com.vibe.ui.screens

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.ui.R
import com.vibe.ui.adapters.ChatListAdapter
import com.vibe.ui.di.VibeContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Contacts Screen - List of contacts
 */
class ContactsActivity : AppCompatActivity() {

    private lateinit var chatListAdapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vibe_screen_contacts)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val contactsRecycler = findViewById<RecyclerView>(R.id.contacts_recycler)
        val emptyState = findViewById<TextView>(R.id.empty_state)

        // Setup contacts list
        chatListAdapter = ChatListAdapter { contact ->
            // Open contact profile
        }
        contactsRecycler.layoutManager = LinearLayoutManager(this)
        contactsRecycler.adapter = chatListAdapter

        btnBack.setOnClickListener {
            finish()
        }

        // Load contacts
        loadContacts()
    }

    private fun loadContacts() {
        lifecycleScope.launch {
            try {
                val gateway = VibeContainer.getGateway()
                // Use first() to get initial contacts list
                val contacts = gateway.contacts.getContacts()

                withContext(Dispatchers.Main) {
                    // Convert VibeUser to VibeChat for display
                    val chatContacts = contacts.map { user ->
                        com.vibe.bridge.model.VibeChat(
                            id = user.id,
                            title = "${user.firstName} ${user.lastName ?: ""}".trim(),
                            type = com.vibe.bridge.model.VibeChat.ChatType.PRIVATE,
                            lastMessage = null,
                            unreadCount = 0,
                            isMuted = false,
                            isPinned = false,
                            isArchived = false,
                            draftText = null,
                            lastActivityDate = System.currentTimeMillis()
                        )
                    }

                    if (chatContacts.isNotEmpty()) {
                        chatListAdapter.submitList(chatContacts)
                        findViewById<TextView>(R.id.empty_state).visibility = android.view.View.GONE
                        findViewById<RecyclerView>(R.id.contacts_recycler).visibility = android.view.View.VISIBLE
                    } else {
                        chatListAdapter.submitList(emptyList())
                        findViewById<TextView>(R.id.empty_state).visibility = android.view.View.VISIBLE
                        findViewById<RecyclerView>(R.id.contacts_recycler).visibility = android.view.View.GONE
                    }
                }
            } catch (e: Exception) {
                com.vibe.common.logging.VibeLogger.e("ContactsActivity", "Operation failed", e)
            }
        }
    }
}
