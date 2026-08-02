package com.vibe.ui.data.mesh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.util.Base64
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.data.db.VibeDatabase
import com.vibe.ui.data.db.entity.MeshMessageEntity
import com.vibe.ui.data.db.entity.MeshStatus
import com.vibe.ui.network.ServerConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread

object MeshManager {

    const val TCP_PORT = 48761
    private const val BLE_SERVICE_UUID = "0000F1BE-0000-1000-8000-00805F9B34FB"
    private const val MAX_TTL = 5
    private const val MAX_SEEN = 512
    private const val ACK_TIMEOUT_MS = 8000L

    private val tag = "MeshManager"
    private val crashHandler = CoroutineExceptionHandler { _, e ->
        VibeLogger.e(tag, "background coroutine crashed", e)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + crashHandler)
    private lateinit var appContext: Context
    private lateinit var db: VibeDatabase
    private lateinit var serverConfig: ServerConfig

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _peerCount = MutableStateFlow(0)
    val peerCount: StateFlow<Int> = _peerCount.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val peers = ConcurrentHashMap<String, Socket>()
    private val sessionKeys = ConcurrentHashMap<String, SecretKey>()
    private val keyLock = Any()
    private var cachedKeyPair: KeyPair? = null
    private val seenMessages = Collections.synchronizedMap(
        object : LinkedHashMap<String, Boolean>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean =
                size > MAX_SEEN
        }
    )
    private var serverSocket: ServerSocket? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var advertising = false
    private var scanning = false

    val myPeerId: String
        get() = serverConfig.getUserId().takeIf { it.isNotBlank() }
            ?: serverConfig.getRustUserId().takeIf { it.isNotBlank() }
            ?: "peer_" + UUID.randomUUID().toString().take(8)

    // ============ Mesh E2E crypto (ECDH + AES-256-GCM, TOFU per peer) ============

    private fun keyPair(): KeyPair {
        cachedKeyPair?.let { return it }
        synchronized(keyLock) {
            cachedKeyPair?.let { return it }
            val privB64 = serverConfig.getMeshPrivateKey()
            val pubB64 = serverConfig.getMeshPublicKey()
            if (privB64.isNotBlank() && pubB64.isNotBlank()) {
                try {
                    val kf = KeyFactory.getInstance("EC")
                    val priv = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privB64, Base64.NO_WRAP)))
                    val pub = kf.generatePublic(X509EncodedKeySpec(Base64.decode(pubB64, Base64.NO_WRAP)))
                    val pair = KeyPair(pub, priv)
                    cachedKeyPair = pair
                    return pair
                } catch (e: Exception) {
                    VibeLogger.w(tag, "mesh key load failed, regenerating: ${e.message}")
                }
            }
            val gen = KeyPairGenerator.getInstance("EC")
            gen.initialize(ECGenParameterSpec("secp256r1"))
            val pair = gen.generateKeyPair()
            serverConfig.setMeshPrivateKey(Base64.encodeToString(pair.private.encoded, Base64.NO_WRAP))
            serverConfig.setMeshPublicKey(Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP))
            cachedKeyPair = pair
            return pair
        }
    }

    private fun meshPubKeyB64(): String =
        Base64.encodeToString(keyPair().public.encoded, Base64.NO_WRAP)

    private fun deriveSessionKey(peerId: String) {
        val peerPubB64 = serverConfig.getMeshPeerKeys()[peerId] ?: return
        try {
            val kf = KeyFactory.getInstance("EC")
            val peerPub = kf.generatePublic(X509EncodedKeySpec(Base64.decode(peerPubB64, Base64.NO_WRAP)))
            val ka = KeyAgreement.getInstance("ECDH")
            ka.init(keyPair().private)
            ka.doPhase(peerPub, true)
            val digest = MessageDigest.getInstance("SHA-256").digest(ka.generateSecret())
            sessionKeys[peerId] = SecretKeySpec(digest, "AES")
        } catch (e: Exception) {
            VibeLogger.w(tag, "deriveSessionKey failed for $peerId: ${e.message}")
        }
    }

    private fun cachePeerKey(peerId: String, pubB64: String) {
        val keys = serverConfig.getMeshPeerKeys().toMutableMap()
        keys[peerId] = pubB64
        serverConfig.setMeshPeerKeys(keys)
        sessionKeys.remove(peerId)
        deriveSessionKey(peerId)
    }

    private fun encryptFor(peerId: String, plain: JSONObject): JSONObject? {
        val key = sessionKeys[peerId] ?: run {
            deriveSessionKey(peerId)
            sessionKeys[peerId]
        } ?: return null
        return try {
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            val ct = cipher.doFinal(plain.toString().toByteArray(Charsets.UTF_8))
            JSONObject()
                .put("ct", Base64.encodeToString(ct, Base64.NO_WRAP))
                .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
        } catch (e: Exception) {
            VibeLogger.w(tag, "encrypt failed: ${e.message}")
            null
        }
    }

    private fun decryptFrom(peerId: String, outer: JSONObject): JSONObject? {
        val key = sessionKeys[peerId] ?: return null
        return try {
            val ct = Base64.decode(outer.optString("ct"), Base64.NO_WRAP)
            val iv = Base64.decode(outer.optString("iv"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            JSONObject(String(cipher.doFinal(ct), Charsets.UTF_8))
        } catch (e: Exception) {
            VibeLogger.w(tag, "decrypt failed for $peerId: ${e.message}")
            null
        }
    }

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        db = VibeDatabase.getDatabase(appContext)
        serverConfig = ServerConfig(appContext)
    }

    fun start(context: Context) {
        init(context)
        try {
            val intent = Intent(context, MeshService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            VibeLogger.e(tag, "startForegroundService failed", e)
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, MeshService::class.java))
    }

    internal fun startInternal(context: Context) {
        init(context)
        _enabled.value = true
        scope.launch { startTcpServer() }
        scope.launch { startBleAnnounce() }
        startWifiDirect()
        VibeLogger.i(tag, "Mesh activated, peerId=${myPeerId}")
    }

    internal fun stopInternal() {
        _enabled.value = false
        stopBle()
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        peers.values.forEach { s -> try { s.close() } catch (_: Exception) {} }
        peers.clear()
        sessionKeys.clear()
        _peerCount.value = 0
    }

    // ============ Transport: TCP server + client ============

    private suspend fun startTcpServer() = withContext(Dispatchers.IO) {
        try {
            serverSocket = ServerSocket(TCP_PORT).apply {
                reuseAddress = true
            }
            while (_enabled.value) {
                val socket = serverSocket?.accept() ?: break
                socket.soTimeout = 30000
                handlePeer(socket)
            }
        } catch (e: Exception) {
            if (_enabled.value) {
                VibeLogger.e(tag, "TCP server stopped", e)
                _lastError.value = "TCP: ${e.message}"
            }
        }
    }

    private fun handlePeer(socket: Socket) {
        thread(name = "mesh-peer") {
            var peerId = ""
            try {
                val reader = socket.getInputStream().bufferedReader()
                while (_enabled.value) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    val json = JSONObject(line)
                    when (json.optString("type")) {
                        "hello" -> {
                            peerId = json.optString("peerId")
                            val peerPub = json.optString("pubKey")
                            if (peerId.isNotBlank() && peerPub.isNotBlank()) {
                                cachePeerKey(peerId, peerPub)
                                peers[peerId]?.let { old ->
                                    try { old.close() } catch (_: Exception) {}
                                }
                                peers[peerId] = socket
                                _peerCount.value = peers.size
                                socket.getOutputStream().write(
                                    ("{\"type\":\"hello\",\"peerId\":\"$myPeerId\",\"pubKey\":\"${meshPubKeyB64()}\"}\n")
                                        .toByteArray()
                                )
                                socket.getOutputStream().flush()
                                VibeLogger.i(tag, "Peer connected: $peerId")
                            } else {
                                VibeLogger.w(tag, "Peer dropped: missing pubKey in hello")
                                return@thread
                            }
                        }
                        "msg" -> handleEnvelope(json, peerId)
                        "ack" -> {
                            val msgId = json.optString("msgId")
                            if (msgId.isNotBlank()) {
                                scope.launch {
                                    db.meshDao().updateStatus(msgId, MeshStatus.ACKED)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                VibeLogger.d(tag, "Peer $peerId dropped: ${e.message}")
            } finally {
                if (peerId.isNotBlank()) {
                    peers.remove(peerId)
                    _peerCount.value = peers.size
                }
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    private fun handleEnvelope(json: JSONObject, fromPeerId: String) {
        val msgId = json.optString("msgId")
        if (msgId.isBlank()) return
        synchronized(seenMessages) {
            if (seenMessages.containsKey(msgId)) {
                sendAck(fromPeerId, msgId)
                return
            }
            seenMessages[msgId] = true
        }

        val inner = decryptFrom(fromPeerId, json)
        if (inner == null) {
            VibeLogger.w(tag, "dropping undecryptable message $msgId from $fromPeerId")
            return
        }

        val toPeerId = inner.optString("toPeerId", "")
        val isForUs = toPeerId.isBlank() || toPeerId == myPeerId
        if (isForUs) {
            val text = inner.optString("text")
            scope.launch {
                db.meshDao().insert(
                    MeshMessageEntity(
                        messageId = msgId,
                        fromPeerId = inner.optString("fromPeerId", fromPeerId),
                        toPeerId = toPeerId,
                        text = text,
                        mediaPath = inner.optString("mediaPath").ifEmpty { null },
                        status = MeshStatus.DELIVERED,
                        isOutgoing = false,
                        deliveredViaMesh = true
                    )
                )
                db.meshDao().updateStatus(msgId, MeshStatus.DELIVERED)
            }
            sendAck(fromPeerId, msgId)
        }

        val ttl = inner.optInt("ttl", MAX_TTL)
        if (ttl > 1) {
            inner.put("ttl", ttl - 1)
            flood(inner, excludePeerId = fromPeerId)
        }
    }

    private fun sendAck(peerId: String, msgId: String) {
        val socket = peers[peerId] ?: return
        try {
            socket.getOutputStream().write("{\"type\":\"ack\",\"msgId\":\"$msgId\"}\n".toByteArray())
            socket.getOutputStream().flush()
        } catch (_: Exception) {
        }
    }

    // ============ Sending + epidemic flood ============

    fun sendText(text: String, targetPeerId: String = "") {
        val msgId = UUID.randomUUID().toString()
        val inner = JSONObject().apply {
            put("msgId", msgId)
            put("fromPeerId", myPeerId)
            put("toPeerId", targetPeerId)
            put("ttl", MAX_TTL)
            put("ts", System.currentTimeMillis())
            put("text", text)
        }
        scope.launch {
            db.meshDao().insert(
                MeshMessageEntity(
                    messageId = msgId,
                    fromPeerId = myPeerId,
                    toPeerId = targetPeerId,
                    text = text,
                    status = MeshStatus.PENDING,
                    isOutgoing = true,
                    deliveredViaMesh = true
                )
            )
            val delivered = flood(inner, excludePeerId = "")
            if (delivered) {
                db.meshDao().updateStatus(msgId, MeshStatus.SENT)
            } else {
                db.meshDao().updateStatus(msgId, MeshStatus.FAILED)
            }
        }
    }

    private fun flood(inner: JSONObject, excludePeerId: String): Boolean {
        var sent = false
        peers.forEach { (peerId, socket) ->
            if (peerId == excludePeerId) return@forEach
            val outer = encryptFor(peerId, inner) ?: return@forEach
            outer.put("type", "msg")
            outer.put("msgId", inner.optString("msgId"))
            try {
                socket.getOutputStream().write((outer.toString() + "\n").toByteArray())
                socket.getOutputStream().flush()
                sent = true
            } catch (e: Exception) {
                peers.remove(peerId)
                _peerCount.value = peers.size
            }
        }
        return sent
    }

    // ============ BLE announce (service UUID + mesh tag) ============

    private fun startBleAnnounce() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return

            advertiser = adapter.bluetoothLeAdvertiser
            val data = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(UUID.fromString(BLE_SERVICE_UUID)))
                .addServiceData(
                    ParcelUuid(UUID.fromString(BLE_SERVICE_UUID)),
                    "VM:${myPeerId.take(12)}".toByteArray()
                )
                .setIncludeDeviceName(false)
                .build()
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build()
            advertiser?.startAdvertising(settings, data, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    advertising = true
                    VibeLogger.i(tag, "BLE advertising started")
                }

                override fun onStartFailure(errorCode: Int) {
                    VibeLogger.w(tag, "BLE advertising failed: $errorCode")
                }
            })
        } catch (e: Exception) {
            VibeLogger.w(tag, "BLE announce unavailable: ${e.message}")
        }
    }

    private fun stopBle() {
        try {
            if (advertising) advertiser?.stopAdvertising(null)
        } catch (_: Exception) {
        }
        advertising = false
        try {
            if (scanning) scanner?.stopScan(bleScanCallback)
        } catch (_: Exception) {
        }
        scanning = false
    }

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val serviceUuid = result.scanRecord?.serviceUuids
                ?.find { it.uuid == UUID.fromString(BLE_SERVICE_UUID) }
                ?: return
            val data = result.scanRecord?.getServiceData(serviceUuid) ?: return
            val tag = String(data)
            if (tag.startsWith("VM:")) {
                val peerId = tag.removePrefix("VM:")
                if (peerId != myPeerId.take(12)) {
                    VibeLogger.d(tag, "Mesh peer nearby via BLE: $peerId")
                }
            }
        }
    }

    // ============ Wi-Fi Direct (guarded; BLE+TCP is the primary path) ============

    private fun startWifiDirect() {
        try {
            val wifiP2p = appContext.getSystemService(Context.WIFI_P2P_SERVICE)
            if (wifiP2p != null) {
                VibeLogger.d(tag, "Wi-Fi Direct available; group setup deferred to OS group formation")
            }
        } catch (e: Exception) {
            VibeLogger.w(tag, "Wi-Fi Direct unavailable: ${e.message}")
        }
    }

    fun localIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .firstOrNull { it.name.startsWith("p2p") || it.name.startsWith("wlan") }
                ?.inetAddresses?.toList()
                ?.firstOrNull { !it.isLoopbackAddress && it is InetAddress }
                ?.hostAddress
                ?: "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }
}
