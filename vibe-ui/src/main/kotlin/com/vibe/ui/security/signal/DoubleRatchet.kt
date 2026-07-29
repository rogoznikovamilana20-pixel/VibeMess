package com.vibe.ui.security.signal

import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ⚠️ BROKEN — DO NOT USE.
 *
 * This implementation has critical cryptographic flaws:
 * 1. kdfRootKey() uses incorrect HKDF implementation (missing salt, wrong expansion)
 * 2. Uses X25519KeyExchange which actually uses secp256r1, not X25519
 * 3. Skipped message key management may cause state desync
 *
 * Will be replaced with libsignal-client or a vetted E2EE library in a future phase.
 */
@Deprecated("Broken implementation — incorrect HKDF, wrong curve. Will be replaced.")
class DoubleRatchet(
    private val sharedSecret: ByteArray,
    private val ourPrivateKey: ByteArray,
    private val theirPublicKey: ByteArray,
    private val ourPublicKey: ByteArray = ByteArray(0)
) {
    private val keyExchange = X25519KeyExchange()

    // Root key
    private var rootKey: ByteArray = sharedSecret

    // Sending chain
    private var sendingChainKey: ByteArray? = null
    private var sendingMessageNumber = 0

    // Receiving chain
    private var receivingChainKey: ByteArray? = null
    private var receivingMessageNumber = 0
    private var previousSendingChainLength = 0

    // Skipped message keys for out-of-order messages
    private val skippedMessageKeys = mutableMapOf<Long, ByteArray>()

    /**
     * Encrypt a message using the current sending chain.
     */
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray = ByteArray(0)): EncryptedMessage {
        // Derive message key from sending chain
        val messageKey = deriveSendingMessageKey()

        // Encrypt with AES-256-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = javax.crypto.spec.SecretKeySpec(messageKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)

        // Create header
        val header = MessageHeader(
            dhPublicKey = ourPublicKey,
            previousChainLength = previousSendingChainLength,
            messageNumber = sendingMessageNumber
        )

        // Update state
        sendingMessageNumber++

        return EncryptedMessage(
            header = header,
            ciphertext = ciphertext,
            iv = iv
        )
    }

    /**
     * Decrypt a received message using the receiving chain.
     */
    fun decrypt(message: EncryptedMessage, associatedData: ByteArray = ByteArray(0)): ByteArray {
        // Check for skipped message
        val skipKey = skippedMessageKeys.remove(message.header.messageNumber.toLong())
        if (skipKey != null) {
            return decryptWithKey(message, skipKey)
        }

        // Advance receiving chain if needed
        if (message.header.previousChainLength > previousSendingChainLength) {
            // Perform DH ratchet
            performDHRatchet(message.header.dhPublicKey)
        }

        // Skip ahead if needed
        while (receivingMessageNumber < message.header.messageNumber) {
            val skippedKey = deriveReceivingMessageKey()
            skippedMessageKeys[receivingMessageNumber.toLong()] = skippedKey
            receivingMessageNumber++
        }

        // Derive message key for this message
        val messageKey = deriveReceivingMessageKey()
        receivingMessageNumber++

        return decryptWithKey(message, messageKey)
    }

    private fun deriveSendingMessageKey(): ByteArray {
        val chainKey = sendingChainKey ?: throw IllegalStateException("Sending chain not initialized")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(chainKey, "HmacSHA256"))

        val messageKey = mac.doFinal(byteArrayOf(0x01))
        sendingChainKey = mac.doFinal(byteArrayOf(0x02))

        return messageKey
    }

    private fun deriveReceivingMessageKey(): ByteArray {
        val chainKey = receivingChainKey ?: throw IllegalStateException("Receiving chain not initialized")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(chainKey, "HmacSHA256"))

        val messageKey = mac.doFinal(byteArrayOf(0x01))
        receivingChainKey = mac.doFinal(byteArrayOf(0x02))

        return messageKey
    }

    private fun performDHRatchet(theirPublicKey: ByteArray) {
        // Receive chain
        val dhSecret = keyExchange.performKeyAgreement(ourPrivateKey, theirPublicKey)
        val (newRootKey, newReceivingChainKey) = kdfRootKey(dhSecret)
        rootKey = newRootKey
        receivingChainKey = newReceivingChainKey

        // Update sending chain
        previousSendingChainLength = sendingMessageNumber
        sendingMessageNumber = 0

        // Generate new key pair for sending chain
        val newKeyPair = keyExchange.generateKeyPair()
        val sendingDhSecret = keyExchange.performKeyAgreement(newKeyPair.privateKey, theirPublicKey)
        val (newRootKey2, newSendingChainKey) = kdfRootKey(sendingDhSecret)
        rootKey = newRootKey2
        sendingChainKey = newSendingChainKey
    }

    private fun kdfRootKey(dhOutput: ByteArray): Pair<ByteArray, ByteArray> {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(rootKey, "HmacSHA256"))

        // HKDF expand
        val prk = mac.doFinal(dhOutput)

        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        val output1 = mac.doFinal(byteArrayOf(0x01))
        val output2 = mac.doFinal(output1 + byteArrayOf(0x02))

        return Pair(output1, output2)
    }

    private fun decryptWithKey(message: EncryptedMessage, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val ivSpec = IvParameterSpec(message.iv)
        val keySpec = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(message.ciphertext)
    }
}

data class MessageHeader(
    val dhPublicKey: ByteArray,
    val previousChainLength: Int,
    val messageNumber: Int
)

data class EncryptedMessage(
    val header: MessageHeader,
    val ciphertext: ByteArray,
    val iv: ByteArray
)
