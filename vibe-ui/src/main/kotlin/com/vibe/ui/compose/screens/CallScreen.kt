package com.vibe.ui.compose.screens

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.vibe.ui.call.CallManager
import com.vibe.ui.call.E2eeManager
import com.vibe.ui.call.CallUtils
import com.vibe.ui.compose.components.VibeAvatar
import com.vibe.ui.data.AchievementManager
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallScreen(
    isVideoCall: Boolean = false,
    contactName: String = "Анна Смирнова",
    contactUserId: String? = null,
    incomingRoomId: String? = null,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    val userId = remember { CallUtils.getUserId(context) }

    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(isVideoCall) }
    var isCallActive by remember { mutableStateOf(false) }
    var isRinging by remember { mutableStateOf(incomingRoomId != null) }
    var remoteVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var currentRoomId by remember { mutableStateOf(incomingRoomId ?: "") }
    var e2eeVerified by remember { mutableStateOf(false) }
    var showE2eeDialog by remember { mutableStateOf(false) }
    var localFingerprint by remember { mutableStateOf("") }
    var remoteFingerprint by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }

    var hasAudioPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraPending by remember { mutableStateOf(false) }

    val callManager = remember {
        try {
            CallManager(context, userId).apply {
                initialize(object : CallManager.Callback {
                    override fun onCallStateChanged(state: CallManager.CallState) {
                        isCallActive = state == CallManager.CallState.CONNECTED
                        if (state == CallManager.CallState.CONNECTED) {
                            isRinging = false
                        }
                    }

                    override fun onRemoteVideoTrack(track: VideoTrack?) {
                        remoteVideoTrack = track
                    }

                    override fun onIncomingCall(callerId: String, roomId: String) {
                        currentRoomId = roomId
                        isRinging = true
                    }

                    override fun onE2eeStatusChanged(verified: Boolean, localFp: String, remoteFp: String, safetyNumber: String) {
                        e2eeVerified = verified
                        localFingerprint = localFp
                        remoteFingerprint = remoteFp
                        verificationCode = safetyNumber
                    }
                })
            }
        } catch (e: Exception) {
            com.vibe.common.logging.VibeLogger.e("CallScreen", "CallManager init failed", e)
            null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasAudioPermission = result[android.Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
        hasCameraPermission = result[android.Manifest.permission.CAMERA] ?: hasCameraPermission
        if (cameraPending && hasCameraPermission) {
            cameraPending = false
            callManager?.toggleVideo(true)
        }
    }

    suspend fun fetchTurnCredentials() {
        com.vibe.ui.network.VibeHttpClient(
            com.vibe.ui.network.ServerConfig(context)
        ).getTurnCredentials()?.let { creds ->
            creds.urls.firstOrNull()?.let { url ->
                callManager?.addTurnCredentials(url, creds.username, creds.credential)
            }
        }
    }

    // Start outgoing call
    LaunchedEffect(contactUserId, hasAudioPermission, hasCameraPermission) {
        if (contactUserId != null && incomingRoomId == null && currentRoomId.isEmpty()) {
            val needVideoPerm = isVideoCall && !hasCameraPermission
            if (hasAudioPermission && !needVideoPerm) {
                fetchTurnCredentials()
                currentRoomId = callManager?.callUser(contactUserId) ?: ""
            } else {
                val perms = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
                if (isVideoCall) perms.add(android.Manifest.permission.CAMERA)
                permissionLauncher.launch(perms.toTypedArray())
            }
        }
    }

    // Connect to incoming call room for SDP exchange
    LaunchedEffect(currentRoomId, hasAudioPermission) {
        if (incomingRoomId != null && currentRoomId.isNotEmpty() && hasAudioPermission) {
            fetchTurnCredentials()
            callManager?.acceptCall(currentRoomId, contactUserId ?: "")
        }
    }

    LaunchedEffect(isCallActive) {
        if (isCallActive) {
            AchievementManager(context).unlock(AchievementManager.Id.FIRST_CALL)
        }
    }

    DisposableEffect(Unit) {
        onDispose { callManager?.endCall() }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A0F2E), Color(0xFF0C081A)),
                        center = Offset(0.5f, 0.4f),
                        radius = 1.3f
                    )
                )
        )

        // Remote video
        if (isVideoEnabled && isCallActive) {
            remoteVideoTrack?.let { track ->
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            setMirror(false)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            track.addSink(this)
                        }
                    },
                    update = { renderer -> track.addSink(renderer) }
                )
            }
        }

        // Local preview (PiP)
        if (isVideoEnabled && isCallActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 120.dp, end = 16.dp)
                    .size(120.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                AndroidView(
                    modifier = Modifier.size(120.dp),
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            setMirror(true)
                            setZOrderMediaOverlay(true)
                            layoutParams = ViewGroup.LayoutParams(120, 120)
                        }
                    }
                )
            }
        }

        // Contact info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = if (isCallActive && isVideoEnabled) 40.dp else 60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isRinging) {
                    com.vibe.ui.compose.components.PulseRing(
                        modifier = Modifier.size(220.dp),
                        color = Color(0xFF8D2BFA)
                    )
                }
                VibeAvatar(
                    name = contactName,
                    size = if (isCallActive && isVideoEnabled) 56.dp else 180.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = contactName,
                style = if (isCallActive && isVideoEnabled) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when {
                    isRinging -> "Входящий вызов..."
                    isCallActive -> "Подключено"
                    else -> if (isVideoCall) "Видеозвонок..." else "Аудиозвонок..."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFA8A3B8)
            )

            if (isCallActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    onClick = { showE2eeDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (e2eeVerified) Color(0xFF4ADE80).copy(alpha = 0.15f)
                        else Color(0xFFF59E0B).copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (e2eeVerified) Icons.Default.Lock else Icons.Default.LockOpen,
                            null, tint = if (e2eeVerified) Color(0xFF4ADE80) else Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (e2eeVerified) "E2EE • Подтверждено" else "E2EE • Проверить",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (e2eeVerified) Color(0xFF4ADE80) else Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }

        // Bottom controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            if (isRinging) {
                // Accept call
                CallControlButton(
                    icon = Icons.Default.Call,
                    label = "Ответить",
                    onClick = {
                        callManager?.acceptCall(currentRoomId, contactUserId ?: "")
                        isRinging = false
                    }
                )

                // Decline call
                CallControlButton(
                    icon = Icons.Default.CallEnd,
                    label = "Отклонить",
                    isEndCall = true,
                    onClick = {
                        callManager?.endCall()
                        onEndCall()
                    }
                )
            } else if (isCallActive) {
                // Mute
                CallControlButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMuted) "Выкл" else "Микрофон",
                    isActive = isMuted,
                    onClick = {
                        isMuted = !isMuted
                        callManager?.toggleMute(isMuted)
                    }
                )

                // Speaker
                CallControlButton(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Динамик",
                    isActive = isSpeakerOn,
                    onClick = {
                        isSpeakerOn = !isSpeakerOn
                        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                        audio?.mode = if (isSpeakerOn) AudioManager.MODE_IN_COMMUNICATION else AudioManager.MODE_NORMAL
                        audio?.isSpeakerphoneOn = isSpeakerOn
                    }
                )

                // End call
                CallControlButton(
                    icon = Icons.Default.CallEnd,
                    label = "Завершить",
                    isEndCall = true,
                    onClick = {
                        callManager?.endCall()
                        onEndCall()
                    }
                )

                // Video toggle
                if (isVideoCall) {
                    CallControlButton(
                        icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = if (isVideoEnabled) "Видео" else "Без видео",
                        isActive = !isVideoEnabled,
                        onClick = {
                            isVideoEnabled = !isVideoEnabled
                            if (isVideoEnabled && !hasCameraPermission) {
                                cameraPending = true
                                permissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
                            } else {
                                callManager?.toggleVideo(isVideoEnabled)
                            }
                        }
                    )

                    CallControlButton(
                        icon = Icons.Default.Cameraswitch,
                        label = "Камера",
                        onClick = { callManager?.switchCamera() }
                    )
                }
            } else {
                // Cancelling
                CallControlButton(
                    icon = Icons.Default.CallEnd,
                    label = "Отмена",
                    isEndCall = true,
                    onClick = {
                        callManager?.endCall()
                        onEndCall()
                    }
                )
            }
        }

        // E2EE verification dialog
        if (showE2eeDialog) {
            AlertDialog(
                onDismissRequest = { showE2eeDialog = false },
                title = { Text("E2EE: Сквозное шифрование") },
                text = {
                    Column {
                        Text(
                            "Код безопасности: ${verificationCode.ifEmpty { "—" }}",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Сравните этот код с кодом на устройстве собеседника.", 
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Локальный отпечаток:",
                            style = MaterialTheme.typography.labelSmall)
                        Text(localFingerprint.ifEmpty { "—" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Удалённый отпечаток:",
                            style = MaterialTheme.typography.labelSmall)
                        Text(remoteFingerprint.ifEmpty { "—" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                callManager?.confirmE2ee()
                                showE2eeDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CF6)
                            )
                        ) {
                            Text("Верифицировать")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showE2eeDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    isEndCall: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isEndCall) Color(0xFFEF4444)
                else if (isActive) MaterialTheme.colorScheme.primary
                else Color(0xFF2A1F3E)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, label,
                    tint = if (isEndCall || isActive) Color.White else Color(0xFFA8A3B8),
                    modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFA8A3B8),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center)
    }
}
