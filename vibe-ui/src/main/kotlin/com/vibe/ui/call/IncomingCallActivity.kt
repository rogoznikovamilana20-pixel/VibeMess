package com.vibe.ui.call

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.vibe.ui.VibeActivity

class IncomingCallActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callerId = intent?.getStringExtra("callerId") ?: return finish()
        val roomId = intent?.getStringExtra("roomId") ?: return finish()
        val action = intent?.getStringExtra("action") ?: return finish()

        val mainIntent = Intent(this, VibeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("call_action", action)
            putExtra("caller_id", callerId)
            putExtra("room_id", roomId)
        }
        startActivity(mainIntent)
        finish()
    }
}
