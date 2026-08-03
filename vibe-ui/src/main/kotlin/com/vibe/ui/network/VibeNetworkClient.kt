package com.vibe.ui.network

import com.vibe.common.logging.VibeLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

data class AuthResult(
    val success: Boolean = false,
    val userId: String = "",
    val username: String = "",
    val vibeId: String = "",
    val token: String = "",
    val error: String? = null
) {
    companion object {
        fun Success(userId: String, username: String, vibeId: String, token: String) =
            AuthResult(true, userId, username, vibeId, token)

        fun Error(message: String) = AuthResult(error = message)
    }
}

data class ChatData(
    val id: String,
    val title: String,
    val type: String
)

data class MessageData(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val type: String,
    val createdAt: String
)

class VibeNetworkClient(
    private val serverConfig: ServerConfig
) {
    private val tag = "VibeNetworkClient"
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var scope: CoroutineScope? = null
    private var reconnectAttempt = 0
    private var userId: String? = null
    private var authToken: String? = null

    private val _messages = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
    val messages: SharedFlow<JSONObject> = _messages.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun connect(connectScope: CoroutineScope) {
        if (_isConnected.value || _connectionState.value == ConnectionState.CONNECTING) return

        scope = connectScope
        connectScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                doConnect()
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.DISCONNECTED
                _isConnected.value = false
            }
        }
    }

    private fun doConnect() {
        val wsUrl = serverConfig.getRustWsUrl()

        val requestBuilder = Request.Builder()
            .url(wsUrl)
        if (authToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer $authToken")
        }
        val request = requestBuilder.build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED
                _isConnected.value = true
                reconnectAttempt = 0
                VibeLogger.d(tag, "WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    _messages.tryEmit(json)
                } catch (e: Exception) {
                    VibeLogger.e(tag, "Failed to parse WS message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                VibeLogger.e(tag, "WebSocket failure", t)
                _isConnected.value = false
                _connectionState.value = ConnectionState.ERROR
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        val delay = listOf(1000L, 2000L, 4000L, 8000L, 16000L, 30000L)
            .getOrElse(reconnectAttempt) { 60000L }
        reconnectAttempt++

        scope?.launch {
            try {
                delay(delay)
                if (!_isConnected.value) {
                    VibeLogger.d(tag, "Reconnecting (attempt $reconnectAttempt)")
                    doConnect()
                }
            } catch (e: Exception) {
                VibeLogger.e(tag, "Reconnect failed", e)
            }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closing")
        webSocket = null
        scope?.cancel()
        scope = null
        _isConnected.value = false
        _connectionState.value = ConnectionState.DISCONNECTED
        userId = null
        authToken = null
    }

    fun send(json: JSONObject): Boolean {
        return webSocket?.send(json.toString()) ?: false
    }

    fun setAuth(token: String, uid: String) {
        authToken = token
        userId = uid
    }
}
