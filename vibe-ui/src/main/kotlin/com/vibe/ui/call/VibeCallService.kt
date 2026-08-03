package com.vibe.ui.call

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.vibe.common.logging.VibeLogger
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class VibeCallService : Service() {

    private val TAG = "VibeCallService"
    private var signaling: SupabaseSignaling? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (hasMicPermission) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                VibeLogger.w(TAG, "RECORD_AUDIO not granted, starting FGS without mic type")
                ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
            }
            startForeground(NOTIFICATION_ID, buildNotification("Ожидание звонков"), type)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification("Ожидание звонков"))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val userId = intent.getStringExtra(EXTRA_USER_ID)
                if (userId == null) {
                    // Process was restarted with a null intent (START_STICKY) —
                    // restore the last known user from prefs instead of idling.
                    val restored = CallUtils.getUserIdFromPrefs(this)
                    if (restored.isNotBlank()) {
                        connectSignaling(restored)
                    }
                    return START_STICKY
                }
                connectSignaling(userId)
            }
            ACTION_STOP -> {
                disconnectSignaling()
                @Suppress("InlinedApi")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                // Null intent after process restart.
                val restored = CallUtils.getUserIdFromPrefs(this)
                if (restored.isNotBlank()) {
                    connectSignaling(restored)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectSignaling(userId: String) {
        disconnectSignaling()
        signaling = try {
            SupabaseSignaling(
                projectUrl = SupabaseSignaling.SUPABASE_URL,
                anonKey = SupabaseSignaling.SUPABASE_ANON_KEY,
                userId = userId,
                onIncomingCall = { callerId, roomId ->
                    VibeLogger.d(TAG, "Incoming call from $callerId")
                    showIncomingCallNotification(callerId, roomId)
                },
                onRemoteSdp = {},
                onRemoteIce = {},
                onCallAccepted = {}
            )
        } catch (e: Exception) {
            VibeLogger.e(TAG, "signaling init failed", e)
            null
        }
        try {
            signaling?.connect()
        } catch (e: Exception) {
            VibeLogger.e(TAG, "signaling connect failed", e)
        }
    }

    private fun disconnectSignaling() {
        signaling?.disconnect()
        signaling = null
    }

    private fun showIncomingCallNotification(callerId: String, roomId: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val answerIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("callerId", callerId)
            putExtra("roomId", roomId)
            putExtra("action", "answer")
        }

        val declineIntent = Intent(this, IncomingCallActivity::class.java).apply {
            putExtra("callerId", callerId)
            putExtra("roomId", roomId)
            putExtra("action", "decline")
        }

        val answerPendingIntent = PendingIntent.getActivity(
            this, 0, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declinePendingIntent = PendingIntent.getActivity(
            this, 1, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setContentTitle("Входящий звонок Vibe")
            .setContentText("Звонок от $callerId")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(answerPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_call, "Ответить", answerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отклонить", declinePendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, notification)

        updateForegroundNotification("Входящий звонок от $callerId")
    }

    private fun updateForegroundNotification(text: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("Vibe")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID, "Vibe фон", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление фоновой работы Vibe"
            }
            notificationManager.createNotificationChannel(serviceChannel)

            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID, "Vibe звонки", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Входящие звонки Vibe"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(callChannel)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val INCOMING_CALL_NOTIFICATION_ID = 1002
        const val SERVICE_CHANNEL_ID = "vibe_service"
        const val CALL_CHANNEL_ID = "vibe_calls"

        const val ACTION_START = "com.vibe.ui.call.START"
        const val ACTION_STOP = "com.vibe.ui.call.STOP"
        const val EXTRA_USER_ID = "user_id"

        fun start(context: Context, userId: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                VibeLogger.w("VibeCallService", "RECORD_AUDIO not granted — cannot start call service")
                return
            }
            val intent = Intent(context, VibeCallService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_USER_ID, userId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VibeCallService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
