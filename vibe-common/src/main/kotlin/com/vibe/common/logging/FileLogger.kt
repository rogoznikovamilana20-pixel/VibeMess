package com.vibe.common.logging

import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * File-based implementation of [VibeLogger] for persistent logging.
 * Useful for debugging and crash reporting.
 */
class FileLogger(
    private val logFile: File,
    private val maxFileSizeBytes: Long = 5 * 1024 * 1024 // 5MB default
) : VibeLogger {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val lock = Any()

    override fun d(tag: String, message: String) {
        writeLog("DEBUG", tag, message)
    }

    override fun i(tag: String, message: String) {
        writeLog("INFO", tag, message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        writeLog("WARN", tag, fullMessage)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        writeLog("ERROR", tag, fullMessage)
    }

    private fun writeLog(level: String, tag: String, message: String) {
        synchronized(lock) {
            try {
                // Rotate log file if it exceeds max size
                if (logFile.exists() && logFile.length() > maxFileSizeBytes) {
                    rotateLogFile()
                }

                val timestamp = dateFormat.format(Date())
                val logLine = "$timestamp [$level] [$tag] $message\n"

                FileWriter(logFile, true).use { writer ->
                    writer.write(logLine)
                }
            } catch (e: Exception) {
                // Fallback to console if file logging fails
                System.err.println("Failed to write to log file: ${e.message}")
                System.err.println("Original log: [$level] [$tag] $message")
            }
        }
    }

    private fun rotateLogFile() {
        try {
            val rotatedFile = File(logFile.parent, "${logFile.name}.old")
            if (rotatedFile.exists()) {
                rotatedFile.delete()
            }
            logFile.renameTo(rotatedFile)
        } catch (e: Exception) {
            System.err.println("Failed to rotate log file: ${e.message}")
        }
    }

    /**
     * Clear the log file contents.
     */
    fun clearLogs() {
        synchronized(lock) {
            try {
                if (logFile.exists()) {
                    logFile.writeText("")
                }
            } catch (e: Exception) {
                System.err.println("Failed to clear log file: ${e.message}")
            }
        }
    }

    /**
     * Get the log file contents as a string.
     */
    fun getLogContents(): String {
        synchronized(lock) {
            return try {
                if (logFile.exists()) {
                    logFile.readText()
                } else {
                    ""
                }
            } catch (e: Exception) {
                "Error reading log file: ${e.message}"
            }
        }
    }
}
