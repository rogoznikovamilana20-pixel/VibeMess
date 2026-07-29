package com.vibe.ui.security.signal

import java.security.SecureRandom
import java.security.KeyPairGenerator
import java.security.KeyFactory
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * ⚠️ BROKEN — DO NOT USE.
 *
 * This class is named X25519KeyExchange but actually uses secp256r1 (NIST P-256),
 * NOT X25519/Curve25519. The implementation is mathematically incompatible with
 * the Signal Protocol specification.
 *
 * Will be replaced with libsignal-client or a vetted E2EE library in a future phase.
 */
@Deprecated("Broken implementation — uses secp256r1 despite X25519 naming. Will be replaced.")
class X25519KeyExchange {

    companion object {
        private const val ALGORITHM = "EC"
        private const val CURVE = "secp256r1" // NIST P-256
        private val random = SecureRandom()
    }

    /**
     * Generate a new key pair for key exchange.
     */
    fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance(ALGORITHM)
        generator.initialize(ECGenParameterSpec(CURVE), random)
        val keyPair = generator.generateKeyPair()

        return KeyPair(
            publicKey = keyPair.public.encoded,
            privateKey = keyPair.private.encoded
        )
    }

    /**
     * Perform key agreement to derive shared secret.
     */
    fun performKeyAgreement(
        privateKey: ByteArray,
        peerPublicKey: ByteArray
    ): ByteArray {
        val keyFactory = KeyFactory.getInstance(ALGORITHM)

        val privateKeySpec = java.security.spec.PKCS8EncodedKeySpec(privateKey)
        val privateKeyObj = keyFactory.generatePrivate(privateKeySpec)

        val publicKeySpec = java.security.spec.X509EncodedKeySpec(peerPublicKey)
        val publicKeyObj = keyFactory.generatePublic(publicKeySpec)

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKeyObj)
        keyAgreement.doPhase(publicKeyObj, true)

        return keyAgreement.generateSecret()
    }

    /**
     * Derive encryption key from shared secret using HKDF.
     */
    fun deriveKey(sharedSecret: ByteArray, info: ByteArray): SecretKey {
        val derived = hkdf(sharedSecret, info, 32)
        return SecretKeySpec(derived, "AES")
    }

    /**
     * HKDF (HMAC-based Key Derivation Function).
     */
    private fun hkdf(ikm: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")

        // Extract
        mac.init(SecretKeySpec(ByteArray(32), "HmacSHA256"))
        val prk = mac.doFinal(ikm)

        // Expand
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        var t = ByteArray(0)
        val okm = ByteArray(length)
        var i = 0
        while (i * 32 < length) {
            t = mac.doFinal(t + info + byteArrayOf((i + 1).toByte()))
            System.arraycopy(t, 0, okm, i * 32, minOf(32, length - i * 32))
            i++
        }

        return okm
    }
}

data class KeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KeyPair
        return publicKey.contentEquals(other.publicKey) && privateKey.contentEquals(other.privateKey)
    }

    override fun hashCode(): Int {
        return publicKey.contentHashCode() * 31 + privateKey.contentHashCode()
    }
}
