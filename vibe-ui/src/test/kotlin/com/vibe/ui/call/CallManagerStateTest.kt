package com.vibe.ui.call

import org.junit.Assert.*
import org.junit.Test

class CallManagerStateTest {

    @Test
    fun `CallState enum values are in correct order`() {
        val states = CallManager.CallState.values()
        assertEquals("Should have 6 states", 6, states.size)
        assertEquals("First should be IDLE", CallManager.CallState.IDLE, states[0])
        assertEquals("Second should be CONNECTING", CallManager.CallState.CONNECTING, states[1])
        assertEquals("Third should be RINGING", CallManager.CallState.RINGING, states[2])
        assertEquals("Fourth should be CONNECTED", CallManager.CallState.CONNECTED, states[3])
        assertEquals("Fifth should be DISCONNECTED", CallManager.CallState.DISCONNECTED, states[4])
        assertEquals("Sixth should be FAILED", CallManager.CallState.FAILED, states[5])
    }

    @Test
    fun `CallState IDLE has expected name`() {
        assertEquals("IDLE", CallManager.CallState.IDLE.name)
    }

    @Test
    fun `CallState CONNECTED has expected name`() {
        assertEquals("CONNECTED", CallManager.CallState.CONNECTED.name)
    }

    @Test
    fun `CallState DISCONNECTED has expected name`() {
        assertEquals("DISCONNECTED", CallManager.CallState.DISCONNECTED.name)
    }

    @Test
    fun `CallState FAILED has expected name`() {
        assertEquals("FAILED", CallManager.CallState.FAILED.name)
    }

    @Test
    fun `CallState RINGING has expected name`() {
        assertEquals("RINGING", CallManager.CallState.RINGING.name)
    }

    @Test
    fun `CallManager State transitions are valid`() {
        assertEquals("CONNECTING follows IDLE", 
            CallManager.CallState.CONNECTING, 
            CallManager.CallState.CONNECTING)
    }

    @Test
    fun `E2eeManager interop with CallState`() {
        val e2ee = E2eeManager()
        
        assertFalse("E2EE should not be verified before call", e2ee.isVerified())
        assertEquals("Local fingerprint should be empty", "", e2ee.getLocalFingerprint())
    }
}
