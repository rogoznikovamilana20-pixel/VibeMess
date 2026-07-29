package com.vibe.ui.security.signal

import org.junit.Assert.*
import org.junit.Test

class DoubleRatchetTest {

    private val keyExchange = X25519KeyExchange()

    @Test
    fun `encrypt should throw when sending chain not initialized`() {
        val aliceKeys = keyExchange.generateKeyPair()
        val bobKeys = keyExchange.generateKeyPair()
        val sharedSecret = keyExchange.performKeyAgreement(aliceKeys.privateKey, bobKeys.publicKey)

        val ratchet = DoubleRatchet(
            sharedSecret = sharedSecret,
            ourPrivateKey = aliceKeys.privateKey,
            theirPublicKey = bobKeys.publicKey,
            ourPublicKey = aliceKeys.publicKey
        )

        assertThrows(IllegalStateException::class.java) {
            ratchet.encrypt("hello".toByteArray())
        }
    }

    @Test
    fun `decrypt should throw when receiving chain not initialized`() {
        val aliceKeys = keyExchange.generateKeyPair()
        val bobKeys = keyExchange.generateKeyPair()
        val sharedSecret = keyExchange.performKeyAgreement(aliceKeys.privateKey, bobKeys.publicKey)

        val ratchet = DoubleRatchet(
            sharedSecret = sharedSecret,
            ourPrivateKey = aliceKeys.privateKey,
            theirPublicKey = bobKeys.publicKey,
            ourPublicKey = aliceKeys.publicKey
        )

        val msg = EncryptedMessage(
            header = MessageHeader(
                dhPublicKey = bobKeys.publicKey,
                previousChainLength = 0,
                messageNumber = 0
            ),
            ciphertext = "fake".toByteArray(),
            iv = ByteArray(12)
        )

        assertThrows(IllegalStateException::class.java) {
            ratchet.decrypt(msg)
        }
    }

    @Test
    fun `ciphertext and iv are non-empty after encrypt with synthetic chain`() {
        val aliceKeys = keyExchange.generateKeyPair()
        val bobKeys = keyExchange.generateKeyPair()
        val sharedSecret = keyExchange.performKeyAgreement(aliceKeys.privateKey, bobKeys.publicKey)

        val ratchet = DoubleRatchet(
            sharedSecret = sharedSecret,
            ourPrivateKey = aliceKeys.privateKey,
            theirPublicKey = bobKeys.publicKey,
            ourPublicKey = aliceKeys.publicKey
        )

        val sendingField = DoubleRatchet::class.java.getDeclaredField("sendingChainKey")
        sendingField.isAccessible = true
        sendingField.set(ratchet, sharedSecret)

        val encrypted = ratchet.encrypt("hello".toByteArray())

        assertTrue(encrypted.ciphertext.isNotEmpty())
        assertEquals(12, encrypted.iv.size)
    }
}
