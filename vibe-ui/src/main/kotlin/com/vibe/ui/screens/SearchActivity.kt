package com.vibe.ui.screens

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.ui.R
import com.vibe.ui.adapters.ChatListAdapter
import com.vibe.ui.di.VibeContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Search Screen - Global search across chats, contacts, messages
 */
class SearchActivity : AppCompatActivity() {

    private lateinit var chatListAdapter: ChatListAdapter
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vibe_screen_search)

        val searchInput = findViewById<EditText>(R.id.search_input)
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val resultsRecycler = findViewById<RecyclerView>(R.id.results_recycler)
        val emptyState = findViewById<View>(R.id.empty_state)

        // Setup chat list
        chatListAdapter = ChatListAdapter { chat ->
            // Open chat
            finish()
        }
        resultsRecycler.layoutManager = LinearLayoutManager(this)
        resultsRecycler.adapter = chatListAdapter

        // Search functionality with debounce
        searchInput.requestFocus()
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // Debounce 300ms
                    performSearch(s?.toString() ?: "")
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnBack.setOnClickListener {
            finish()
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isEmpty()) {
            chatListAdapter.submitList(emptyList())
            withContext(Dispatchers.Main) {
                findViewById<View>(R.id.empty_state).visibility = View.VISIBLE
                findViewById<RecyclerView>(R.id.results_recycler).visibility = View.GONE
            }
            return
        }

        try {
            val gateway = VibeContainer.getGateway()
            val allChats = mutableListOf<com.vibe.bridge.model.VibeChat>()

            // Search in chats - use first() to get initial snapshot
            val chats = gateway.chats.getActiveChats().first()
            val filtered = chats.filter { chat ->
                chat.title.contains(query, ignoreCase = true) ||
                chat.lastMessage?.text?.contains(query, ignoreCase = true) == true
            }
            allChats.addAll(filtered)

            withContext(Dispatchers.Main) {
                if (allChats.isNotEmpty()) {
                    chatListAdapter.submitList(allChats)
                    findViewById<View>(R.id.empty_state).visibility = View.GONE
                    findViewById<RecyclerView>(R.id.results_recycler).visibility = View.VISIBLE
                } else {
                    chatListAdapter.submitList(emptyList())
                    findViewById<View>(R.id.empty_state).visibility = View.VISIBLE
                    findViewById<RecyclerView>(R.id.results_recycler).visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            com.vibe.common.logging.VibeLogger.e("SearchActivity", "Operation failed", e)
        }
    }
}
