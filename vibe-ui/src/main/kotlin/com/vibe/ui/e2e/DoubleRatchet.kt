package com.vibe.ui.e2e

import android.util.Base64
import android.util.Log
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.GCMParameterSpec

/**
 * Double Ratchet Algorithm implementation.
 * Based on Signal Protocol specification.
 *
 * Properties:
 * - Forward Secrecy: compromise of long-term keys doesn't compromise past sessions
 * - Break-in Recovery: compromise of session keys doesn't compromise future sessions
 * - Message Key Ratcheting: each message has a unique encryption key
 *
 * Reference: https://signal.org/docs/specifications/doubleratchet/
 */
class DoubleRatchet {

    companion object {
        private const val TAG = "DoubleRatchet"
        private const val MAX_SKIP = 1000 // Maximum number of skipped message keys
        private const val RATCHET_TERMINATED = -1
    }

    // Session state
    private var rootKey: ByteArray? = null
    private var sendingChainKey: ByteArray? = null
    private var receivingChainKey: ByteArray? = null
    private var sendingRatchetCounter: Int = 0
    private var receivingRatchetCounter: Int = 0
    private var previousSendingRatchetCounter: Int = 0

    // Skipped message keys (for out-of-order messages)
    private val skippedMessageKeys = HashMap<String, ByteArray>()

    // Our current ratchet public/private key pair
    private var currentRatchetKeyPair: RatchetKeyPair? = null
    private var remoteRatchetPublicKey: ByteArray? = null

    /**
     * Initialize the ratchet with a shared secret from key exchange.
     */
    fun initialize(sharedSecret: ByteArray, isAlice: Boolean) {
        rootKey = sharedSecret.copyOf()

        if (isAlice) {
            currentRatchetKeyPair = generateRatchetKeyPair()
        }

        Log.d(TAG, "Ratchet initialized (isAlice=$isAlice)")
    }

    /**
     * Set Bob's initial ratchet key pair (his pre-key pair from X3DH bundle).
     * Must be called before the first decrypt for the receiving side.
     */
    fun setInitialRatchetKeyPair(keyPair: RatchetKeyPair) {
        currentRatchetKeyPair = keyPair
    }

    /**
     * Get current ratchet public key to send to the other party.
     */
    fun getCurrentRatchetPublicKey(): ByteArray? = currentRatchetKeyPair?.publicKey

    /**
     * Set the remote party's ratchet public key (received in a message).
     */
    fun setRemoteRatchetPublicKey(remoteKey: ByteArray) {
        remoteRatchetPublicKey = remoteKey
    }

    /**
     * Encrypt a message using the current sending chain.
     * Returns encrypted message with ratchet metadata.
     */
    fun encrypt(plaintext: ByteArray): RatchetMessage {
        // Advance sending chain
        val messageKey = advanceSendingChain()

        // Encrypt with AES-256-GCM (authentication tag is automatically appended)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val ciphertext = encryptGCM(plaintext, messageKey, iv)

        // Zeroize message key after use
        CryptoUtils.zeroize(messageKey)

        // Build message with ratchet header
        val header = RatchetHeader(
            senderRatchetPublicKey = currentRatchetKeyPair?.publicKey,
            previousSendingChainLength = previousSendingRatchetCounter,
            messageNumber = sendingRatchetCounter - 1
        )

        return RatchetMessage(
            header = header,
            ciphertext = ciphertext,
            iv = iv
        )
    }

    /**
     * Decrypt a received message.
     * Handles ratchet advancement and out-of-order messages.
     */
    fun decrypt(message: RatchetMessage): ByteArray? {
        val header = message.header

        // Check if we have a skipped message key for this message
        val skipKey = "${Base64.encodeToString(header.senderRatchetPublicKey, Base64.NO_WRAP)}_${header.messageNumber}"
        val skippedKey = skippedMessageKeys.remove(skipKey)
        if (skippedKey != null) {
            val plaintext = decryptWithKey(message.ciphertext, message.iv, skippedKey)
            CryptoUtils.zeroize(skippedKey)
            return plaintext
        }

        // First message: learn sender's ratchet key and perform initial DH
        if (remoteRatchetPublicKey == null) {
            if (header.senderRatchetPublicKey == null) {
                Log.e(TAG, "Missing sender ratchet key in first message")
                return null
            }
            // Perform initial receiving chain derivation via DH
            ratchetReceivingChain(header.senderRatchetPublicKey)
        } else if (header.senderRatchetPublicKey == null) {
            Log.e(TAG, "Missing sender ratchet key")
            return null
        } else if (!CryptoUtils.constantTimeEquals(header.senderRatchetPublicKey, remoteRatchetPublicKey!!)) {
            // Skip any messages from the old receiving chain
            val skippedKeys = skipMessageKeys(header.previousSendingChainLength)
            saveSkippedMessageKeys(skippedKeys, header.senderRatchetPublicKey, header.previousSendingChainLength)

            // Perform receiving chain ratchet
            ratchetReceivingChain(header.senderRatchetPublicKey!!)
        }

        // Skip messages if needed (out of order within same ratchet)
        val skippedKeys = skipMessageKeys(header.messageNumber)
        saveSkippedMessageKeys(skippedKeys, remoteRatchetPublicKey, header.messageNumber)

        // Advance receiving chain to get the message key
        val messageKey = advanceReceivingChain()

        return try {
            val plaintext = decryptGCM(message.ciphertext, messageKey, message.iv)
            CryptoUtils.zeroize(messageKey)
            plaintext
        } catch (e: javax.crypto.BadPaddingException) {
            // Authentication tag verification failed - possible tampering
            CryptoUtils.zeroize(messageKey)
            Log.e(TAG, "GCM authentication failed - message may be tampered", e)
            null
        } catch (e: javax.crypto.IllegalBlockSizeException) {
            CryptoUtils.zeroize(messageKey)
            Log.e(TAG, "Invalid block size", e)
            null
        }
    }

    /**
     * Check if the ratchet is initialized.
     */
    fun isInitialized(): Boolean = rootKey != null

    /**
     * Export current session state for persistence.
     */
    fun exportState(): RatchetState {
        return RatchetState(
            rootKey = rootKey?.copyOf(),
            sendingChainKey = sendingChainKey?.copyOf(),
            receivingChainKey = receivingChainKey?.copyOf(),
            sendingRatchetCounter = sendingRatchetCounter,
            receivingRatchetCounter = receivingRatchetCounter,
            previousSendingRatchetCounter = previousSendingRatchetCounter,
            currentRatchetKeyPair = currentRatchetKeyPair,
            remoteRatchetPublicKey = remoteRatchetPublicKey?.copyOf(),
            skippedMessageKeys = HashMap(skippedMessageKeys.mapValues { it.value.copyOf() })
        )
    }

    /**
     * Import session state from persistence.
     */
    fun importState(state: RatchetState) {
        rootKey = state.rootKey?.copyOf()
        sendingChainKey = state.sendingChainKey?.copyOf()
        receivingChainKey = state.receivingChainKey?.copyOf()
        sendingRatchetCounter = state.sendingRatchetCounter
        receivingRatchetCounter = state.receivingRatchetCounter
        previousSendingRatchetCounter = state.previousSendingRatchetCounter
        currentRatchetKeyPair = state.currentRatchetKeyPair
        remoteRatchetPublicKey = state.remoteRatchetPublicKey?.copyOf()
        skippedMessageKeys.clear()
        skippedMessageKeys.putAll(state.skippedMessageKeys.mapValues { it.value.copyOf() })

        Log.d(TAG, "Ratchet state imported (counter: $sendingRatchetCounter)")
    }

    // --- Internal ratchet operations ---

    /**
     * Derive a message key and advance the sending chain.
     */
    private fun advanceSendingChain(): ByteArray {
        if (sendingChainKey == null) {
            if (currentRatchetKeyPair == null || remoteRatchetPublicKey == null || rootKey == null) {
                throw IllegalStateException("Ratchet not properly initialized: missing key pair, remote key, or root key")
            }
            val dhResult = performDH(currentRatchetKeyPair!!.privateKey, remoteRatchetPublicKey!!)
            val (newRootKey, newChainKey) = kdfRootKey(rootKey!!, dhResult)
            rootKey = newRootKey
            sendingChainKey = newChainKey
        }

        val (messageKey, newChainKey) = kdfChainKey(sendingChainKey!!)
        sendingChainKey = newChainKey
        sendingRatchetCounter++

        return messageKey
    }

    /**
     * Derive a message key and advance the receiving chain.
     */
    private fun advanceReceivingChain(): ByteArray {
        val (messageKey, newChainKey) = kdfChainKey(receivingChainKey!!)
        receivingChainKey = newChainKey
        receivingRatchetCounter++

        return messageKey
    }

    /**
     * Perform receiving chain ratchet when receiving a new ratchet key.
     */
    private fun ratchetReceivingChain(newRemotePublicKey: ByteArray) {
        if (rootKey == null) {
            Log.e(TAG, "Cannot ratchet receiving chain: root key is null")
            return
        }
        if (currentRatchetKeyPair == null) {
            currentRatchetKeyPair = generateRatchetKeyPair()
        }
        previousSendingRatchetCounter = sendingRatchetCounter
        sendingRatchetCounter = 0
        receivingRatchetCounter = 0

        // DH with new remote key
        val dhResult = performDH(currentRatchetKeyPair!!.privateKey, newRemotePublicKey)

        // Update root key and derive new receiving chain
        val (newRootKey, newChainKey) = kdfRootKey(rootKey!!, dhResult)
        rootKey = newRootKey
        receivingChainKey = newChainKey

        // Generate new ratchet key pair and update sending chain
        currentRatchetKeyPair = generateRatchetKeyPair()
        val dhResult2 = performDH(currentRatchetKeyPair!!.privateKey, newRemotePublicKey)
        val (newRootKey2, newSendingChainKey) = kdfRootKey(rootKey!!, dhResult2)
        rootKey = newRootKey2
        sendingChainKey = newSendingChainKey

        remoteRatchetPublicKey = newRemotePublicKey

        Log.d(TAG, "Receiving chain ratcheted")
    }

    /**
     * Skip message keys for out-of-order messages.
     */
    private fun skipMessageKeys(until: Int): List<ByteArray> {
        val skippedKeys = mutableListOf<ByteArray>()
        val chain = receivingChainKey ?: return skippedKeys
        val counter = receivingRatchetCounter

        var currentChain = chain
        var i = counter
        while (i < until && i - counter < MAX_SKIP) {
            val (mk, newChain) = kdfChainKey(currentChain)
            skippedKeys.add(mk)
            currentChain = newChain
            i++
        }

        if (i < until) {
            Log.w(TAG, "Skipped too many messages, some may be lost")
        }

        receivingChainKey = currentChain
        receivingRatchetCounter = i

        return skippedKeys
    }

    /**
     * Save skipped message keys for later decryption.
     */
    private fun saveSkippedMessageKeys(keys: List<ByteArray>, ratchetKey: ByteArray?, startCounter: Int) {
        ratchetKey ?: return
        val ratchetKeyB64 = Base64.encodeToString(ratchetKey, Base64.NO_WRAP)
        keys.forEachIndexed { index, key ->
            val skipKey = "${ratchetKeyB64}_${startCounter + index}"
            skippedMessageKeys[skipKey] = key
        }
    }

    /**
     * Encrypt data with AES-256-GCM.
     * Returns ciphertext with appended authentication tag.
     */
    private fun encryptGCM(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"), spec)

        // Add associated data for authentication (optional but recommended)
        cipher.updateAAD("vibe-e2e-v1".toByteArray(Charsets.UTF_8))

        return cipher.doFinal(plaintext)
    }

    /**
     * Decrypt data with AES-256-GCM.
     * Verifies authentication tag before returning plaintext.
     * Throws BadPaddingException if authentication fails.
     */
    private fun decryptGCM(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"), spec)

        // Add associated data for authentication
        cipher.updateAAD("vibe-e2e-v1".toByteArray(Charsets.UTF_8))

        // doFinal() will throw BadPaddingException if authentication tag verification fails
        return cipher.doFinal(ciphertext)
    }

    // --- Cryptographic primitives ---

    /**
     * Derive a message key and next chain key from the current chain key.
     * Uses HMAC-SHA256.
     */
    private fun kdfChainKey(chainKey: ByteArray): Pair<ByteArray, ByteArray> {
        val mac = Mac.getInstance("HmacSHA256")

        // Message key = HMAC(chain_key, 0x01)
        mac.init(SecretKeySpec(chainKey, "HmacSHA256"))
        val messageKey = mac.doFinal(byteArrayOf(0x01))

        // Next chain key = HMAC(chain_key, 0x02)
        mac.init(SecretKeySpec(chainKey, "HmacSHA256"))
        val nextChainKey = mac.doFinal(byteArrayOf(0x02))

        return Pair(messageKey, nextChainKey)
    }

    /**
     * Derive a new root key and chain key from current root key + DH result.
     * Uses HKDF-like construction.
     */
    private fun kdfRootKey(rootKey: ByteArray, dhResult: ByteArray): Pair<ByteArray, ByteArray> {
        val mac = Mac.getInstance("HmacSHA256")

        // New root key = HMAC(root_key, dh_result || 0x01)
        mac.init(SecretKeySpec(rootKey, "HmacSHA256"))
        val newRootKey = mac.doFinal(dhResult + byteArrayOf(0x01))

        // Chain key = HMAC(root_key, dh_result || 0x02)
        mac.init(SecretKeySpec(rootKey, "HmacSHA256"))
        val chainKey = mac.doFinal(dhResult + byteArrayOf(0x02))

        return Pair(newRootKey, chainKey)
    }

    /**
     * Perform Diffie-Hellman key agreement.
     */
    private fun performDH(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        val kf = java.security.KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        val ka = javax.crypto.KeyAgreement.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)

        val privKey = kf.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privateKey))
        val pubKey = kf.generatePublic(java.security.spec.X509EncodedKeySpec(publicKey))

        ka.init(privKey)
        ka.doPhase(pubKey, true)
        return ka.generateSecret()
    }

    /**
     * Generate a new ratchet key pair.
     */
    private fun generateRatchetKeyPair(): RatchetKeyPair {
        val kpg = java.security.KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        kpg.initialize(java.security.spec.ECGenParameterSpec("curve25519"))
        val keyPair = kpg.generateKeyPair()
        return RatchetKeyPair(
            publicKey = keyPair.public.encoded,
            privateKey = keyPair.private.encoded
        )
    }

    /**
     * Decrypt with a specific key (for skipped messages).
     */
    private fun decryptWithKey(ciphertext: ByteArray, iv: ByteArray, key: ByteArray): ByteArray {
        return try {
            decryptGCM(ciphertext, key, iv)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed for skipped message", e)
            throw e
        }
    }
}

/**
 * Ratchet key pair for Diffie-Hellman operations.
 */
class RatchetKeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RatchetKeyPair) return false
        return publicKey.contentEquals(other.publicKey) && privateKey.contentEquals(other.privateKey)
    }
    override fun hashCode(): Int = publicKey.contentHashCode() * 31 + privateKey.contentHashCode()
    override fun toString(): String = "RatchetKeyPair(pubKey=${publicKey.contentToString()})"
}

/**
 * Encrypted message with ratchet metadata.
 */
class RatchetMessage(
    val header: RatchetHeader,
    val ciphertext: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RatchetMessage) return false
        return header == other.header && ciphertext.contentEquals(other.ciphertext) && iv.contentEquals(other.iv)
    }
    override fun hashCode(): Int = header.hashCode() * 31 + ciphertext.contentHashCode() + iv.contentHashCode()
    override fun toString(): String = "RatchetMessage(header=$header, cipherLen=${ciphertext.size})"
}

/**
 * Ratchet header sent with each message.
 */
class RatchetHeader(
    val senderRatchetPublicKey: ByteArray?,
    val previousSendingChainLength: Int,
    val messageNumber: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RatchetHeader) return false
        return senderRatchetPublicKey.contentEquals(other.senderRatchetPublicKey)
            && previousSendingChainLength == other.previousSendingChainLength
            && messageNumber == other.messageNumber
    }
    override fun hashCode(): Int {
        var result = (senderRatchetPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + previousSendingChainLength
        result = 31 * result + messageNumber
        return result
    }
    override fun toString(): String = "RatchetHeader(prevLen=$previousSendingChainLength, msgNum=$messageNumber)"
}

/**
 * Serializable ratchet state for persistence.
 */
class RatchetState(
    val rootKey: ByteArray?,
    val sendingChainKey: ByteArray?,
    val receivingChainKey: ByteArray?,
    val sendingRatchetCounter: Int,
    val receivingRatchetCounter: Int,
    val previousSendingRatchetCounter: Int,
    val currentRatchetKeyPair: RatchetKeyPair?,
    val remoteRatchetPublicKey: ByteArray?,
    val skippedMessageKeys: Map<String, ByteArray>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RatchetState) return false
        return rootKey.contentEquals(other.rootKey)
            && sendingChainKey.contentEquals(other.sendingChainKey)
            && receivingChainKey.contentEquals(other.receivingChainKey)
            && sendingRatchetCounter == other.sendingRatchetCounter
            && receivingRatchetCounter == other.receivingRatchetCounter
            && previousSendingRatchetCounter == other.previousSendingRatchetCounter
            && currentRatchetKeyPair == other.currentRatchetKeyPair
            && remoteRatchetPublicKey.contentEquals(other.remoteRatchetPublicKey)
    }
    override fun hashCode(): Int {
        var result = (rootKey?.contentHashCode() ?: 0)
        result = 31 * result + (sendingChainKey?.contentHashCode() ?: 0)
        result = 31 * result + (receivingChainKey?.contentHashCode() ?: 0)
        result = 31 * result + sendingRatchetCounter
        result = 31 * result + receivingRatchetCounter
        result = 31 * result + previousSendingRatchetCounter
        result = 31 * result + (currentRatchetKeyPair?.hashCode() ?: 0)
        result = 31 * result + (remoteRatchetPublicKey?.contentHashCode() ?: 0)
        return result
    }
}
