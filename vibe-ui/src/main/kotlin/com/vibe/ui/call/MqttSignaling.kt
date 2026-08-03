package com.vibe.ui.call

import android.content.Context
import com.vibe.common.logging.VibeLogger
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.SessionDescription.Type
import java.util.UUID

class MqttSignaling(
    private val context: Context,
    private val onRemoteSdp: (SessionDescription) -> Unit,
    private val onRemoteIce: (IceCandidate) -> Unit,
    private val onRemoteRing: () -> Unit
) {
    private val TAG = "MqttSignaling"
    private val BROKER = "ssl://broker.emqx.io:8883"
    private val CLIENT_ID = "vibe_${UUID.randomUUID().toString().take(8)}"

    private var client: MqttClient? = null
    private var roomId: String = ""
    private var isCaller: Boolean = false
    private var reconnectAttempt = 0
    private val maxReconnectDelay = 60000L

    private val callback = object : MqttCallback {
        override fun connectionLost(cause: Throwable?) {
            VibeLogger.w(TAG, "MQTT disconnected: ${cause?.message}")
            reconnectWithBackoff()
        }

        override fun messageArrived(topic: String, message: MqttMessage) {
            val payload = String(message.payload)
            VibeLogger.d(TAG, "MQTT << $topic: $payload")

            when {
                topic.endsWith("/sdp") -> {
                    val parts = payload.split("|", limit = 2)
                    if (parts.size == 2) {
                        val type = if (parts[0] == "offer") Type.OFFER
                        else if (parts[0] == "answer") Type.ANSWER
                        else return
                        onRemoteSdp(SessionDescription(type, parts[1]))
                    }
                }
                topic.endsWith("/ice") -> {
                    val parts = payload.split("|", limit = 3)
                    if (parts.size == 3) {
                        onRemoteIce(IceCandidate(parts[0], parts[1].toInt(), parts[2]))
                    }
                }
                topic.endsWith("/ring") -> {
                    onRemoteRing()
                }
            }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {}
    }

    fun connect(roomId: String, caller: Boolean, @Suppress("UNUSED_PARAMETER") timeout: Int = 30000) {
        this.roomId = roomId
        this.isCaller = caller

        try {
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 30
            }

            client = MqttClient(BROKER, CLIENT_ID, MemoryPersistence()).apply {
                setCallback(callback)
                connect(options)

                subscribe("\$share/vibe/${roomId}/sdp", 2)
                subscribe("\$share/vibe/${roomId}/ice", 2)
                subscribe("\$share/vibe/${roomId}/ring", 2)
            }

            VibeLogger.d(TAG, "MQTT connected to room: $roomId")
        } catch (e: MqttException) {
            VibeLogger.e(TAG, "MQTT connect failed: ${e.message}")
        }
    }

    fun sendSdp(sdp: SessionDescription) {
        val type = if (sdp.type == Type.OFFER) "offer" else "answer"
        val payload = "$type|${sdp.description}"
        publish("vibe/${roomId}/sdp", payload)
    }

    fun sendIce(candidate: IceCandidate) {
        val payload = "${candidate.sdpMid}|${candidate.sdpMLineIndex}|${candidate.sdp}"
        publish("vibe/${roomId}/ice", payload)
    }

    fun sendRing() {
        publish("vibe/${roomId}/ring", "ring")
    }

    fun disconnect() {
        try {
            client?.disconnect()
            client?.close()
        } catch (e: Exception) {
            VibeLogger.e(TAG, "disconnect failed", e)
        }
        client = null
    }

    private fun publish(topic: String, payload: String) {
        try {
            client?.publish(topic, MqttMessage(payload.toByteArray()).apply { qos = 2 })
        } catch (e: MqttException) {
            VibeLogger.e(TAG, "MQTT publish failed: ${e.message}")
        }
    }

    private fun reconnectWithBackoff() {
        reconnectAttempt++
        val delay = minOf((1000L shl reconnectAttempt.coerceAtMost(6)), maxReconnectDelay)
        VibeLogger.d(TAG, "Reconnecting in ${delay}ms (attempt $reconnectAttempt)")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                client?.connect()
                subscribeAll()
                reconnectAttempt = 0
            } catch (e: Exception) {
                VibeLogger.e(TAG, "reconnect failed (attempt $reconnectAttempt)", e)
            }
        }, delay)
    }

    private fun subscribeAll() {
        try {
            client?.subscribe("\$share/vibe/${roomId}/sdp", 2)
            client?.subscribe("\$share/vibe/${roomId}/ice", 2)
            client?.subscribe("\$share/vibe/${roomId}/ring", 2)
        } catch (e: Exception) {
            VibeLogger.e(TAG, "subscribeAll failed", e)
        }
    }

    companion object {
        fun generateRoomId(): String = UUID.randomUUID().toString().take(12)
    }
}
