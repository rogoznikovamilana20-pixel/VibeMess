package com.vibe.ui.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Server configuration manager.
 * Stores server URL and credentials securely.
 */
class ServerConfig(context: Context) {

    companion object {
        private const val PREFS_NAME = "vibe_server_config"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_VIBE_ID = "vibe_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
        private const val KEY_AI_API_KEY = "ai_api_key"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_AI_BASE_URL = "ai_base_url"
        private const val KEY_AI_MODEL = "ai_model"
        private const val KEY_RUST_SERVER_URL = "rust_server_url"
        private const val KEY_RUST_WS_URL = "rust_ws_url"
        private const val KEY_PRIVACY_ONLINE = "privacy_show_online"
        private const val KEY_PRIVACY_RECEIPTS = "privacy_read_receipts"
        private const val KEY_PRIVACY_PHONE = "privacy_phone_visibility"
        private const val KEY_NOTIF_MESSAGES = "notif_messages"
        private const val KEY_NOTIF_GROUPS = "notif_groups"
        private const val KEY_NOTIF_SOUND = "notif_sound"
        private const val KEY_NOTIF_VIBRATION = "notif_vibration"
        private const val KEY_LANGUAGE = "app_language"

        // Default server URLs (localhost for development)
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:8766"
        const val DEFAULT_RUST_SERVER_URL = "http://10.0.2.2:3000"
        const val DEFAULT_RUST_WS_URL = "ws://10.0.2.2:3000/ws"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getServerUrl(): String = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL

    fun setServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""

    fun setUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getAuthToken(): String = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""

    fun setAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getVibeId(): String = prefs.getString(KEY_VIBE_ID, "") ?: ""

    fun setVibeId(vibeId: String) {
        prefs.edit().putString(KEY_VIBE_ID, vibeId).apply()
    }

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun setUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun isAuthenticated(): Boolean = prefs.getBoolean(KEY_IS_AUTHENTICATED, false)

    fun setAuthenticated(authenticated: Boolean) {
        prefs.edit().putBoolean(KEY_IS_AUTHENTICATED, authenticated).apply()
    }

    fun getAiApiKey(): String {
        val stored = prefs.getString(KEY_AI_API_KEY, "") ?: ""
        if (stored.isNotBlank()) return stored
        return com.vibe.ui.BuildConfig.AI_API_KEY
    }

    fun setAiApiKey(key: String) {
        prefs.edit().putString(KEY_AI_API_KEY, key).apply()
    }

    fun getAiProvider(): String = prefs.getString(KEY_AI_PROVIDER, "") ?: ""

    fun setAiProvider(provider: String) {
        prefs.edit().putString(KEY_AI_PROVIDER, provider).apply()
    }

    fun isPrivacyOnline(): Boolean = prefs.getBoolean(KEY_PRIVACY_ONLINE, true)
    fun setPrivacyOnline(value: Boolean) { prefs.edit().putBoolean(KEY_PRIVACY_ONLINE, value).apply() }
    fun isPrivacyReceipts(): Boolean = prefs.getBoolean(KEY_PRIVACY_RECEIPTS, true)
    fun setPrivacyReceipts(value: Boolean) { prefs.edit().putBoolean(KEY_PRIVACY_RECEIPTS, value).apply() }
    fun isPrivacyPhone(): Boolean = prefs.getBoolean(KEY_PRIVACY_PHONE, true)
    fun setPrivacyPhone(value: Boolean) { prefs.edit().putBoolean(KEY_PRIVACY_PHONE, value).apply() }
    fun isNotifMessages(): Boolean = prefs.getBoolean(KEY_NOTIF_MESSAGES, true)
    fun setNotifMessages(value: Boolean) { prefs.edit().putBoolean(KEY_NOTIF_MESSAGES, value).apply() }
    fun isNotifGroups(): Boolean = prefs.getBoolean(KEY_NOTIF_GROUPS, true)
    fun setNotifGroups(value: Boolean) { prefs.edit().putBoolean(KEY_NOTIF_GROUPS, value).apply() }
    fun isNotifSound(): Boolean = prefs.getBoolean(KEY_NOTIF_SOUND, true)
    fun setNotifSound(value: Boolean) { prefs.edit().putBoolean(KEY_NOTIF_SOUND, value).apply() }
    fun isNotifVibration(): Boolean = prefs.getBoolean(KEY_NOTIF_VIBRATION, true)
    fun setNotifVibration(value: Boolean) { prefs.edit().putBoolean(KEY_NOTIF_VIBRATION, value).apply() }
    fun getAppLanguage(): String = prefs.getString(KEY_LANGUAGE, "Русский") ?: "Русский"
    fun setAppLanguage(lang: String) { prefs.edit().putString(KEY_LANGUAGE, lang).apply() }

    fun getAiBaseUrl(): String = prefs.getString(KEY_AI_BASE_URL, "http://192.168.1.100:11434/v1") ?: "http://192.168.1.100:11434/v1"

    fun setAiBaseUrl(url: String) {
        prefs.edit().putString(KEY_AI_BASE_URL, url).apply()
    }

    fun getAiModel(): String = prefs.getString(KEY_AI_MODEL, "llama3.2") ?: "llama3.2"

    fun setAiModel(model: String) {
        prefs.edit().putString(KEY_AI_MODEL, model).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun getRustServerUrl(): String = prefs.getString(KEY_RUST_SERVER_URL, DEFAULT_RUST_SERVER_URL) ?: DEFAULT_RUST_SERVER_URL

    fun setRustServerUrl(url: String) {
        prefs.edit().putString(KEY_RUST_SERVER_URL, url).apply()
    }

    fun getRustWsUrl(): String = prefs.getString(KEY_RUST_WS_URL, DEFAULT_RUST_WS_URL) ?: DEFAULT_RUST_WS_URL

    fun setRustWsUrl(url: String) {
        prefs.edit().putString(KEY_RUST_WS_URL, url).apply()
    }

    fun hasServerConfig(): Boolean {
        return prefs.contains(KEY_SERVER_URL)
    }
}
