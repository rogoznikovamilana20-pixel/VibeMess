package com.vibe.ui.di

import com.vibe.bridge.api.ITelegramGateway
import com.vibe.bridge.internal.BridgeManager
import com.vibe.common.logging.ConsoleLogger
import com.vibe.common.logging.VibeLogger
import io.mockk.mockk
import io.mockk.every
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class VibeContainerTest {

    private lateinit var mockBridgeManager: BridgeManager
    private lateinit var mockGateway: ITelegramGateway

    @Before
    fun setup() {
        VibeContainer.destroy()
        VibeLogger.setLogger(ConsoleLogger())

        mockBridgeManager = mockk()
        mockGateway = mockk()
        every { mockBridgeManager.isInitialized } returns true
    }

    @After
    fun teardown() {
        VibeContainer.destroy()
    }

    @Test
    fun `container should not be initialized initially`() {
        assertFalse("Container should not be initialized initially",
            VibeContainer.isInitialized())
    }

    @Test
    fun `container should initialize successfully`() {
        VibeContainer.setTestBridgeManager(mockBridgeManager, mockGateway)
        assertTrue("Container should be initialized after initialize() call",
            VibeContainer.isInitialized())
    }

    @Test
    fun `container should be idempotent on multiple initializations`() {
        VibeContainer.setTestBridgeManager(mockBridgeManager, mockGateway)
        VibeContainer.setTestBridgeManager(mockBridgeManager, mockGateway)
        assertTrue("Container should still be initialized",
            VibeContainer.isInitialized())
    }

    @Test
    fun `container should provide gateway after initialization`() {
        VibeContainer.setTestBridgeManager(mockBridgeManager, mockGateway)
        val gateway = VibeContainer.getGateway()
        assertNotNull("Gateway should not be null after initialization", gateway)
    }

    @Test
    fun `container should provide bridge manager after initialization`() {
        VibeContainer.setTestBridgeManager(mockBridgeManager, mockGateway)
        val bridgeManager = VibeContainer.getBridgeManager()
        assertNotNull("BridgeManager should not be null after initialization", bridgeManager)
    }

    @Test
    fun `container should throw exception when gateway requested before initialization`() {
        try {
            VibeContainer.getGateway()
            fail("Should throw IllegalStateException when gateway requested before initialization")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message should mention initialization",
                e.message?.contains("initialization") == true)
        }
    }

    @Test
    fun `container should throw exception when bridge manager requested before initialization`() {
        try {
            VibeContainer.getBridgeManager()
            fail("Should throw IllegalStateException when bridge manager requested before initialization")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message should mention initialization",
                e.message?.contains("initialization") == true)
        }
    }

    @Test
    fun `container should cleanup on destroy`() {
        VibeContainer.setTestBridgeManager(mockBridgeManager, mockGateway)
        VibeContainer.destroy()
        assertFalse("Container should not be initialized after destroy",
            VibeContainer.isInitialized())
    }

    @Test
    fun `container should handle destroy when not initialized`() {
        VibeContainer.destroy()
        assertFalse("Container should still not be initialized",
            VibeContainer.isInitialized())
    }

    @Test
    fun `container should handle initialize after destroy`() {
        VibeContainer.setTestBridgeManager(mockBridgeManager, mockGateway)
        VibeContainer.destroy()

        VibeContainer.setTestBridgeManager(mockBridgeManager, mockGateway)
        assertTrue("Container should be initialized after re-initialization",
            VibeContainer.isInitialized())
    }

    @Test
    fun `container should throw exception when gateway requested after destroy`() {
        VibeContainer.setTestBridgeManager(mockBridgeManager, mockGateway)
        VibeContainer.destroy()

        try {
            VibeContainer.getGateway()
            fail("Should throw IllegalStateException when gateway requested after destroy")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message should mention initialization",
                e.message?.contains("initialization") == true)
        }
    }

    @Test
    fun `container should return null initialization error when successful`() {
        VibeContainer.setTestBridgeManager(mockBridgeManager, mockGateway)
        assertNull("Initialization error should be null when successful",
            VibeContainer.getInitializationError())
    }
}
