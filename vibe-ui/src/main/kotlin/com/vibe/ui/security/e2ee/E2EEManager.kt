package com.vibe.ui.security.e2ee

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vibe.ui.security.signal.DoubleRatchet
import com.vibe.ui.security.signal.EncryptedMessage
import com.vibe.ui.security.signal.IdentityKeyPair
import com.vibe.ui.security.signal.SignalSession
import com.vibe.ui.security.signal.X25519KeyExchange
import com.vibe.ui.security.signal.KeyPair as X25519KeyPair

/**
 * ⚠️ DISABLED — DO NOT USE.
 *
 * Previous E2EE implementation. The underlying signal code is mathematically broken:
 * - Uses secp256r1 instead of X25519
 * - Incorrect HKDF implementation
 * - Sessions never persisted
 *
 * SecurityManager.initialize() does NOT call this class.
 * Will be replaced with libsignal-client or a vetted E2EE library in a future phase.
 */
@Deprecated("Broken E2EE implementation — SecurityManager does not use this class. Will be replaced.")
class E2EEManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "vibe_e2ee_state"
        private const val KEY_PUBLIC = "identity_public_key"
        private const val KEY_PRIVATE = "identity_private_key"
    }

    private val keyExchange = X25519KeyExchange()
    private val signalSession = SignalSession(context)

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val e2eePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private var cachedIdentityKeyPair: IdentityKeyPair? = null

    /**
     * Initialize E2EE - generate identity keys if not exists.
     */
    fun initialize() {
        if (!hasIdentityKey()) {
            generateIdentityKey()
        }
    }

    /**
     * Generate identity key pair using X25519.
     */
    private fun generateIdentityKey() {
        val keyPair = keyExchange.generateKeyPair()
        e2eePrefs.edit()
            .putString(KEY_PUBLIC, keyPair.publicKey.joinToString(",") { it.toInt().toString() })
            .putString(KEY_PRIVATE, keyPair.privateKey.joinToString(",") { it.toInt().toString() })
            .apply()
        cachedIdentityKeyPair = IdentityKeyPair(keyPair.publicKey, keyPair.privateKey)
    }

    /**
     * Get stored identity key pair.
     */
    private fun getIdentityKeyPair(): IdentityKeyPair? {
        cachedIdentityKeyPair?.let { return it }

        val pubStr = e2eePrefs.getString(KEY_PUBLIC, null) ?: return null
        val privStr = e2eePrefs.getString(KEY_PRIVATE, null) ?: return null
        val publicKey = pubStr.split(",").map { it.toInt().toByte() }.toByteArray()
        val privateKey = privStr.split(",").map { it.toInt().toByte() }.toByteArray()
        val pair = IdentityKeyPair(publicKey = publicKey, privateKey = privateKey)
        cachedIdentityKeyPair = pair
        return pair
    }

    /**
     * Check if identity key exists.
     */
    fun hasIdentityKey(): Boolean {
        return e2eePrefs.contains(KEY_PUBLIC) && e2eePrefs.contains(KEY_PRIVATE)
    }

    /**
     * Get the public identity key for sharing with peers.
     */
    fun getIdentityPublicKey(): ByteArray {
        return getIdentityKeyPair()?.publicKey ?: ByteArray(0)
    }

    /**
     * Establish a secure session with a peer.
     */
    fun establishSession(
        peerUserId: String,
        peerPublicKey: ByteArray
    ): Boolean {
        val identityKeyPair = getIdentityKeyPair() ?: return false
        return try {
            val session = signalSession.createSession(
                peerUserId = peerUserId,
                ourIdentityKey = identityKeyPair,
                peerIdentityPublicKey = peerPublicKey,
                peerSignedPreKey = peerPublicKey,
                peerOneTimePreKey = null
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Encrypt a message for a specific peer.
     */
    fun encryptMessage(
        peerUserId: String,
        plaintext: String
    ): EncryptedMessage? {
        val session = signalSession.getSession(peerUserId) ?: return null
        return signalSession.encryptMessage(
            session = session,
            plaintext = plaintext.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Decrypt a message from a peer.
     */
    fun decryptMessage(
        peerUserId: String,
        message: EncryptedMessage
    ): String? {
        val session = signalSession.getSession(peerUserId) ?: return null
        return try {
            val plaintext = signalSession.decryptMessage(session, message)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get or establish session for a peer.
     */
    fun getOrCreateSession(
        peerUserId: String,
        peerPublicKey: ByteArray
    ): Boolean {
        val identityKeyPair = getIdentityKeyPair() ?: return false
        return try {
            signalSession.getOrCreateSession(
                peerUserId = peerUserId,
                ourIdentityKey = identityKeyPair,
                peerIdentityPublicKey = peerPublicKey,
                peerSignedPreKey = peerPublicKey,
                peerOneTimePreKey = null
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear all E2EE data (for logout).
     */
    fun clearAll() {
        signalSession.clearAllSessions()
        e2eePrefs.edit().clear().apply()
        cachedIdentityKeyPair = null
    }

    /**
     * Get E2EE status for a conversation.
     */
    fun getEncryptionStatus(peerUserId: String): EncryptionStatus {
        val hasSession = signalSession.getSession(peerUserId) != null
        return EncryptionStatus(
            isEncrypted = hasSession,
            protocol = if (hasSession) "Signal Protocol" else "None",
            keyVerified = hasSession,
            forwardSecrecy = hasSession
        )
    }
}

data class EncryptionStatus(
    val isEncrypted: Boolean,
    val protocol: String,
    val keyVerified: Boolean,
    val forwardSecrecy: Boolean
)
