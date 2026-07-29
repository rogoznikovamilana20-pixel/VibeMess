package com.vibe.ui.data.db

import android.content.Context
import com.vibe.common.logging.VibeLogger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for backing up and restoring Vibe database.
 * Provides automatic backup before migrations and manual backup/restore functionality.
 */
class DatabaseBackup(private val context: Context) {

    companion object {
        private const val TAG = "DatabaseBackup"
        private const val BACKUP_DIR = "vibe_backups"
        private const val MAX_BACKUP_COUNT = 5
        private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }

    private val backupDir = File(context.filesDir, BACKUP_DIR)

    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }

    /**
     * Create a backup of the current database.
     * @return true if backup was successful, false otherwise
     */
    fun createBackup(): Boolean {
        return try {
            val databaseFile = context.getDatabasePath("vibe_database")
            if (!databaseFile.exists()) {
                VibeLogger.w(TAG, "Database file does not exist, skipping backup")
                return false
            }

            val timestamp = dateFormat.format(Date())
            val backupFile = File(backupDir, "vibe_database_$timestamp.db")

            // Copy database file to backup location
            FileInputStream(databaseFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Clean up old backups
            cleanupOldBackups()

            VibeLogger.i(TAG, "Database backup created successfully: ${backupFile.name}")
            true
        } catch (e: Exception) {
            VibeLogger.e(TAG, "Failed to create database backup", e)
            false
        }
    }

    /**
     * Restore database from a specific backup file.
     * @param backupFile The backup file to restore from
     * @return true if restore was successful, false otherwise
     */
    fun restoreBackup(backupFile: File): Boolean {
        return try {
            if (!backupFile.exists()) {
                VibeLogger.e(TAG, "Backup file does not exist: ${backupFile.name}")
                return false
            }

            val databaseFile = context.getDatabasePath("vibe_database")
            
            // Close database connection (will be reopened automatically)
            VibeDatabase.resetInstance()

            // Create a backup of current database before restore
            val tempBackup = File(backupDir, "pre_restore_${System.currentTimeMillis()}.db")
            if (databaseFile.exists()) {
                FileInputStream(databaseFile).use { input ->
                    FileOutputStream(tempBackup).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Restore from backup
            FileInputStream(backupFile).use { input ->
                FileOutputStream(databaseFile).use { output ->
                    input.copyTo(output)
                }
            }

            VibeLogger.i(TAG, "Database restored successfully from: ${backupFile.name}")
            true
        } catch (e: Exception) {
            VibeLogger.e(TAG, "Failed to restore database backup", e)
            false
        }
    }

    /**
     * Get list of available backup files.
     * @return List of backup files sorted by modification time (newest first)
     */
    fun getAvailableBackups(): List<File> {
        return backupDir.listFiles { file ->
            file.name.startsWith("vibe_database_") && file.name.endsWith(".db")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Delete old backups, keeping only the most recent MAX_BACKUP_COUNT.
     */
    private fun cleanupOldBackups() {
        try {
            val backups = getAvailableBackups()
            if (backups.size > MAX_BACKUP_COUNT) {
                backups.drop(MAX_BACKUP_COUNT).forEach { backup ->
                    if (backup.delete()) {
                        VibeLogger.d(TAG, "Deleted old backup: ${backup.name}")
                    }
                }
            }
        } catch (e: Exception) {
            VibeLogger.e(TAG, "Failed to cleanup old backups", e)
        }
    }

    /**
     * Delete all backup files.
     */
    fun clearAllBackups(): Boolean {
        return try {
            getAvailableBackups().forEach { backup ->
                if (backup.delete()) {
                    VibeLogger.d(TAG, "Deleted backup: ${backup.name}")
                }
            }
            true
        } catch (e: Exception) {
            VibeLogger.e(TAG, "Failed to clear backups", e)
            false
        }
    }

    /**
     * Get total size of all backup files in bytes.
     */
    fun getTotalBackupSize(): Long {
        return getAvailableBackups().sumOf { it.length() }
    }
}
