package com.vibe.ui

import android.content.Context

/**
 * Application-context holder independent of the Telegram core.
 *
 * [init] must be called once from the entry activity (VibeActivity).
 * Used by UI/data layers so they do not depend on
 * `org.telegram.messenger.ApplicationLoader` (which is removed when the
 * Telegram core is dropped in the engine migration).
 */
object VibeAppContext {

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getOrNull(): Context? = appContext

    fun get(): Context = appContext
        ?: error("VibeAppContext is not initialized. Call VibeAppContext.init(context) first.")
}
