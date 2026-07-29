package com.vibe.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.vibe.ui.compose.VibeApp
import com.vibe.ui.di.VibeContainer
import com.vibe.ui.security.SecureKeyManager

class VibeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        VibeContainer.initialize()
        SecureKeyManager(this)

        window.decorView.setBackgroundColor(0xFF0C0B1A.toInt())

        setContent {
            VibeApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VibeContainer.destroy()
    }
}
