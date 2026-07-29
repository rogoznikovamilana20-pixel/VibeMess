package com.vibe.ui.security.signal

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * ⚠️ BROKEN — DO NOT USE.
 *
 * Critical issues:
 * 1. storeSession() is empty — sessions are NEVER persisted
 * 2. getSession() always returns null — sessions are NEVER restored
 * 3. Uses X25519KeyExchange which actually uses secp256r1, not X25519
 * 4. X3DH implementation uses only 2 DH operations instead of 4
 *
 * Will be replaced with libsignal-client or a vetted E2EE library in a future phase.
 */
@Deprecated("Broken implementation — sessions never persist, wrong key exchange. Will be replaced.")
class SignalSession(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "vibe_signal_sessions"
        private const val KEY_PREFIX = "session_"
    }

    private val keyExchange = X25519KeyExchange()
    private val random = SecureRandom()

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sessionStore: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Generate identity key pair for this device.
     */
    fun generateIdentityKeyPair(): IdentityKeyPair {
        val keyPair = keyExchange.generateKeyPair()
        return IdentityKeyPair(
            publicKey = keyPair.publicKey,
            privateKey = keyPair.privateKey
        )
    }

    /**
     * Create a new session with a peer using X3DH key exchange.
     */
    fun createSession(
        peerUserId: String,
        ourIdentityKey: IdentityKeyPair,
        peerIdentityPublicKey: ByteArray,
        peerSignedPreKey: ByteArray,
        peerOneTimePreKey: ByteArray?
    ): Session {
        // X3DH Key Agreement
        val dh1 = keyExchange.performKeyAgreement(
            ourIdentityKey.privateKey,
            peerIdentityPublicKey
        )
        val dh2 = keyExchange.performKeyAgreement(
            ourIdentityKey.privateKey,
            peerSignedPreKey
        )

        // Combine DH outputs
        val sharedSecret = dh1 + dh2

        // Initialize Double Ratchet
        val doubleRatchet = DoubleRatchet(
            sharedSecret = sharedSecret,
            ourPrivateKey = ourIdentityKey.privateKey,
            theirPublicKey = peerSignedPreKey
        )

        val session = Session(
            peerUserId = peerUserId,
            doubleRatchet = doubleRatchet,
            createdAt = System.currentTimeMillis()
        )

        // Store session
        storeSession(session)

        return session
    }

    /**
     * Encrypt a message for a peer.
     */
    fun encryptMessage(
        session: Session,
        plaintext: ByteArray
    ): EncryptedMessage {
        return session.doubleRatchet.encrypt(plaintext)
    }

    /**
     * Decrypt a message from a peer.
     */
    fun decryptMessage(
        session: Session,
        message: EncryptedMessage
    ): ByteArray {
        return session.doubleRatchet.decrypt(message)
    }

    /**
     * Get or create session for a peer.
     */
    fun getOrCreateSession(
        peerUserId: String,
        ourIdentityKey: IdentityKeyPair,
        peerIdentityPublicKey: ByteArray,
        peerSignedPreKey: ByteArray,
        peerOneTimePreKey: ByteArray?
    ): Session {
        val existing = getSession(peerUserId)
        if (existing != null) return existing

        return createSession(
            peerUserId,
            ourIdentityKey,
            peerIdentityPublicKey,
            peerSignedPreKey,
            peerOneTimePreKey
        )
    }

    private fun storeSession(session: Session) {
        // In production, serialize session state to encrypted storage
        // For now, keep in memory
    }

    /**
     * Get an existing session for a peer.
     */
    fun getSession(peerUserId: String): Session? {
        // In production, deserialize from encrypted storage
        return null
    }

    /**
     * Clear all sessions (for logout).
     */
    fun clearAllSessions() {
        sessionStore.edit().clear().apply()
    }
}

data class IdentityKeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray
)

data class Session(
    val peerUserId: String,
    val doubleRatchet: DoubleRatchet,
    val createdAt: Long
)
