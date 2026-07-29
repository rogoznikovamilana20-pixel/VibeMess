package com.vibe.ui.call

import com.vibe.common.logging.VibeLogger
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.SessionDescription.Type
import java.util.concurrent.TimeUnit

class SupabaseSignaling(
    private val projectUrl: String,
    private val anonKey: String,
    private val userId: String,
    private val onIncomingCall: (callerId: String, roomId: String) -> Unit,
    private val onRemoteSdp: (SessionDescription) -> Unit,
    private val onRemoteIce: (IceCandidate) -> Unit,
    private val onCallAccepted: () -> Unit
) {
    private val TAG = "SupabaseSignaling"
    var lastLocalFingerprint: String = ""
    var lastRemoteFingerprint: String = ""
    var isE2eeVerified: Boolean = false
    private val wsUrl = "$projectUrl/realtime/v1/websocket?apikey=$anonKey&vsn=2.0.0"
    private val httpUrl = "${projectUrl.replace("https", "wss")}/realtime/v1/websocket?apikey=$anonKey&vsn=2.0.0"

    private var webSocket: WebSocket? = null
    private var currentRoomId: String = ""
    private var refCounter = 1
    private var heartbeatJob: java.util.Timer? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            VibeLogger.d(TAG, "Supabase WS connected")
            joinUserChannel()
            startHeartbeat()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            VibeLogger.d(TAG, "Supabase WS closing: $reason")
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            VibeLogger.d(TAG, "Supabase WS closed")
            stopHeartbeat()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            VibeLogger.e(TAG, "Supabase WS failed: ${t.message}")
            stopHeartbeat()
            reconnect()
        }
    }

    fun connect() {
        if (projectUrl.isEmpty() || anonKey.isEmpty()) {
            VibeLogger.e(TAG, "Supabase not configured — set SUPABASE_URL and SUPABASE_ANON_KEY in local.properties")
            return
        }
        val request = Request.Builder()
            .url(httpUrl)
            .addHeader("apikey", anonKey)
            .build()
        webSocket = client.newWebSocket(request, socketListener)
    }

    fun callUser(targetUserId: String): String {
        val roomId = "room_${userId}_${targetUserId}_${System.currentTimeMillis()}"
        currentRoomId = roomId
        sendPhoenixMessage("user:$targetUserId", "call", JSONObject().apply {
            put("caller", userId)
            put("room", roomId)
        })
        return roomId
    }

    fun acceptCall(callerId: String, roomId: String) {
        currentRoomId = roomId
        sendPhoenixMessage("user:$userId", "call_accepted", JSONObject().apply {
            put("callee", userId)
            put("room", roomId)
        })
        onCallAccepted()
    }

    fun joinRoom(roomId: String) {
        currentRoomId = roomId
        sendPhoenixMessage("room:$roomId", "phx_join", JSONObject())
    }

    fun sendSdp(sdp: SessionDescription, targetUserId: String? = null) {
        val channel = targetUserId?.let { "user:$it" } ?: "room:$currentRoomId"
        val fingerprint = E2eeManager.extractFingerprint(sdp.description) ?: ""
        if (fingerprint.isNotEmpty()) {
            lastLocalFingerprint = fingerprint
        }
        sendPhoenixMessage(channel, "sdp", JSONObject().apply {
            put("type", if (sdp.type == Type.OFFER) "offer" else "answer")
            put("sdp", sdp.description)
            put("from", userId)
            put("fingerprint", fingerprint)
        })
    }

    fun sendIce(candidate: IceCandidate, targetUserId: String? = null) {
        val channel = targetUserId?.let { "user:$it" } ?: "room:$currentRoomId"
        sendPhoenixMessage(channel, "ice", JSONObject().apply {
            put("mid", candidate.sdpMid)
            put("index", candidate.sdpMLineIndex)
            put("candidate", candidate.sdp)
            put("from", userId)
        })
    }

    fun disconnect() {
        stopHeartbeat()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    private fun joinUserChannel() {
        sendPhoenixMessage("user:$userId", "phx_join", JSONObject())
    }

    private fun sendPhoenixMessage(channel: String, event: String, payload: JSONObject) {
        val ref = refCounter++
        val msg = JSONArray().apply {
            put(ref.toString())
            put(ref.toString())
            put(channel)
            put(event)
            put(payload)
        }
        VibeLogger.d(TAG, "WS >> $msg")
        webSocket?.send(msg.toString())
    }

    fun handleMessage(text: String) {
        VibeLogger.d(TAG, "WS << $text")
        try {
            val arr = JSONArray(text)
            val channel = arr.optString(2, "")
            val event = arr.optString(3, "")
            val payload = arr.optJSONObject(4) ?: JSONObject()

            handlePhoenixMessage(channel, event, payload)
        } catch (e: Exception) {
            VibeLogger.e(TAG, "Failed to parse message: ${e.message}")
        }
    }

    fun handlePhoenixMessage(channel: String, event: String, payload: JSONObject) {
        when {
            event == "call" -> {
                    val callerId = payload.optString("caller", "")
                    val roomId = payload.optString("room", "")
                    if (callerId.isNotBlank()) {
                        joinRoom(roomId)
                        onIncomingCall(callerId, roomId)
                    }
                }
                event == "call_accepted" -> {
                    val roomId = payload.optString("room", "")
                    joinRoom(roomId)
                    onCallAccepted()
                }
                event == "sdp" -> {
                    val type = payload.optString("type", "")
                    val sdp = payload.optString("sdp", "")
                    val remoteFp = payload.optString("fingerprint", "")
                    if (remoteFp.isNotEmpty()) {
                        lastRemoteFingerprint = remoteFp
                    }
                    if (type.isNotBlank() && sdp.isNotBlank()) {
                        val sdpType = if (type == "offer") Type.OFFER else Type.ANSWER
                        onRemoteSdp(SessionDescription(sdpType, sdp))
                    }
                }
                event == "ice" -> {
                    val mid = payload.optString("mid", "")
                    val index = payload.optInt("index", 0)
                    val candidate = payload.optString("candidate", "")
                    if (mid.isNotBlank()) {
                        onRemoteIce(IceCandidate(mid, index, candidate))
                    }
                }
                event == "phx_reply" -> {
                    // Room join confirmation
                }
                event == "user_offline" -> {
                    VibeLogger.d(TAG, "User ${payload.optString("user")} went offline")
                }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob = java.util.Timer("SupabaseHeartbeat", true).apply {
            schedule(object : java.util.TimerTask() {
                override fun run() {
                    sendPhoenixMessage("phoenix", "heartbeat", JSONObject())
                }
            }, 15000, 15000)
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private var reconnectAttempt = 0
    private var wsReconnecting = false

    private fun reconnect() {
        if (wsReconnecting) return
        wsReconnecting = true
        reconnectAttempt++
        val delay = minOf((1000L shl reconnectAttempt.coerceAtMost(6)), 60000L)
        VibeLogger.d(TAG, "WS reconnect in ${delay}ms (attempt $reconnectAttempt)")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            wsReconnecting = false
            connect()
        }, delay)
    }

    companion object {
        val SUPABASE_URL: String get() = com.vibe.ui.BuildConfig.SUPABASE_URL
        val SUPABASE_ANON_KEY: String get() = com.vibe.ui.BuildConfig.SUPABASE_ANON_KEY
    }
}
