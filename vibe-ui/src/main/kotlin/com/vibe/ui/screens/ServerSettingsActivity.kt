package com.vibe.ui.screens

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vibe.ui.R
import com.vibe.ui.network.ServerConfig
import com.vibe.ui.network.VibeHttpClient
import kotlinx.coroutines.launch

/**
 * Server Settings Screen
 * Configure server connection for Vibe backend.
 */
class ServerSettingsActivity : AppCompatActivity() {

    private lateinit var serverConfig: ServerConfig
    private lateinit var httpClient: VibeHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vibe_screen_server_settings)

        serverConfig = ServerConfig(this)
        httpClient = VibeHttpClient(serverConfig)

        val serverUrlInput = findViewById<EditText>(R.id.input_server_url)
        val userIdText = findViewById<TextView>(R.id.text_user_id)
        val vibeIdText = findViewById<TextView>(R.id.text_vibe_id)
        val statusText = findViewById<TextView>(R.id.text_status)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnTest = findViewById<Button>(R.id.btn_test)
        val btnBack = findViewById<ImageButton>(R.id.btn_back)

        // Load current config
        serverUrlInput.setText(serverConfig.getServerUrl())
        userIdText.text = "User ID: ${serverConfig.getUserId().take(16)}..."
        vibeIdText.text = "Vibe ID: ${serverConfig.getVibeId()}"
        statusText.text = if (serverConfig.isAuthenticated()) "Подключено" else "Не подключено"

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val url = serverUrlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                serverConfig.setServerUrl(url)
                Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            }
        }

        btnTest.setOnClickListener {
            testConnection()
        }
    }

    private fun testConnection() {
        lifecycleScope.launch {
            try {
                val code = httpClient.requestVerificationCode("test@test.com")
                if (code != null) {
                    Toast.makeText(this@ServerSettingsActivity, "Сервер доступен! Код: $code", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ServerSettingsActivity, "Сервер недоступен", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ServerSettingsActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
