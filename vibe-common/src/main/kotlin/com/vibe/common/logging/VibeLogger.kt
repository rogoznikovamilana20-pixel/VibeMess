package com.vibe.common.logging

import android.util.Log

/**
 * Interface for logging messages across the Vibe project.
 */
interface VibeLogger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)

    companion object : VibeLogger {
        private var logger: VibeLogger = LogcatLogger()

        fun setLogger(newLogger: VibeLogger) {
            logger = newLogger
        }

        override fun d(tag: String, message: String) = logger.d(tag, message)
        override fun i(tag: String, message: String) = logger.i(tag, message)
        override fun w(tag: String, message: String, throwable: Throwable?) = logger.w(tag, message, throwable)
        override fun e(tag: String, message: String, throwable: Throwable?) = logger.e(tag, message, throwable)
    }
}

/**
 * Android Logcat implementation of [VibeLogger].
 * Uses android.util.Log for production logging.
 */
class LogcatLogger : VibeLogger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}

/**
 * A basic console implementation of [VibeLogger].
 * For development/debugging without Android context.
 */
class ConsoleLogger : VibeLogger {
    override fun d(tag: String, message: String) {
        println("DEBUG: [$tag] $message")
    }

    override fun i(tag: String, message: String) {
        println("INFO: [$tag] $message")
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        println("WARN: [$tag] $message")
        throwable?.printStackTrace()
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        println("ERROR: [$tag] $message")
        throwable?.printStackTrace()
    }
}
