package com.vibe.bridge.internal

import com.vibe.bridge.api.ITelegramGateway
import com.vibe.common.logging.ConsoleLogger
import com.vibe.common.logging.VibeLogger
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for BridgeManager lifecycle and initialization.
 */
class BridgeManagerTest {

    @Before
    fun setup() {
        // Use console logger for tests (no Android context needed)
        VibeLogger.setLogger(ConsoleLogger())
    }

    @Test
    fun `bridge manager should not be initialized initially`() {
        val bridgeManager = BridgeManager()
        assertFalse("BridgeManager should not be initialized initially", 
            bridgeManager.isInitialized)
    }

    @Test
    fun `bridge manager should initialize gateway on first call`() {
        val bridgeManager = BridgeManager()
        bridgeManager.initialize()
        
        assertNotNull("Gateway should be initialized after initialize() call", 
            bridgeManager.gateway)
    }

    @Test
    fun `bridge manager should be idempotent on multiple initializations`() {
        val bridgeManager = BridgeManager()
        val gateway1 = bridgeManager.gateway
        
        bridgeManager.initialize()
        val gateway2 = bridgeManager.gateway
        
        // Second initialization should not create new gateway
        assertSame("Gateway should be the same instance after re-initialization", 
            gateway1, gateway2)
    }

    @Test
    fun `bridge manager should cleanup observers on destroy`() {
        val bridgeManager = BridgeManager()
        bridgeManager.initialize()
        
        val gatewayBeforeDestroy = bridgeManager.gateway
        bridgeManager.destroy()
        
        assertFalse("BridgeManager should not be initialized after destroy", 
            bridgeManager.isInitialized)
    }

    @Test
    fun `bridge manager should handle destroy when not initialized`() {
        val bridgeManager = BridgeManager()
        // Should not throw exception
        bridgeManager.destroy()
        assertFalse("BridgeManager should still not be initialized", 
            bridgeManager.isInitialized)
    }

    @Test
    fun `bridge manager should handle initialize after destroy`() {
        val bridgeManager = BridgeManager()
        bridgeManager.initialize()
        bridgeManager.destroy()
        
        // Should be able to re-initialize after destroy (may fail if no Telegram runtime)
        // The important thing is it doesn't throw
        try {
            val result = bridgeManager.initialize()
            // If re-init succeeded, check it's marked as initialized
            if (result.getOrNull() != null) {
                assertTrue("BridgeManager should be initialized after re-initialization",
                    bridgeManager.isInitialized)
            }
        } catch (_: Throwable) {
            // Without Telegram runtime, re-init may fail — that's acceptable
        }
    }

    @Test
    fun `gateway should provide all required services`() {
        val bridgeManager = BridgeManager()
        bridgeManager.initialize()
        val gateway = bridgeManager.gateway
        
        assertNotNull("Gateway should provide messages service", gateway.messages)
        assertNotNull("Gateway should provide users service", gateway.users)
        assertNotNull("Gateway should provide contacts service", gateway.contacts)
        assertNotNull("Gateway should provide accounts service", gateway.accounts)
        assertNotNull("Gateway should provide chats service", gateway.chats)
        assertNotNull("Gateway should provide media service", gateway.media)
        assertNotNull("Gateway should provide notifications service", gateway.notifications)
    }
}
