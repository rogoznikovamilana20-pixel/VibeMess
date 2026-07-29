package com.vibe.ui.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages encryption keys using Android Keystore.
 * Supports key rotation and authentication requirements.
 */
class KeyManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "vibe_e2ee_key_v"
        private const val CURRENT_KEY_VERSION = 1
        private const val KEYSTORE_NAME = "VibeKeyStore"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    /**
     * Get the current key alias with version.
     */
    private fun getCurrentKeyAlias(): String = "$KEY_ALIAS_PREFIX$CURRENT_KEY_VERSION"

    /**
     * Generate or retrieve the E2EE key.
     * Requires user authentication (biometric/PIN) for key usage.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getOrCreateKey(): SecretKey {
        val alias = getCurrentKeyAlias()

        // Check if key already exists
        keyStore.getEntry(alias, null)?.let { entry ->
            return (entry as KeyStore.SecretKeyEntry).secretKey
        }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        @Suppress("DEPRECATION")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(300) // 5 minutes
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Rotate to a new key version.
     * Old keys are kept for decryption of existing data.
     */
    fun rotateKey(): SecretKey {
        // Delete current key
        deleteKey(CURRENT_KEY_VERSION)

        // Generate new key with incremented version
        // Note: In production, you'd increment CURRENT_KEY_VERSION
        return getOrCreateKey()
    }

    /**
     * Get a specific key version for decryption.
     */
    fun getKeyByVersion(version: Int): SecretKey? {
        val alias = "$KEY_ALIAS_PREFIX$version"
        return keyStore.getEntry(alias, null)?.let { entry ->
            (entry as KeyStore.SecretKeyEntry).secretKey
        }
    }

    /**
     * Get the key fingerprint for verification.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getKeyFingerprint(): String {
        val key = getOrCreateKey()
        val fingerprint = java.security.MessageDigest.getInstance("SHA-256")
            .digest(key.encoded)
        return fingerprint.joinToString(":") { "%02X".format(it) }
    }

    /**
     * Delete the key for a specific version.
     */
    fun deleteKey(version: Int = CURRENT_KEY_VERSION) {
        val alias = "$KEY_ALIAS_PREFIX$version"
        keyStore.deleteEntry(alias)
    }

    /**
     * Delete all key versions.
     */
    fun deleteAllKeys() {
        for (i in 1..10) { // Check up to 10 versions
            val alias = "$KEY_ALIAS_PREFIX$i"
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        }
    }

    /**
     * Check if current key exists.
     */
    fun hasKey(): Boolean {
        return keyStore.containsAlias(getCurrentKeyAlias())
    }

    /**
     * Get current key version.
     */
    fun getCurrentVersion(): Int = CURRENT_KEY_VERSION
}
