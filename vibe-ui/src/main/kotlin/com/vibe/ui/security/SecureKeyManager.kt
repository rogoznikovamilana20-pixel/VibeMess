package com.vibe.ui.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Consolidated secure storage for API keys using Android Keystore + EncryptedSharedPreferences.
 * This is the single source of truth for all encrypted key storage.
 */
class SecureKeyManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "vibe_api_key"
        private const val PREFS_NAME = "vibe_secure_prefs"
        private const val KEY_OPENAI = "openai_api_key"
        private const val KEY_ANTHROPIC = "anthropic_api_key"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val MIN_CIPHERTEXT_LENGTH = GCM_IV_LENGTH + 16 // IV + minimum tag
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    /**
     * Get or create the encryption key from Android Keystore.
     */
    private fun getOrCreateKey(): SecretKey? {
        return try {
            keyStore.getEntry(KEY_ALIAS, null)?.let { entry ->
                (entry as? KeyStore.SecretKeyEntry)?.secretKey
            } ?: run {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    KEYSTORE_PROVIDER
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Encrypt and store the OpenAI API key.
     */
    fun storeOpenAiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_OPENAI, apiKey).apply()
    }

    /**
     * Retrieve the OpenAI API key.
     */
    fun getOpenAiKey(): String {
        return encryptedPrefs.getString(KEY_OPENAI, "") ?: ""
    }

    /**
     * Encrypt and store the Anthropic API key.
     */
    fun storeAnthropicKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_ANTHROPIC, apiKey).apply()
    }

    /**
     * Retrieve the Anthropic API key.
     */
    fun getAnthropicKey(): String {
        return encryptedPrefs.getString(KEY_ANTHROPIC, "") ?: ""
    }

    /**
     * Encrypt a string using AES-GCM.
     */
    fun encrypt(plaintext: String): String {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt a string using AES-GCM with input validation.
     */
    fun decrypt(encrypted: String): String {
        val combined = Base64.decode(encrypted, Base64.NO_WRAP)

        // Validate minimum ciphertext length
        if (combined.size < MIN_CIPHERTEXT_LENGTH) {
            throw SecurityException("Invalid ciphertext: too short (minimum $MIN_CIPHERTEXT_LENGTH bytes)")
        }

        val key = getOrCreateKey()
        val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
        val ciphertext = combined.sliceArray(GCM_IV_LENGTH until combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    /**
     * Clear all stored keys.
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }

    /**
     * Check if keys are stored.
     */
    fun hasKeys(): Boolean {
        return encryptedPrefs.contains(KEY_OPENAI) || encryptedPrefs.contains(KEY_ANTHROPIC)
    }
}
