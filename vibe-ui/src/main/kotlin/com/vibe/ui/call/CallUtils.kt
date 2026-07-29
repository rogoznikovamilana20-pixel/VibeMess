package com.vibe.ui.call

import android.content.Context
import java.util.UUID

object CallUtils {
    private const val PREFS_NAME = "vibe_call_prefs"
    private const val KEY_USER_ID = "user_id"

    fun getUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var uid = prefs.getString(KEY_USER_ID, null)
        if (uid == null) {
            uid = "vibe_${UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString(KEY_USER_ID, uid).apply()
        }
        return uid
    }

    fun getUserIdFromPrefs(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    fun setUserId(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }
}
