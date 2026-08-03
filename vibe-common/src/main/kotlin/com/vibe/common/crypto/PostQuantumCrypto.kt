package com.vibe.common.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.lang.reflect.Method
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Post-Quantum Key Encapsulation Mechanism using ML-KEM-768 (Kyber-768).
 * NIST FIPS 203 Level 3 security.
 *
 * Key generation uses standard JCA KeyPairGenerator (works everywhere).
 * Encapsulation/decapsulation use BC's internal MLKEMEngine via reflection.
 * On Android (ART): reflection works fine, full ML-KEM support.
 * On JVM unit tests: reflection may be blocked by module system — caller should catch.
 */
object PostQuantumKem {

    private const val ALGORITHM = "ML-KEM-768"
    private const val SHARED_SECRET_SIZE = 32

    private val mlkemEngineClass by lazy { Class.forName("org.bouncycastle.pqc.crypto.mlkem.MLKEMEngine") }
    private val initMethod: Method by lazy { mlkemEngineClass.getMethod("init", SecureRandom::class.java) }
    private val genKeyPairMethod: Method by lazy { mlkemEngineClass.getMethod("generateKemKeyPair") }
    private val kemEncryptMethod: Method by lazy { mlkemEngineClass.getMethod("kemEncrypt", ByteArray::class.java, ByteArray::class.java) }
    private val kemDecryptMethod: Method by lazy { mlkemEngineClass.getMethod("kemDecrypt", ByteArray::class.java, ByteArray::class.java) }

    private val mlkemParamsClass by lazy { Class.forName("org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters") }
    private val getEngineMethod: Method by lazy { mlkemParamsClass.getMethod("getEngine") }
    private val mlkem768Field by lazy { mlkemParamsClass.getField("ml_kem_768") }

    private fun createEngine(): Any {
        val params = mlkem768Field.get(null)!!
        val engine = getEngineMethod.invoke(params)!!
        initMethod.invoke(engine, SecureRandom())
        return engine
    }

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    data class KemKeyPair(
        val publicKey: ByteArray,
        val privateKey: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is KemKeyPair) return false
            return publicKey.contentEquals(other.publicKey) &&
                    privateKey.contentEquals(other.privateKey)
        }
        override fun hashCode(): Int = publicKey.contentHashCode() * 31 + privateKey.contentHashCode()
    }

    data class EncapsulationResult(
        val ciphertext: ByteArray,
        val sharedSecret: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EncapsulationResult) return false
            return ciphertext.contentEquals(other.ciphertext) &&
                    sharedSecret.contentEquals(other.sharedSecret)
        }
        override fun hashCode(): Int = ciphertext.contentHashCode() * 31 + sharedSecret.contentHashCode()
    }

    fun generateKeyPair(): KemKeyPair {
        val kpg = java.security.KeyPairGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
        val kp = kpg.generateKeyPair()
        return KemKeyPair(
            publicKey = kp.public.encoded,
            privateKey = kp.private.encoded
        )
    }

    fun encapsulate(publicKeyBytes: ByteArray): EncapsulationResult {
        val engine = createEngine()
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        @Suppress("UNCHECKED_CAST")
        val result = kemEncryptMethod.invoke(engine, publicKeyBytes, random) as Array<ByteArray>
        return EncapsulationResult(
            ciphertext = result[0],
            sharedSecret = result[1].copyOf(SHARED_SECRET_SIZE)
        )
    }

    fun decapsulate(ciphertext: ByteArray, privateKeyBytes: ByteArray): ByteArray {
        val engine = createEngine()
        val sharedSecret = kemDecryptMethod.invoke(engine, privateKeyBytes, ciphertext) as ByteArray
        return sharedSecret.copyOf(SHARED_SECRET_SIZE)
    }

    fun deriveSharedSecret(kemSecret1: ByteArray, kemSecret2: ByteArray, kemSecret3: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec("vibe-pq-x3dh".toByteArray(), "HmacSHA256"))
        val combined = kemSecret1 + kemSecret2 + kemSecret3
        return mac.doFinal(combined)
    }
}

/**
 * Post-Quantum Digital Signature using ML-DSA-65 (Dilithium-3).
 * NIST FIPS 204 Level 3 security.
 */
object PostQuantumSignature {

    private const val ALGORITHM = "ML-DSA-65"

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    data class SignatureKeyPair(
        val publicKey: ByteArray,
        val privateKey: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SignatureKeyPair) return false
            return publicKey.contentEquals(other.publicKey) &&
                    privateKey.contentEquals(other.privateKey)
        }
        override fun hashCode(): Int = publicKey.contentHashCode() * 31 + privateKey.contentHashCode()
    }

    fun generateKeyPair(): SignatureKeyPair {
        val kpg = java.security.KeyPairGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
        val kp = kpg.generateKeyPair()
        return SignatureKeyPair(
            publicKey = kp.public.encoded,
            privateKey = kp.private.encoded
        )
    }

    fun sign(data: ByteArray, privateKeyBytes: ByteArray): ByteArray {
        val kf = java.security.KeyFactory.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
        val privKey = kf.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes))
        val sig = java.security.Signature.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
        sig.initSign(privKey)
        sig.update(data)
        return sig.sign()
    }

    fun verify(data: ByteArray, signature: ByteArray, publicKeyBytes: ByteArray): Boolean {
        return try {
            val kf = java.security.KeyFactory.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
            val pubKey = kf.generatePublic(java.security.spec.X509EncodedKeySpec(publicKeyBytes))
            val sig = java.security.Signature.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
            sig.initVerify(pubKey)
            sig.update(data)
            sig.verify(signature)
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * AES-256-GCM symmetric encryption for message payloads.
 */
object AesGcmCipher {

    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    fun encrypt(key: ByteArray, plaintext: ByteArray, associatedData: ByteArray? = null): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), spec)
        associatedData?.let { cipher.updateAAD(it) }

        val ciphertext = cipher.doFinal(plaintext)
        return Pair(ciphertext, iv)
    }

    fun decrypt(key: ByteArray, ciphertext: ByteArray, iv: ByteArray, associatedData: ByteArray? = null): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), spec)
        associatedData?.let { cipher.updateAAD(it) }

        return cipher.doFinal(ciphertext)
    }
}
