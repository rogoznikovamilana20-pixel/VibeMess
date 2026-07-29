package com.vibe.ui.call

import org.junit.Assert.*
import org.junit.Test

class E2eeManagerTest {

    private val sampleSdp = """
v=0
o=- 12345 2 IN IP4 0.0.0.0
s=-
t=0 0
a=group:BUNDLE 0 1
a=msid-semantic: WMS
m=audio 9 UDP/TLS/RTP/SAVPF 111 103
c=IN IP4 0.0.0.0
a=rtcp:9 IN IP4 0.0.0.0
a=ice-ufrag:test
a=ice-pwd:test
a=fingerprint:sha-256 AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99
a=setup:actpass
a=mid:0
a=sendrecv
a=rtpmap:111 opus/48000/2
a=rtpmap:103 ISAC/16000
    """.trimIndent()

    private val sdpWithoutFingerprint = """
v=0
o=- 12345 2 IN IP4 0.0.0.0
s=-
t=0 0
m=audio 9 UDP/TLS/RTP/SAVPF 111
c=IN IP4 0.0.0.0
    """.trimIndent()

    private val sampleFingerprint = "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"

    @Test
    fun `extractFingerprint returns fingerprint from valid SDP`() {
        val result = E2eeManager.extractFingerprint(sampleSdp)
        assertNotNull("Fingerprint should not be null", result)
        assertEquals(sampleFingerprint, result)
    }

    @Test
    fun `extractFingerprint returns null when no fingerprint in SDP`() {
        val result = E2eeManager.extractFingerprint(sdpWithoutFingerprint)
        assertNull("Fingerprint should be null", result)
    }

    @Test
    fun `extractFingerprint returns null for empty SDP`() {
        val result = E2eeManager.extractFingerprint("")
        assertNull("Fingerprint should be null for empty SDP", result)
    }

    @Test
    fun `extractFingerprint is case-insensitive and uppercases result`() {
        val sdpLower = sampleSdp.replace("AA", "aa").replace("BB", "bb")
        val result = E2eeManager.extractFingerprint(sdpLower)
        assertEquals("Should uppercase fingerprint", "AA", result?.substringBefore(":"))
    }

    @Test
    fun `generateSafetyNumber is deterministic for same fingerprints`() {
        val fp1 = sampleFingerprint
        val fp2 = "11:22:33:44:55:66:77:88:99:00:AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00:AA:BB:CC:DD:EE:FF"
        val code1 = E2eeManager.generateSafetyNumber(fp1, fp2)
        val code2 = E2eeManager.generateSafetyNumber(fp1, fp2)
        assertEquals("Safety numbers should be identical for same pair", code1, code2)
    }

    @Test
    fun `generateSafetyNumber is order-independent`() {
        val fp1 = sampleFingerprint
        val fp2 = "11:22:33:44:55:66:77:88:99:00:AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00:AA:BB:CC:DD:EE:FF"
        val a = E2eeManager.generateSafetyNumber(fp1, fp2)
        val b = E2eeManager.generateSafetyNumber(fp2, fp1)
        assertEquals("Safety number must be same regardless of argument order", a, b)
    }

    @Test
    fun `generateSafetyNumber returns non-empty for two valid fingerprints`() {
        val fp1 = sampleFingerprint
        val fp2 = "11:22:33:44:55:66:77:88:99:00:AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00:AA:BB:CC:DD:EE:FF"
        val code = E2eeManager.generateSafetyNumber(fp1, fp2)
        assertTrue("Safety number should not be empty", code.isNotEmpty())
    }

    @Test
    fun `generateSafetyNumber returns empty if either fingerprint is empty`() {
        assertTrue("Empty local should give empty", E2eeManager.generateSafetyNumber("", sampleFingerprint).isEmpty())
        assertTrue("Empty remote should give empty", E2eeManager.generateSafetyNumber(sampleFingerprint, "").isEmpty())
        assertTrue("Both empty should give empty", E2eeManager.generateSafetyNumber("", "").isEmpty())
    }

    @Test
    fun `generateShortCode returns consistent code for same fingerprint`() {
        val code1 = E2eeManager.generateShortCode(sampleFingerprint)
        val code2 = E2eeManager.generateShortCode(sampleFingerprint)
        assertEquals("Short codes should be identical for same fingerprint", code1, code2)
    }

    @Test
    fun `generateShortCode returns non-empty for valid fingerprint`() {
        val code = E2eeManager.generateShortCode(sampleFingerprint)
        assertTrue("Short code should not be empty", code.isNotEmpty())
        assertTrue("Short code should contain separator", code.contains("-"))
    }

    @Test
    fun `generateShortCode returns empty for empty fingerprint`() {
        val code = E2eeManager.generateShortCode("")
        assertEquals("Short code should be empty", "", code)
    }

    @Test
    fun `generateVerificationDigits returns 6 digits`() {
        val digits = E2eeManager.generateVerificationDigits()
        assertEquals("Should return 6 characters", 6, digits.length)
        assertTrue("Should only contain digits", digits.all { it.isDigit() })
    }

    @Test
    fun `verify returns current status without confirming`() {
        val manager = E2eeManager()
        manager.setLocalFingerprint(sampleSdp)
        val differentSdp = sampleSdp.replace("AA:BB:CC", "AA:BB:99")
        manager.setRemoteFingerprint(differentSdp)

        val result = manager.verify()
        assertFalse("Verification should be false initially", result.verified)
        assertTrue("Safety number should be present", result.safetyNumber.isNotEmpty())
    }

    @Test
    fun `confirmVerification sets verified true when safety number exists`() {
        val manager = E2eeManager()
        manager.setLocalFingerprint(sampleSdp)
        val differentSdp = sampleSdp.replace("AA:BB:CC", "AA:BB:99")
        manager.setRemoteFingerprint(differentSdp)

        manager.confirmVerification()
        assertTrue("Should be verified after confirmVerification", manager.isVerified())
    }

    @Test
    fun `verify returns false when remote not set`() {
        val manager = E2eeManager()
        manager.setLocalFingerprint(sampleSdp)

        val result = manager.verify()
        assertFalse("Verification should fail without remote", result.verified)
    }

    @Test
    fun `formatFingerprint adds newlines for readability`() {
        val formatted = E2eeManager.formatFingerprint(sampleFingerprint)
        assertTrue("Formatted should contain newlines", formatted.contains("\n"))
        assertEquals("First chunk should be 16 chars", 16, formatted.substringBefore("\n").length)
    }

    @Test
    fun `isVerified returns false initially`() {
        val manager = E2eeManager()
        assertFalse("Should not be verified initially", manager.isVerified())
    }

    @Test
    fun `isVerified returns true after confirmVerification`() {
        val manager = E2eeManager()
        manager.setLocalFingerprint(sampleSdp)
        manager.setRemoteFingerprint(sampleSdp)
        manager.confirmVerification()
        assertTrue("Should be verified after confirmVerification", manager.isVerified())
    }

    @Test
    fun `resetVerification clears verified state`() {
        val manager = E2eeManager()
        manager.setLocalFingerprint(sampleSdp)
        manager.setRemoteFingerprint(sampleSdp)
        manager.confirmVerification()
        assertTrue("Should be verified before reset", manager.isVerified())

        manager.resetVerification()
        assertFalse("Should not be verified after reset", manager.isVerified())
    }

    @Test
    fun `getLocalFingerprint returns set fingerprint`() {
        val manager = E2eeManager()
        manager.setLocalFingerprint(sampleSdp)
        assertEquals(sampleFingerprint, manager.getLocalFingerprint())
    }

    @Test
    fun `getRemoteFingerprint returns set fingerprint`() {
        val manager = E2eeManager()
        manager.setRemoteFingerprint(sampleSdp)
        assertEquals(sampleFingerprint, manager.getRemoteFingerprint())
    }

    @Test
    fun `getSafetyNumber returns generated safety number`() {
        val manager = E2eeManager()
        manager.setLocalFingerprint(sampleSdp)
        val differentSdp = sampleSdp.replace("AA:BB:CC", "AA:BB:99")
        manager.setRemoteFingerprint(differentSdp)

        val safety = manager.getSafetyNumber()
        assertTrue("Safety number should not be empty", safety.isNotEmpty())
        assertTrue("Safety number should contain dashes", safety.contains("-"))
    }
}
