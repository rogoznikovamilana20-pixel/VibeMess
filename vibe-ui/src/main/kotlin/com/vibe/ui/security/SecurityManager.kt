package com.vibe.ui.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vibe.common.logging.VibeLogger
import com.vibe.ui.security.e2ee.EncryptionStatus

/**
 * Central security manager for Vibe Messenger.
 *
 * ⚠️ NOTE: E2EE components are currently DISABLED.
 * The previous E2EE implementation (signal package) was mathematically broken:
 * - Used secp256r1 instead of X25519
 * - Incorrect HKDF implementation
 * - Sessions never persisted
 *
 * These components are marked @Deprecated and will be replaced with
 * libsignal-client or a vetted E2EE library in a future phase.
 *
 * Currently provides:
 * - Key management (SecureKeyManager)
 * - Integrity verification (IntegrityVerifier)
 */
class SecurityManager(private val context: Context) {

    companion object {
        @Volatile
        private var instance: SecurityManager? = null

        fun getInstance(context: Context): SecurityManager {
            return instance ?: synchronized(this) {
                instance ?: SecurityManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    val keyManager = KeyManager(context)
    val secureKeyManager = SecureKeyManager(context)
    val messageEncryptor = MessageEncryptor(keyManager)
    val integrityVerifier = IntegrityVerifier(context)

    private var isInitialized = false
    private val trustedFingerprints: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "vibe_trusted_fingerprints",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Initialize all security components.
     *
     * Note: E2EE initialization is SKIPPED because the previous implementation
     * was mathematically broken. It will be replaced in a future phase.
     */
    fun initialize() {
        if (isInitialized) return

        // Verify integrity first
        val integrity = integrityVerifier.performFullCheck()
        if (!integrity.isSecure) {
            VibeLogger.w("SecurityManager", "Integrity check warnings: ${integrity.getWarnings()}")
        }

        // E2EE initialization is DISABLED.
        // The previous signal/* implementation is broken and will be replaced.
        // e2eeManager.initialize()

        isInitialized = true
        VibeLogger.i("SecurityManager", "Security system initialized (E2EE disabled)")
    }

    /**
     * ⚠️ E2EE is DISABLED.
     *
     * The previous implementation was mathematically broken and has been isolated.
     * These methods return null/false until a proper E2EE library is integrated.
     */
    fun encryptMessage(peerUserId: String, plaintext: String): ByteArray? {
        VibeLogger.w("SecurityManager", "E2EE disabled — encryptMessage() not available")
        return null
    }

    fun decryptMessage(peerUserId: String, ciphertext: ByteArray): String? {
        VibeLogger.w("SecurityManager", "E2EE disabled — decryptMessage() not available")
        return null
    }

    fun establishSecureSession(peerUserId: String, peerPublicKey: ByteArray): Boolean {
        VibeLogger.w("SecurityManager", "E2EE disabled — establishSecureSession() not available")
        return false
    }

    fun getEncryptionStatus(peerUserId: String): EncryptionStatus {
        return EncryptionStatus(
            isEncrypted = false,
            protocol = "Disabled (previous implementation was broken)",
            keyVerified = false,
            forwardSecrecy = false
        )
    }

    /**
     * Verify peer's identity.
     */
    fun verifyPeerIdentity(
        peerUserId: String,
        peerFingerprint: String
    ): Boolean {
        val trustedFingerprint = trustedFingerprints.getString(peerUserId, null)
            ?: return false
        return SecureMemory.secureCompare(
            trustedFingerprint.toByteArray(),
            peerFingerprint.toByteArray()
        )
    }

    fun storePeerFingerprint(peerUserId: String, fingerprint: String) {
        trustedFingerprints.edit().putString(peerUserId, fingerprint).apply()
    }

    /**
     * Clear all security data (for logout).
     */
    fun clearAll() {
        secureKeyManager.clearAll()
        keyManager.deleteAllKeys()
        trustedFingerprints.edit().clear().apply()
        SecureMemory.wipeAll()
    }

    /**
     * Get security report.
     */
    fun getSecurityReport(): SecurityReport {
        return SecurityReport(
            e2eeEnabled = false,
            e2eeNote = "E2EE disabled — previous implementation was broken and removed",
            integrityStatus = integrityVerifier.performFullCheck(),
            keyRotationSupported = true,
            forwardSecrecy = false,
            authenticationRequired = true
        )
    }
}

data class SecurityReport(
    val e2eeEnabled: Boolean,
    val e2eeNote: String = "",
    val integrityStatus: IntegrityReport,
    val keyRotationSupported: Boolean,
    val forwardSecrecy: Boolean,
    val authenticationRequired: Boolean
)
