package com.vibe.ui.e2e

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages encrypted session state persistence.
 * Ensures Forward Secrecy by encrypting session state at rest.
 */
class SessionManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "e2e_sessions",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val TAG = "SessionManager"
        private const val KEY_SESSION_PREFIX = "session_"
        private const val KEY_SESSION_EXPIRY_PREFIX = "session_expiry_"
        private const val SESSION_EXPIRY_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }

    /**
     * Save session state for a contact.
     */
    fun saveSession(contactUserId: String, state: RatchetState) {
        try {
            val sessionData = serializeState(state)
            val encryptedData = encryptSessionData(sessionData)

            prefs.edit()
                .putString("$KEY_SESSION_PREFIX$contactUserId", Base64.encodeToString(encryptedData, Base64.NO_WRAP))
                .putLong("$KEY_SESSION_EXPIRY_PREFIX$contactUserId", System.currentTimeMillis() + SESSION_EXPIRY_MS)
                .apply()

            Log.d(TAG, "Session saved for $contactUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session", e)
        }
    }

    /**
     * Load session state for a contact.
     * Returns null if session doesn't exist or has expired.
     */
    fun loadSession(contactUserId: String): RatchetState? {
        return try {
            val expiry = prefs.getLong("$KEY_SESSION_EXPIRY_PREFIX$contactUserId", 0)
            if (System.currentTimeMillis() > expiry) {
                // Session expired
                deleteSession(contactUserId)
                Log.d(TAG, "Session expired for $contactUserId")
                return null
            }

            val encryptedData = prefs.getString("$KEY_SESSION_PREFIX$contactUserId", null) ?: return null
            val sessionData = decryptSessionData(Base64.decode(encryptedData, Base64.NO_WRAP))
            deserializeState(sessionData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load session", e)
            null
        }
    }

    /**
     * Delete session for a contact.
     */
    fun deleteSession(contactUserId: String) {
        prefs.edit()
            .remove("$KEY_SESSION_PREFIX$contactUserId")
            .remove("$KEY_SESSION_EXPIRY_PREFIX$contactUserId")
            .apply()
        Log.d(TAG, "Session deleted for $contactUserId")
    }

    /**
     * Check if session exists and is valid.
     */
    fun hasValidSession(contactUserId: String): Boolean {
        val expiry = prefs.getLong("$KEY_SESSION_EXPIRY_PREFIX$contactUserId", 0)
        return System.currentTimeMillis() <= expiry && prefs.contains("$KEY_SESSION_PREFIX$contactUserId")
    }

    /**
     * Get all active session IDs.
     */
    fun getActiveSessions(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith(KEY_SESSION_PREFIX) && !it.startsWith(KEY_SESSION_EXPIRY_PREFIX) }
            .map { it.removePrefix(KEY_SESSION_PREFIX) }
            .filter { hasValidSession(it) }
    }

    /**
     * Clean up expired sessions.
     */
    fun cleanupExpiredSessions() {
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        var cleaned = 0

        prefs.all.keys
            .filter { it.startsWith(KEY_SESSION_EXPIRY_PREFIX) }
            .forEach { key ->
                val expiry = prefs.getLong(key, 0)
                if (now > expiry) {
                    val contactId = key.removePrefix(KEY_SESSION_EXPIRY_PREFIX)
                    editor.remove("$KEY_SESSION_PREFIX$contactId")
                    editor.remove(key)
                    cleaned++
                }
            }

        editor.apply()
        if (cleaned > 0) {
            Log.d(TAG, "Cleaned up $cleaned expired sessions")
        }
    }

    /**
     * Clear all sessions (on logout).
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "All sessions cleared")
    }

    // --- Encryption/Decryption ---

    /**
     * Encrypt session data using device key.
     * Uses Android KeyStore-backed AES-GCM encryption.
     */
    private fun encryptSessionData(data: ByteArray): ByteArray {
        val keyAlias = "e2e_session_key"

        // Check if key exists in KeyStore
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val secretKey = if (keyStore.containsAlias(keyAlias)) {
            val entry = keyStore.getEntry(keyAlias, null) as java.security.KeyStore.SecretKeyEntry
            entry.secretKey
        } else {
            // Generate new AES key
            val keyGen = javax.crypto.KeyGenerator.getInstance("AES")
            keyGen.init(256)
            keyGen.generateKey()
        }

        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(data)

        // Prepend IV to ciphertext
        val result = ByteArray(4 + iv.size + ciphertext.size)
        System.arraycopy(intToBytes(iv.size), 0, result, 0, 4)
        System.arraycopy(iv, 0, result, 4, iv.size)
        System.arraycopy(ciphertext, 0, result, 4 + iv.size, ciphertext.size)

        return result
    }

    /**
     * Decrypt session data using device key.
     */
    private fun decryptSessionData(data: ByteArray): ByteArray {
        val keyAlias = "e2e_session_key"

        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(keyAlias)) {
            throw IllegalStateException("Encryption key not found")
        }

        val entry = keyStore.getEntry(keyAlias, null) as java.security.KeyStore.SecretKeyEntry
        val secretKey = entry.secretKey

        val ivSize = bytesToInt(data, 0)
        val iv = data.copyOfRange(4, 4 + ivSize)
        val ciphertext = data.copyOfRange(4 + ivSize, data.size)

        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, javax.crypto.spec.GCMParameterSpec(128, iv))

        return cipher.doFinal(ciphertext)
    }

    // --- Serialization ---

    private fun serializeState(state: RatchetState): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Helper to write nullable byte array
        fun writeNullableBytes(bytes: ByteArray?) {
            dos.writeBoolean(bytes != null)
            bytes?.let { dos.writeInt(it.size); dos.write(it) }
        }

        // Write root key
        writeNullableBytes(state.rootKey)

        // Write chain keys
        writeNullableBytes(state.sendingChainKey)
        writeNullableBytes(state.receivingChainKey)

        // Write counters
        dos.writeInt(state.sendingRatchetCounter)
        dos.writeInt(state.receivingRatchetCounter)
        dos.writeInt(state.previousSendingRatchetCounter)

        // Write ratchet key pair
        dos.writeBoolean(state.currentRatchetKeyPair != null)
        state.currentRatchetKeyPair?.let {
            dos.writeInt(it.publicKey.size)
            dos.write(it.publicKey)
            dos.writeInt(it.privateKey.size)
            dos.write(it.privateKey)
        }

        // Write remote ratchet public key
        writeNullableBytes(state.remoteRatchetPublicKey)

        // Write skipped message keys
        dos.writeInt(state.skippedMessageKeys.size)
        state.skippedMessageKeys.forEach { (key, value) ->
            dos.writeUTF(key)
            dos.writeInt(value.size)
            dos.write(value)
        }

        return baos.toByteArray()
    }

    private fun deserializeState(data: ByteArray): RatchetState {
        val bais = ByteArrayInputStream(data)
        val dis = DataInputStream(bais)

        // Helper to read nullable byte array
        fun readNullableBytes(): ByteArray? {
            return if (dis.readBoolean()) {
                val size = dis.readInt()
                val bytes = ByteArray(size)
                dis.readFully(bytes)
                bytes
            } else null
        }

        // Read root key
        val rootKey = readNullableBytes()

        // Read chain keys
        val sendingChainKey = readNullableBytes()
        val receivingChainKey = readNullableBytes()

        // Read counters
        val sendingRatchetCounter = dis.readInt()
        val receivingRatchetCounter = dis.readInt()
        val previousSendingRatchetCounter = dis.readInt()

        // Read ratchet key pair
        val ratchetKeyPair = if (dis.readBoolean()) {
            val pubSize = dis.readInt()
            val pubKey = ByteArray(pubSize)
            dis.readFully(pubKey)
            val privSize = dis.readInt()
            val privKey = ByteArray(privSize)
            dis.readFully(privKey)
            RatchetKeyPair(publicKey = pubKey, privateKey = privKey)
        } else null

        // Read remote ratchet public key
        val remoteRatchetPublicKey = readNullableBytes()

        // Read skipped message keys
        val skippedCount = dis.readInt()
        val skippedKeys = HashMap<String, ByteArray>()
        repeat(skippedCount) {
            val key = dis.readUTF()
            val size = dis.readInt()
            val value = ByteArray(size)
            dis.readFully(value)
            skippedKeys[key] = value
        }

        return RatchetState(
            rootKey = rootKey,
            sendingChainKey = sendingChainKey,
            receivingChainKey = receivingChainKey,
            sendingRatchetCounter = sendingRatchetCounter,
            receivingRatchetCounter = receivingRatchetCounter,
            previousSendingRatchetCounter = previousSendingRatchetCounter,
            currentRatchetKeyPair = ratchetKeyPair,
            remoteRatchetPublicKey = remoteRatchetPublicKey,
            skippedMessageKeys = skippedKeys
        )
    }

    // --- Helpers ---

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }

    private fun bytesToInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
               ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
               ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
               (bytes[offset + 3].toInt() and 0xFF)
    }
}
