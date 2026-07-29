package com.vibe.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vibe.ui.R

/**
 * Offline/Error State Screen
 * Shows when app cannot start due to network issues or errors.
 */
class OfflineErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vibe_screen_offline_error)

        val retryButton = findViewById<Button>(R.id.btn_retry)
        
        retryButton.setOnClickListener {
            // Retry - go back to splash
            val intent = Intent(this, SplashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}
