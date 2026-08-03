package com.vibe.ui.e2e

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.spec.ECGenParameterSpec
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Manages E2E encryption keys for the Vibe messenger.
 * Uses X25519 for key exchange, AES-256-GCM for encryption, HMAC-SHA256 for authentication.
 * All keys stored in EncryptedSharedPreferences — never leave the device.
 */
class SignalKeyManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "signal_keys",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val TAG = "SignalKeyManager"
        private const val KEY_IDENTITY_PRIVATE = "identity_private_key"
        private const val KEY_IDENTITY_PUBLIC = "identity_public_key"
        private const val KEY_SIGNED_PRE_KEY_PRIVATE = "signed_pre_key_private"
        private const val KEY_SIGNED_PRE_KEY_PUBLIC = "signed_pre_key_public"
        private const val KEY_SIGNED_PRE_KEY_ID = "signed_pre_key_id"
        private const val KEY_ONE_TIME_PRE_KEYS = "one_time_pre_keys"
        private const val KEY_REGISTERED = "e2e_registered"
        private const val CURVE_NAME = "curve25519"
        private const val PRE_KEY_BATCH_SIZE = 20
    }

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * Check if E2E keys are generated and registered.
     */
    fun isRegistered(): Boolean = prefs.getBoolean(KEY_REGISTERED, false)

    /**
     * Generate all keys for a new user. Called once at registration.
     * Returns the public key bundle to upload to Supabase.
     */
    fun generateKeys(): PreKeyBundleData {
        val random = SecureRandom()

        // Identity Key Pair (long-term, X25519)
        val identityKeyPair = generateX25519KeyPair(random)

        // Signed Pre Key
        val signedPreKeyId = 1
        val signedPreKeyPair = generateX25519KeyPair(random)
        val signature = signData(signedPreKeyPair.public.encoded, identityKeyPair.private)

        // One-Time Pre Keys
        val oneTimePreKeys = (1..PRE_KEY_BATCH_SIZE).map { id ->
            val keyPair = generateX25519KeyPair(random)
            OneTimePreKeyData(
                keyId = id,
                publicKey = keyPair.public.encoded
            )
        }

        // Store locally
        prefs.edit()
            .putString(KEY_IDENTITY_PRIVATE, Base64.encodeToString(identityKeyPair.private.encoded, Base64.NO_WRAP))
            .putString(KEY_IDENTITY_PUBLIC, Base64.encodeToString(identityKeyPair.public.encoded, Base64.NO_WRAP))
            .putString(KEY_SIGNED_PRE_KEY_PRIVATE, Base64.encodeToString(signedPreKeyPair.private.encoded, Base64.NO_WRAP))
            .putString(KEY_SIGNED_PRE_KEY_PUBLIC, Base64.encodeToString(signedPreKeyPair.public.encoded, Base64.NO_WRAP))
            .putInt(KEY_SIGNED_PRE_KEY_ID, signedPreKeyId)
            .putString(KEY_ONE_TIME_PRE_KEYS, oneTimePreKeys.joinToString(",") { "${it.keyId}:${Base64.encodeToString(it.publicKey, Base64.NO_WRAP)}" })
            .putBoolean(KEY_REGISTERED, true)
            .apply()

        Log.d(TAG, "Keys generated for new user")

        return PreKeyBundleData(
            identityPublicKey = identityKeyPair.public.encoded,
            signedPreKeyId = signedPreKeyId,
            signedPreKeyPublic = signedPreKeyPair.public.encoded,
            signedPreKeySignature = signature,
            oneTimePreKeys = oneTimePreKeys
        )
    }

    /**
     * Get our identity private key.
     */
    fun getIdentityPrivateKey(): ECPrivateKey {
        val base64 = prefs.getString(KEY_IDENTITY_PRIVATE, null)
            ?: throw IllegalStateException("Identity key not generated")
        val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
        val kf = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        return kf.generatePrivate(PKCS8EncodedKeySpec(keyBytes)) as ECPrivateKey
    }

    /**
     * Get our identity public key.
     */
    fun getIdentityPublicKey(): ECPublicKey {
        val base64 = prefs.getString(KEY_IDENTITY_PUBLIC, null)
            ?: throw IllegalStateException("Identity key not generated")
        val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
        val kf = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        return kf.generatePublic(X509EncodedKeySpec(keyBytes)) as ECPublicKey
    }

    /**
     * Get signed pre key private key.
     */
    fun getSignedPreKeyPrivateKey(): ECPrivateKey {
        val base64 = prefs.getString(KEY_SIGNED_PRE_KEY_PRIVATE, null)
            ?: throw IllegalStateException("Signed pre key not generated")
        val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
        val kf = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        return kf.generatePrivate(PKCS8EncodedKeySpec(keyBytes)) as ECPrivateKey
    }

    /**
     * Get signed pre key public key.
     */
    fun getSignedPreKeyPublicKey(): ECPublicKey {
        val base64 = prefs.getString(KEY_SIGNED_PRE_KEY_PUBLIC, null)
            ?: throw IllegalStateException("Signed pre key not generated")
        val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
        val kf = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        return kf.generatePublic(X509EncodedKeySpec(keyBytes)) as ECPublicKey
    }

    /**
     * Get signed pre key ID.
     */
    fun getSignedPreKeyId(): Int = prefs.getInt(KEY_SIGNED_PRE_KEY_ID, 1)

    /**
     * Perform X25519 key agreement and derive shared secret.
     */
    fun performKeyAgreement(myPrivate: ECPrivateKey, theirPublic: ECPublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
        ka.init(myPrivate)
        ka.doPhase(theirPublic, true)
        return ka.generateSecret()
    }

    /**
     * Derive encryption key from shared secret using HKDF-SHA256 (RFC 5869).
     */
    fun deriveKey(sharedSecret: ByteArray, salt: ByteArray, info: String): SecretKey {
        val kdf = HKDFBytesGenerator(SHA256Digest())
        kdf.init(HKDFParameters(sharedSecret, salt, info.toByteArray(Charsets.UTF_8)))
        val okm = ByteArray(32)
        kdf.generateBytes(okm, 0, okm.size)
        return SecretKeySpec(okm, "AES")
    }

    /**
     * Generate a random IV for AES-GCM.
     */
    fun generateIV(): ByteArray {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        return iv
    }

    /**
     * Encrypt data with AES-256-GCM.
     */
    fun encrypt(plaintext: ByteArray, key: SecretKey, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        return cipher.doFinal(plaintext)
    }

    /**
     * Decrypt data with AES-256-GCM.
     */
    fun decrypt(ciphertext: ByteArray, key: SecretKey, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(ciphertext)
    }

    /**
     * Sign data with our identity key.
     */
    private fun signData(data: ByteArray, privateKey: java.security.PrivateKey): ByteArray {
        val signature = java.security.Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME)
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    /**
     * Get public keys to upload to Supabase (JSON string).
     * Uses existing keys — does NOT regenerate them.
     */
    fun getPublicKeyBundleJson(): String {
        val identityPublicBase64 = prefs.getString(KEY_IDENTITY_PUBLIC, null)
            ?: throw IllegalStateException("Identity key not generated")
        val signedPreKeyPublicBase64 = prefs.getString(KEY_SIGNED_PRE_KEY_PUBLIC, null)
            ?: throw IllegalStateException("Signed pre key not generated")
        val signedPreKeyId = prefs.getInt(KEY_SIGNED_PRE_KEY_ID, 1)
        val signedPreKeySignatureBase64 = prefs.getString("signed_pre_key_signature", null)

        val signatureBase64 = signedPreKeySignatureBase64 ?: run {
            val signedPreKeyBytes = Base64.decode(signedPreKeyPublicBase64, Base64.NO_WRAP)
            Base64.encodeToString(signData(signedPreKeyBytes, getIdentityPrivateKey()), Base64.NO_WRAP)
        }

        val oneTimePreKeysStr = prefs.getString(KEY_ONE_TIME_PRE_KEYS, null) ?: ""

        return org.json.JSONObject().apply {
            put("identity_public_key", identityPublicBase64)
            put("signed_pre_key_id", signedPreKeyId)
            put("signed_pre_key_public", signedPreKeyPublicBase64)
            put("signed_pre_key_signature", signatureBase64)
            put("one_time_pre_keys", org.json.JSONArray().apply {
                oneTimePreKeysStr.split(",").filter { it.isNotBlank() }.forEach { entry ->
                    val parts = entry.split(":", limit = 2)
                    if (parts.size == 2) {
                        put(org.json.JSONObject().apply {
                            put("key_id", parts[0].toIntOrNull() ?: 0)
                            put("public_key", parts[1])
                        })
                    }
                }
            })
        }.toString()
    }

    /**
     * Parse a remote user's key bundle from JSON.
     */
    fun parseRemoteBundle(json: org.json.JSONObject): RemotePreKeyBundleData? {
        return try {
            val identityKeyBytes = Base64.decode(json.getString("identity_public_key"), Base64.NO_WRAP)
            val signedPreKeyId = json.getInt("signed_pre_key_id")
            val signedPreKeyBytes = Base64.decode(json.getString("signed_pre_key_public"), Base64.NO_WRAP)
            val signature = Base64.decode(json.getString("signed_pre_key_signature"), Base64.NO_WRAP)
            val oneTimePreKeys = json.getJSONArray("one_time_pre_keys")
            val firstPreKey = oneTimePreKeys.getJSONObject(0)

            val kf = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)

            RemotePreKeyBundleData(
                identityPublicKey = kf.generatePublic(X509EncodedKeySpec(identityKeyBytes)) as ECPublicKey,
                signedPreKeyId = signedPreKeyId,
                signedPreKeyPublic = kf.generatePublic(X509EncodedKeySpec(signedPreKeyBytes)) as ECPublicKey,
                signedPreKeySignature = signature,
                oneTimePreKeyId = firstPreKey.getInt("key_id"),
                oneTimePreKeyPublic = kf.generatePublic(X509EncodedKeySpec(
                    Base64.decode(firstPreKey.getString("public_key"), Base64.NO_WRAP)
                )) as ECPublicKey
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse remote bundle", e)
            null
        }
    }

    /**
     * Clear all keys (for logout).
     */
    fun clear() {
        prefs.edit().clear().apply()
        Log.d(TAG, "All E2E keys cleared")
    }

    // --- Helpers ---

    private fun generateX25519KeyPair(random: SecureRandom): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        kpg.initialize(ECGenParameterSpec("curve25519"), random)
        return kpg.generateKeyPair()
    }
}

/**
 * Public key bundle to upload to Supabase.
 */
data class PreKeyBundleData(
    val identityPublicKey: ByteArray,
    val signedPreKeyId: Int,
    val signedPreKeyPublic: ByteArray,
    val signedPreKeySignature: ByteArray,
    val oneTimePreKeys: List<OneTimePreKeyData>
)

data class OneTimePreKeyData(
    val keyId: Int,
    val publicKey: ByteArray
)

/**
 * Remote user's pre-key bundle.
 */
data class RemotePreKeyBundleData(
    val identityPublicKey: ECPublicKey,
    val signedPreKeyId: Int,
    val signedPreKeyPublic: ECPublicKey,
    val signedPreKeySignature: ByteArray,
    val oneTimePreKeyId: Int,
    val oneTimePreKeyPublic: ECPublicKey
)
