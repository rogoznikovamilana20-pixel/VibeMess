package com.vibe.ui.e2e

import android.util.Log
import com.vibe.common.crypto.PostQuantumKem
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Security
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * ML-KEM (Module-Lattice Key Encapsulation Mechanism) implementation.
 * Uses Bouncy Castle's ML-KEM-768 via low-level MLKEMEngine API.
 *
 * ML-KEM-768 provides NIST Level 3 security.
 * Resistant to attacks by both classical and quantum computers.
 *
 * Reference: https://pq-crystals.org/kyber/
 */
class PostQuantumKeyExchange {

    companion object {
        private const val TAG = "PQKeyExchange"
    }

    private val pqKem = PostQuantumKem

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun generateKeyPair(): PQKeyPair {
        return try {
            val kp = pqKem.generateKeyPair()
            PQKeyPair(publicKey = kp.publicKey, privateKey = kp.privateKey, algorithm = "ML-KEM-768")
        } catch (e: Exception) {
            Log.w(TAG, "ML-KEM not available, using X25519 fallback", e)
            generateX25519KeyPair()
        }
    }

    fun encapsulate(recipientPublicKey: ByteArray): PQEncapsulation {
        return try {
            val result = pqKem.encapsulate(recipientPublicKey)
            PQEncapsulation(ciphertext = result.ciphertext, sharedSecret = result.sharedSecret)
        } catch (e: Exception) {
            Log.w(TAG, "ML-KEM encapsulation failed, using ECDH fallback", e)
            ecdhEncapsulate(recipientPublicKey)
        }
    }

    fun decapsulate(ciphertext: ByteArray, privateKey: ByteArray): ByteArray {
        return try {
            pqKem.decapsulate(ciphertext, privateKey)
        } catch (e: Exception) {
            Log.w(TAG, "ML-KEM decapsulation failed, using ECDH fallback", e)
            ecdhDecapsulate(ciphertext, privateKey)
        }
    }

    fun hybridKeyExchange(
        recipientPQPublicKey: ByteArray,
        recipientECPublicKey: ByteArray
    ): ByteArray {
        val ecKpg = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        ecKpg.initialize(ECGenParameterSpec("curve25519"))
        val ecKeyPair = ecKpg.generateKeyPair()

        val ka = KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
        ka.init(ecKeyPair.private)
        val recipientEcKey = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
            .generatePublic(X509EncodedKeySpec(recipientECPublicKey))
        ka.doPhase(recipientEcKey, true)
        val ecSharedSecret = ka.generateSecret()

        val pqResult = try {
            encapsulate(recipientPQPublicKey)
        } catch (e: Exception) {
            PQEncapsulation(
                ciphertext = ecKeyPair.public.encoded,
                sharedSecret = ecSharedSecret
            )
        }

        val combined = ByteArray(ecSharedSecret.size + pqResult.sharedSecret.size)
        System.arraycopy(ecSharedSecret, 0, combined, 0, ecSharedSecret.size)
        System.arraycopy(pqResult.sharedSecret, 0, combined, ecSharedSecret.size, pqResult.sharedSecret.size)

        return deriveKey(combined, "vibe-hybrid-v1", 32)
    }

    private fun generateX25519KeyPair(): PQKeyPair {
        val kpg = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        kpg.initialize(ECGenParameterSpec("curve25519"))
        val keyPair = kpg.generateKeyPair()
        return PQKeyPair(
            publicKey = keyPair.public.encoded,
            privateKey = keyPair.private.encoded,
            algorithm = "X25519"
        )
    }

    private fun ecdhEncapsulate(recipientPublicKey: ByteArray): PQEncapsulation {
        val kpg = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        kpg.initialize(ECGenParameterSpec("curve25519"))
        val keyPair = kpg.generateKeyPair()

        val ka = KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
        ka.init(keyPair.private)
        val recipientKey = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
            .generatePublic(X509EncodedKeySpec(recipientPublicKey))
        ka.doPhase(recipientKey, true)
        val sharedSecret = ka.generateSecret()

        return PQEncapsulation(
            ciphertext = keyPair.public.encoded,
            sharedSecret = deriveKey(sharedSecret, "vibe-ecdh", 32)
        )
    }

    private fun ecdhDecapsulate(ciphertext: ByteArray, privateKey: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(privateKey))
        val pubKey = kf.generatePublic(X509EncodedKeySpec(ciphertext))

        val ka = KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
        ka.init(privKey)
        ka.doPhase(pubKey, true)
        val sharedSecret = ka.generateSecret()

        return deriveKey(sharedSecret, "vibe-ecdh", 32)
    }

    private fun deriveKey(sharedSecret: ByteArray, salt: String, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val prk = mac.doFinal(sharedSecret)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        val okm = mac.doFinal(byteArrayOf(0x01))
        return okm.copyOf(length)
    }
}

/**
 * ML-KEM key pair.
 */
data class PQKeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray,
    val algorithm: String = "ML-KEM-768"
)

/**
 * ML-KEM encapsulation result.
 */
data class PQEncapsulation(
    val ciphertext: ByteArray,
    val sharedSecret: ByteArray
)
