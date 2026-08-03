package com.vibe.ui.e2e

import android.util.Base64
import android.util.Log
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.Security
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Sealed Sender implementation.
 * Encrypts sender identity so the server cannot see WHO is sending messages.
 *
 * Protocol:
 * 1. Sender generates ephemeral key pair
 * 2. Sender derives shared secret with recipient's identity key
 * 3. Sender encrypts: (sender_identity, message) with derived key
 * 4. Sender sends: ephemeral_public_key + encrypted_payload
 * 5. Server sees only recipient_id, not sender_id
 * 6. Recipient decrypts using ephemeral key + own identity key
 *
 * Reference: https://signal.org/blog/sealed-sender/
 */
class SealedSender {

    companion object {
        private const val TAG = "SealedSender"
        private const val VERSION = 1
    }

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * Seal a message (encrypt sender identity).
     * Returns sealed envelope that can be sent to server.
     */
    fun sealMessage(
        senderId: String,
        senderIdentityKey: java.security.interfaces.ECPrivateKey,
        recipientIdentityKeyBytes: ByteArray,
        plaintext: ByteArray
    ): SealedEnvelope {
        // Generate ephemeral key pair
        val kpg = java.security.KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        kpg.initialize(ECGenParameterSpec("curve25519"))
        val ephemeralKeyPair = kpg.generateKeyPair()

        // Load recipient's identity public key
        val kf = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        val recipientIdentityKey = kf.generatePublic(X509EncodedKeySpec(recipientIdentityKeyBytes)) as java.security.interfaces.ECPublicKey

        // Derive shared secret using ECDH
        val ka = KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
        ka.init(ephemeralKeyPair.private)
        ka.doPhase(recipientIdentityKey, true)
        val sharedSecret = ka.generateSecret()

        // Derive encryption key using HKDF
        val salt = "vibe-sealed-sender".toByteArray(Charsets.UTF_8)
        val info = "sealed-sender:$VERSION".toByteArray(Charsets.UTF_8)
        val encryptionKey = hkdfDerive(sharedSecret, salt, info, 32)

        // Create sender identity block
        val senderIdentity = senderId.toByteArray(Charsets.UTF_8)
        val senderIdentityLength = senderIdentity.size

        // Build payload: [sender_identity_length (2 bytes)] [sender_identity] [plaintext]
        val payload = ByteArray(2 + senderIdentityLength + plaintext.size)
        System.arraycopy(intToShortBytes(senderIdentityLength), 0, payload, 0, 2)
        System.arraycopy(senderIdentity, 0, payload, 2, senderIdentityLength)
        System.arraycopy(plaintext, 0, payload, 2 + senderIdentityLength, plaintext.size)

        // Encrypt with AES-256-GCM
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), spec)

        // Add associated data (ephemeral public key hash for integrity)
        val ephemeralPublicHash = CryptoUtils.sha256(ephemeralKeyPair.public.encoded)
        cipher.updateAAD(ephemeralPublicHash)

        val ciphertext = cipher.doFinal(payload)

        // Zeroize sensitive data
        CryptoUtils.zeroize(sharedSecret)
        CryptoUtils.zeroize(encryptionKey)

        return SealedEnvelope(
            version = VERSION,
            ephemeralPublicKey = ephemeralKeyPair.public.encoded,
            iv = iv,
            ciphertext = ciphertext
        )
    }

    /**
     * Unseal a message (decrypt sender identity).
     * Returns sender ID and decrypted plaintext.
     */
    fun unsealMessage(
        envelope: SealedEnvelope,
        recipientIdentityKey: java.security.interfaces.ECPrivateKey
    ): UnsealedMessage? {
        return try {
            // Load ephemeral public key
            val kf = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
            val ephemeralPublicKey = kf.generatePublic(X509EncodedKeySpec(envelope.ephemeralPublicKey)) as java.security.interfaces.ECPublicKey

            // Derive shared secret using ECDH
            val ka = KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
            ka.init(recipientIdentityKey)
            ka.doPhase(ephemeralPublicKey, true)
            val sharedSecret = ka.generateSecret()

            // Derive decryption key
            val salt = "vibe-sealed-sender".toByteArray(Charsets.UTF_8)
            val info = "sealed-sender:$VERSION".toByteArray(Charsets.UTF_8)
            val decryptionKey = hkdfDerive(sharedSecret, salt, info, 32)

            // Decrypt
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, envelope.iv)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(decryptionKey, "AES"), spec)

            // Add associated data
            val ephemeralPublicHash = CryptoUtils.sha256(envelope.ephemeralPublicKey)
            cipher.updateAAD(ephemeralPublicHash)

            val payload = cipher.doFinal(envelope.ciphertext)

            // Zeroize sensitive data
            CryptoUtils.zeroize(sharedSecret)
            CryptoUtils.zeroize(decryptionKey)

            // Parse payload
            val senderIdentityLength = shortBytesToInt(payload, 0)
            val senderIdentity = payload.copyOfRange(2, 2 + senderIdentityLength)
            val plaintext = payload.copyOfRange(2 + senderIdentityLength, payload.size)

            UnsealedMessage(
                senderId = String(senderIdentity, Charsets.UTF_8),
                plaintext = plaintext
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unseal message", e)
            null
        }
    }

    /**
     * Seal a message for group chat.
     * Encrypts to all members except sender.
     */
    fun sealGroupMessage(
        senderId: String,
        senderIdentityKey: java.security.interfaces.ECPrivateKey,
        memberKeys: List<Pair<String, ByteArray>>, // (memberId, identityKey)
        plaintext: ByteArray
    ): Map<String, SealedEnvelope> {
        val envelopes = mutableMapOf<String, SealedEnvelope>()

        memberKeys.forEach { (memberId, memberKey) ->
            if (memberId != senderId) {
                val envelope = sealMessage(
                    senderId = senderId,
                    senderIdentityKey = senderIdentityKey,
                    recipientIdentityKeyBytes = memberKey,
                    plaintext = plaintext
                )
                envelopes[memberId] = envelope
            }
        }

        return envelopes
    }

    // --- Key Derivation ---

    /**
     * HKDF key derivation.
     */
    private fun hkdfDerive(
        sharedSecret: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")

        // Extract
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(sharedSecret)

        // Expand
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        val okm = mac.doFinal(info + byteArrayOf(0x01))

        return okm.copyOf(length)
    }

    // --- Helpers ---

    private fun intToShortBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 8).toByte(),
            value.toByte()
        )
    }

    private fun shortBytesToInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 8) or
               (bytes[offset + 1].toInt() and 0xFF)
    }
}

/**
 * Sealed envelope for server transmission.
 */
data class SealedEnvelope(
    val version: Int,
    val ephemeralPublicKey: ByteArray,
    val iv: ByteArray,
    val ciphertext: ByteArray
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("version", version)
            put("ephemeral_key", Base64.encodeToString(ephemeralPublicKey, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }.toString()
    }

    companion object {
        fun fromJson(json: String): SealedEnvelope? {
            return try {
                val obj = JSONObject(json)
                SealedEnvelope(
                    version = obj.getInt("version"),
                    ephemeralPublicKey = Base64.decode(obj.getString("ephemeral_key"), Base64.NO_WRAP),
                    iv = Base64.decode(obj.getString("iv"), Base64.NO_WRAP),
                    ciphertext = Base64.decode(obj.getString("ciphertext"), Base64.NO_WRAP)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Unsealed message with sender identity.
 */
data class UnsealedMessage(
    val senderId: String,
    val plaintext: ByteArray
)
