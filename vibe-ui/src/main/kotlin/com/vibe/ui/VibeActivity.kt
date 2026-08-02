package com.vibe.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.vibe.ui.compose.VibeApp
import com.vibe.ui.di.VibeContainer
import com.vibe.ui.security.SecureKeyManager
import org.telegram.messenger.ApplicationLoader

class VibeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        try {
            // Full Telegram core initialization (configs, accounts, services) —
            // exactly what LaunchActivity does in stock Telegram. Idempotent.
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

        window.decorView.setBackgroundColor(0xFF0C0B1A.toInt())

        setContent {
            VibeApp()
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
