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
        private const val KEY_RUST_AUTH_TOKEN = "rust_auth_token"
        private const val KEY_MESH_PRIV = "mesh_private_key"
        private const val KEY_MESH_PUB = "mesh_public_key"
        private const val KEY_MESH_PEER_KEYS = "mesh_peer_keys"
        private const val KEY_PRIVACY_ONLINE = "privacy_show_online"
        private const val KEY_PRIVACY_RECEIPTS = "privacy_read_receipts"
        private const val KEY_PRIVACY_PHONE = "privacy_phone_visibility"
        private const val KEY_NOTIF_MESSAGES = "notif_messages"
        private const val KEY_NOTIF_GROUPS = "notif_groups"
        private const val KEY_NOTIF_SOUND = "notif_sound"
        private const val KEY_NOTIF_VIBRATION = "notif_vibration"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_RUST_USER_ID = "rust_user_id"
        private const val KEY_TURN_SERVER = "turn_server_url"
        private const val KEY_TURN_USERNAME = "turn_username"
        private const val KEY_TURN_PASSWORD = "turn_password"
        private const val KEY_SIGNALING_URL = "signaling_url"
        private const val KEY_SIGNALING_ANON_KEY = "signaling_anon_key"
        private const val KEY_CALLS_NOISE = "calls_noise_cancellation"
        private const val KEY_CALLS_VIDEO_QUALITY = "calls_video_quality"
        private const val KEY_TOUR_COMPLETED = "tour_completed"

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
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e("ServerConfig", "EncryptedSharedPreferences failed, retrying with clean state", e)
            try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                android.util.Log.e("ServerConfig", "EncryptedPrefs retry also failed — secure storage unavailable", e2)
                throw RuntimeException("Secure storage initialization failed. App cannot run without encrypted preferences.", e2)
            }
        }
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

    fun getRefreshToken(): String = prefs.getString("refresh_token", "") ?: ""

    fun setRefreshToken(token: String) {
        prefs.edit().putString("refresh_token", token).apply()
    }

    suspend fun refreshSessionIfNeeded() {
        val refreshToken = getRefreshToken()
        if (refreshToken.isBlank()) return
        val result = SupabaseAuthManager.refreshSession(
            com.vibe.ui.BuildConfig.SUPABASE_URL,
            com.vibe.ui.BuildConfig.SUPABASE_ANON_KEY,
            refreshToken
        )
        if (result.success && result.token != null) {
            setAuthToken(result.token)
        }
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
    fun getAppLanguageCode(): String {
        val v = prefs.getString(KEY_LANGUAGE, "ru") ?: "ru"
        return when (v) {
            "Русский" -> "ru"
            "English" -> "en"
            "Deutsch" -> "de"
            "Français" -> "fr"
            "Español" -> "es"
            "中文" -> "zh"
            "日本語" -> "ja"
            else -> if (com.vibe.ui.i18n.VibeLanguages.byCode.containsKey(v)) v else "en"
        }
    }

    fun setAppLanguage(code: String) { prefs.edit().putString(KEY_LANGUAGE, code).apply() }

    fun getRustUserId(): String = prefs.getString(KEY_RUST_USER_ID, "") ?: ""

    fun setRustUserId(userId: String) {
        prefs.edit().putString(KEY_RUST_USER_ID, userId).apply()
    }

    fun getAiBaseUrl(): String = prefs.getString(KEY_AI_BASE_URL, "https://api.zveno.ai/v1") ?: "https://api.zveno.ai/v1"

    fun getTurnServerUrl(): String = prefs.getString(KEY_TURN_SERVER, "") ?: ""

    fun setTurnServerUrl(url: String) {
        prefs.edit().putString(KEY_TURN_SERVER, url).apply()
    }

    fun getTurnUsername(): String = prefs.getString(KEY_TURN_USERNAME, "") ?: ""

    fun setTurnUsername(username: String) {
        prefs.edit().putString(KEY_TURN_USERNAME, username).apply()
    }

    fun getTurnPassword(): String = prefs.getString(KEY_TURN_PASSWORD, "") ?: ""

    fun setTurnPassword(password: String) {
        prefs.edit().putString(KEY_TURN_PASSWORD, password).apply()
    }

    fun getSignalingUrl(): String {
        val stored = prefs.getString(KEY_SIGNALING_URL, "") ?: ""
        if (stored.isNotBlank()) return stored
        return com.vibe.ui.BuildConfig.SUPABASE_URL
    }

    fun setSignalingUrl(url: String) {
        prefs.edit().putString(KEY_SIGNALING_URL, url).apply()
    }

    fun getSignalingAnonKey(): String {
        val stored = prefs.getString(KEY_SIGNALING_ANON_KEY, "") ?: ""
        if (stored.isNotBlank()) return stored
        return com.vibe.ui.BuildConfig.SUPABASE_ANON_KEY
    }

    fun setSignalingAnonKey(key: String) {
        prefs.edit().putString(KEY_SIGNALING_ANON_KEY, key).apply()
    }

    fun setAiBaseUrl(url: String) {
        prefs.edit().putString(KEY_AI_BASE_URL, url).apply()
    }

    fun isNoiseCancellation(): Boolean = prefs.getBoolean(KEY_CALLS_NOISE, false)

    fun setNoiseCancellation(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CALLS_NOISE, enabled).apply()
    }

    fun isVideoHighQuality(): Boolean = prefs.getBoolean(KEY_CALLS_VIDEO_QUALITY, true)

    fun setVideoHighQuality(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CALLS_VIDEO_QUALITY, enabled).apply()
    }

    fun getAiModel(): String = prefs.getString(KEY_AI_MODEL, "google/gemini-3-flash-preview") ?: "google/gemini-3-flash-preview"

    fun setAiModel(model: String) {
        prefs.edit().putString(KEY_AI_MODEL, model).apply()
    }

    fun isTourCompleted(): Boolean = prefs.getBoolean(KEY_TOUR_COMPLETED, false)

    fun setTourCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_TOUR_COMPLETED, completed).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun getRustServerUrl(): String {
        val stored = prefs.getString(KEY_RUST_SERVER_URL, "") ?: ""
        if (stored.isNotBlank()) return stored
        return com.vibe.ui.BuildConfig.RUST_SERVER_URL.ifEmpty { DEFAULT_RUST_SERVER_URL }
    }

    fun setRustServerUrl(url: String) {
        prefs.edit().putString(KEY_RUST_SERVER_URL, url).apply()
    }

    fun getRustWsUrl(): String {
        val stored = prefs.getString(KEY_RUST_WS_URL, "") ?: ""
        if (stored.isNotBlank()) return stored
        val buildWs = com.vibe.ui.BuildConfig.RUST_WS_URL
        if (buildWs.isNotBlank()) return buildWs
        return DEFAULT_RUST_WS_URL
    }

    fun setRustWsUrl(url: String) {
        prefs.edit().putString(KEY_RUST_WS_URL, url).apply()
    }

    fun getRustAuthToken(): String = prefs.getString(KEY_RUST_AUTH_TOKEN, "") ?: ""

    fun setRustAuthToken(token: String) {
        prefs.edit().putString(KEY_RUST_AUTH_TOKEN, token).apply()
    }

    fun getMeshPrivateKey(): String = prefs.getString(KEY_MESH_PRIV, "") ?: ""

    fun setMeshPrivateKey(key: String) {
        prefs.edit().putString(KEY_MESH_PRIV, key).apply()
    }

    fun getMeshPublicKey(): String = prefs.getString(KEY_MESH_PUB, "") ?: ""

    fun setMeshPublicKey(key: String) {
        prefs.edit().putString(KEY_MESH_PUB, key).apply()
    }

    fun getMeshPeerKeys(): Map<String, String> {
        val raw = prefs.getString(KEY_MESH_PEER_KEYS, "") ?: return emptyMap()
        return try {
            val obj = org.json.JSONObject(raw)
            val out = HashMap<String, String>()
            obj.keys().forEach { k -> out[k] = obj.optString(k) }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun setMeshPeerKeys(keys: Map<String, String>) {
        val obj = org.json.JSONObject()
        keys.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_MESH_PEER_KEYS, obj.toString()).apply()
    }

    fun hasServerConfig(): Boolean {
        return prefs.contains(KEY_SERVER_URL)
    }
}
