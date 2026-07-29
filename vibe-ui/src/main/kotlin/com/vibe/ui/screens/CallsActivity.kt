package com.vibe.ui.screens

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.ui.R
import com.vibe.ui.adapters.ChatListAdapter

/**
 * Calls Screen - Call history
 */
class CallsActivity : AppCompatActivity() {

    private lateinit var chatListAdapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vibe_screen_calls)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val callsRecycler = findViewById<RecyclerView>(R.id.calls_recycler)
        val emptyState = findViewById<TextView>(R.id.empty_state)

        // Setup calls list
        chatListAdapter = ChatListAdapter { call ->
            // Start call
        }
        callsRecycler.layoutManager = LinearLayoutManager(this)
        callsRecycler.adapter = chatListAdapter

        btnBack.setOnClickListener {
            finish()
        }

        // Show empty state
        emptyState.visibility = android.view.View.VISIBLE
        callsRecycler.visibility = android.view.View.GONE
    }
}
