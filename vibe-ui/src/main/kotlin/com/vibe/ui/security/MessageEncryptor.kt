package com.vibe.ui.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts and decrypts messages using AES-GCM.
 */
class MessageEncryptor(private val keyManager: KeyManager) {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    /**
     * Encrypt a message.
     * Returns Base64-encoded ciphertext with IV prepended.
     */
    fun encrypt(plaintext: String): String {
        val key = keyManager.getOrCreateKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Prepend IV to ciphertext
        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt a message.
     * Input is Base64-encoded ciphertext with IV prepended.
     */
    fun decrypt(encrypted: String): String {
        val key = keyManager.getOrCreateKey()
        val combined = Base64.decode(encrypted, Base64.NO_WRAP)

        // Extract IV and ciphertext
        val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
        val ciphertext = combined.sliceArray(GCM_IV_LENGTH until combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    /**
     * Get the encryption key for sharing verification.
     */
    fun getKey(): SecretKey = keyManager.getOrCreateKey()

    /**
     * Get key fingerprint for identity verification.
     */
    fun getKeyFingerprint(): String = keyManager.getKeyFingerprint()
}
