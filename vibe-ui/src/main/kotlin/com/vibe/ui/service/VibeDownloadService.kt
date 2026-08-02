package com.vibe.ui.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.R

class VibeDownloadService : Service() {

    companion object {
        private const val TAG = "VibeDownloadService"
        private const val CHANNEL_ID = "vibe_downloads"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_DOWNLOAD = "com.vibe.ui.action.DOWNLOAD"
        const val ACTION_CANCEL = "com.vibe.ui.action.CANCEL"
        const val EXTRA_FILE_ID = "file_id"
        const val EXTRA_FILE_NAME = "file_name"

        fun startDownload(context: Context, fileId: String, fileName: String) {
            val intent = Intent(context, VibeDownloadService::class.java).apply {
                action = ACTION_DOWNLOAD
                putExtra(EXTRA_FILE_ID, fileId)
                putExtra(EXTRA_FILE_NAME, fileName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification("Подготовка...", 0).build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val fileId = intent.getStringExtra(EXTRA_FILE_ID) ?: return START_STICKY
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: fileId
                VibeLogger.d(TAG, "Download started: $fileName ($fileId)")
                updateNotification(fileName, 0)
            }
            ACTION_CANCEL -> {
                VibeLogger.d(TAG, "Download cancelled")
                safeStopForeground()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun updateProgress(fileName: String, progress: Int) {
        updateNotification(fileName, progress)
    }

    fun onDownloadComplete(fileName: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Загрузка завершена")
            .setContentText(fileName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID + 1, notification)

        safeStopForeground()
        stopSelf()
    }

    private fun safeStopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Загрузки Vibe",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о загрузке файлов"
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(fileName: String, progress: Int = 0): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Загрузка...")
            .setContentText(fileName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
    }

    private fun updateNotification(fileName: String, progress: Int) {
        val notification = buildNotification(fileName, progress).build()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }
}
