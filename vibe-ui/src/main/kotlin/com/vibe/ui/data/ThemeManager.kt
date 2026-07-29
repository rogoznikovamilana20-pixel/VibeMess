package com.vibe.ui.data

import android.content.Context
import android.content.SharedPreferences

class ThemeManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vibe_theme", Context.MODE_PRIVATE)

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    companion object {
        private const val KEY_DARK = "dark_theme"
    }
}
