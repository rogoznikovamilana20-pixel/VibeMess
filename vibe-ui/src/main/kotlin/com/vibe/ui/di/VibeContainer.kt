package com.vibe.ui.di

import com.vibe.common.logging.VibeLogger
import com.vibe.bridge.api.ITelegramGateway
import com.vibe.bridge.internal.BridgeManager

/**
 * Simple DI container for Vibe UI dependencies.
 */
object VibeContainer {
    private const val TAG = "VibeContainer"
    private var bridgeManager: BridgeManager? = null
    private var gateway: ITelegramGateway? = null
    private var initializationError: Throwable? = null

    /**
     * Test-only: inject a mock BridgeManager for unit testing.
     */
    internal fun setTestBridgeManager(mock: BridgeManager, mockGateway: ITelegramGateway) {
        bridgeManager = mock
        gateway = mockGateway
        initializationError = null
    }

    fun initialize(onError: ((Throwable) -> Unit)? = null): Boolean {
        if (bridgeManager != null) return true
        
        val mgr = BridgeManager()
        
        if (onError != null) {
            mgr.onError(onError)
        } else {
            mgr.onError { e ->
                initializationError = e
                VibeLogger.e(TAG, "Bridge error", e)
            }
        }
        
        val initResult = mgr.initialize()
        initResult.fold(
            onSuccess = {
                bridgeManager = mgr
                gateway = mgr.gateway
                VibeLogger.i(TAG, "Vibe Bridge initialized successfully")
            },
            onFailure = { e ->
                initializationError = e
                VibeLogger.e(TAG, "Failed to initialize Vibe Bridge", e)
            }
        )
        return bridgeManager != null
    }

    fun getGateway(): ITelegramGateway {
        return gateway ?: run {
            VibeLogger.w(TAG, "Gateway not available, attempting fallback initialization")
            initialize()
            gateway ?: throw IllegalStateException("Vibe Bridge initialization failed. See logs for details.")
        }
    }

    fun getBridgeManager(): BridgeManager {
        return bridgeManager ?: run {
            VibeLogger.w(TAG, "BridgeManager not available, attempting fallback initialization")
            initialize()
            bridgeManager ?: throw IllegalStateException("Vibe Bridge initialization failed. See logs for details.")
        }
    }

    fun isInitialized(): Boolean {
        return bridgeManager != null && gateway != null
    }

    fun getInitializationError(): Throwable? {
        return initializationError
    }

    fun destroy() {
        try {
            bridgeManager?.destroy()
            VibeLogger.i(TAG, "Vibe Bridge destroyed successfully")
        } catch (e: Exception) {
            VibeLogger.e(TAG, "Error destroying Vibe Bridge", e)
        } finally {
            bridgeManager = null
            gateway = null
            initializationError = null
        }
    }
}
