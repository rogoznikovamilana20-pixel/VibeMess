package com.vibe.ui.e2e

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.Security

class DoubleRatchetTest {

    @Before
    fun setup() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
        }
    }

    private fun generateCurve25519KeyPair(): RatchetKeyPair {
        val kpg = java.security.KeyPairGenerator.getInstance("EC", "BC")
        kpg.initialize(java.security.spec.ECGenParameterSpec("curve25519"))
        val kp = kpg.generateKeyPair()
        return RatchetKeyPair(
            publicKey = kp.public.encoded,
            privateKey = kp.private.encoded
        )
    }

    @Test
    fun `basic ratchet encrypt and decrypt`() {
        val aliceRatchet = DoubleRatchet()
        val bobRatchet = DoubleRatchet()
        val sharedSecret = CryptoUtils.generateSalt(32)

        aliceRatchet.initialize(sharedSecret, isAlice = true)
        bobRatchet.initialize(sharedSecret, isAlice = false)

        // Bob's pre-key pair (simulated X3DH bundle)
        val bobPreKeyPair = generateCurve25519KeyPair()

        // Alice sets Bob's pre-key public as remote ratchet key
        aliceRatchet.setRemoteRatchetPublicKey(bobPreKeyPair.publicKey)

        // Bob sets his pre-key pair as initial ratchet key pair
        bobRatchet.setInitialRatchetKeyPair(bobPreKeyPair)

        // Alice sends
        val message1 = aliceRatchet.encrypt("Hello Bob!".toByteArray())

        // Bob decrypts — ECDH is commutative: DH(alice_priv, bob_pub) = DH(bob_priv, alice_pub)
        val decrypted1 = bobRatchet.decrypt(message1)
        assertNotNull("Bob should decrypt Alice's message", decrypted1)
        assertEquals("Hello Bob!", String(decrypted1!!))
    }

    @Test
    fun `bidirectional ratchet exchange`() {
        val aliceRatchet = DoubleRatchet()
        val bobRatchet = DoubleRatchet()
        val sharedSecret = CryptoUtils.generateSalt(32)

        aliceRatchet.initialize(sharedSecret, isAlice = true)
        bobRatchet.initialize(sharedSecret, isAlice = false)

        val bobPreKeyPair = generateCurve25519KeyPair()
        aliceRatchet.setRemoteRatchetPublicKey(bobPreKeyPair.publicKey)
        bobRatchet.setInitialRatchetKeyPair(bobPreKeyPair)

        // Alice sends
        val msg1 = aliceRatchet.encrypt("Alice 1".toByteArray())
        val dec1 = bobRatchet.decrypt(msg1)
        assertEquals("Alice 1", String(dec1!!))

        // Bob replies
        val msg2 = bobRatchet.encrypt("Bob 1".toByteArray())
        val dec2 = aliceRatchet.decrypt(msg2)
        assertEquals("Bob 1", String(dec2!!))

        // Alice sends again
        val msg3 = aliceRatchet.encrypt("Alice 2".toByteArray())
        val dec3 = bobRatchet.decrypt(msg3)
        assertEquals("Alice 2", String(dec3!!))
    }

    @Test
    fun `RatchetMessage contains correct structure`() {
        val aliceRatchet = DoubleRatchet()
        val sharedSecret = CryptoUtils.generateSalt(32)
        aliceRatchet.initialize(sharedSecret, isAlice = true)

        val bobPreKeyPair = generateCurve25519KeyPair()
        aliceRatchet.setRemoteRatchetPublicKey(bobPreKeyPair.publicKey)

        val message = aliceRatchet.encrypt("test".toByteArray())

        assertNotNull(message.header)
        assertNotNull(message.header.senderRatchetPublicKey)
        assertNotNull(message.ciphertext)
        assertNotNull(message.iv)
        assertEquals(12, message.iv.size)
        assertTrue(message.ciphertext.isNotEmpty())
    }
}
