package com.vibe.ui.data

import android.content.Context
import android.content.SharedPreferences

class ProfileRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vibe_profile", Context.MODE_PRIVATE)

    var displayName: String
        get() = prefs.getString(KEY_NAME, "Андрей") ?: "Андрей"
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var bio: String
        get() = prefs.getString(KEY_BIO, "Full-stack developer, AI enthusiast. Создаю Vibe.") ?: ""
        set(value) = prefs.edit().putString(KEY_BIO, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "@andre") ?: "@andre"
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var vibeId: String
        get() = prefs.getString(KEY_VIBE_ID, "vibe#42a7f1") ?: "vibe#42a7f1"
        set(value) = prefs.edit().putString(KEY_VIBE_ID, value).apply()

    var avatarInitial: String
        get() = prefs.getString(KEY_AVATAR, "А") ?: "А"
        set(value) = prefs.edit().putString(KEY_AVATAR, value).apply()

    companion object {
        private const val KEY_NAME = "display_name"
        private const val KEY_BIO = "bio"
        private const val KEY_USERNAME = "username"
        private const val KEY_VIBE_ID = "vibe_id"
        private const val KEY_AVATAR = "avatar_initial"
    }
}
