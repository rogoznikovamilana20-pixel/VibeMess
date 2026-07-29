package com.vibe.bridge.internal.media

import com.vibe.bridge.api.IMediaService
import com.vibe.bridge.model.DownloadStatus
import com.vibe.bridge.model.VibeMessageAttachment
import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import com.vibe.common.logging.VibeLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal class TelegramMediaService : IMediaService, NotificationCenter.NotificationCenterDelegate, IMediaRegistry {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val registry = ConcurrentHashMap<String, PendingDownload>() // fileName -> PendingDownload
    private val activeFlows = ConcurrentHashMap<String, MutableSharedFlow<DownloadStatus>>() // fileId -> Flow

    private data class PendingDownload(
        val fileId: String,
        val account: Int,
        val telegramObject: Any,
        val expectedFileName: String,
        val createdAt: Long = System.currentTimeMillis()
    )

    init {
        // Cleanup task for stale registry entries
        serviceScope.launch {
            while (isActive) {
                delay(60000L) // Every minute
                val now = System.currentTimeMillis()
                val iterator = registry.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (now - entry.value.createdAt > 300000L) { // 5 minutes TTL
                        iterator.remove()
                    }
                }
            }
        }
    }

    /**
     * Internal hook for MessageMapper (via TelegramCoreAdapter) to register 
     * discovered media objects.
     */
    override fun registerMedia(fileId: String, account: Int, telegramObject: Any) {
        val fileName = TelegramCoreAdapter.getAttachFileName(telegramObject) ?: return
        registry[fileName] = PendingDownload(
            fileId = fileId,
            account = account,
            telegramObject = telegramObject,
            expectedFileName = fileName
        )
    }

    override fun downloadMedia(attachment: VibeMessageAttachment) {
        val pending = findPending(attachment.fileId) ?: return
        VibeLogger.d("TelegramMediaService", "START DOWNLOAD: ${attachment.fileId} (${pending.account})")
        TelegramCoreAdapter.startDownload(pending.account, pending.telegramObject, 1)
    }

    override fun cancelDownload(attachment: VibeMessageAttachment) {
        val pending = findPending(attachment.fileId) ?: return
        VibeLogger.i("TelegramMediaService", "DOWNLOAD CANCELLED: ${attachment.fileId}")
        TelegramCoreAdapter.cancelDownload(pending.account, pending.telegramObject)
        activeFlows.remove(attachment.fileId)
    }

    override fun observeDownload(attachment: VibeMessageAttachment): Flow<DownloadStatus> {
        return activeFlows.getOrPut(attachment.fileId) {
            MutableSharedFlow<DownloadStatus>(replay = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST).apply {
                tryEmit(DownloadStatus(attachment.fileId, 0f, false, null))
                
                // Safety timeout for the flow
                serviceScope.launch {
                    delay(600000L) // 10 minutes
                    if (activeFlows.containsKey(attachment.fileId)) {
                        VibeLogger.w("TelegramMediaService", "FLOW TIMEOUT: ${attachment.fileId}")
                        tryEmit(DownloadStatus(attachment.fileId, 0f, false, null, "Download timeout"))
                        activeFlows.remove(attachment.fileId)
                        VibeLogger.d("TelegramMediaService", "FLOW REMOVED (Timeout): ${attachment.fileId}")
                    }
                }
            }
        }.asSharedFlow()
    }

    private fun findPending(fileId: String): PendingDownload? {
        return registry.values.find { it.fileId == fileId }
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        when (id) {
            NotificationCenter.fileLoaded -> {
                if (args.size < 2) return
                val fileName = args[0] as? String ?: return
                val localFile = args[1] as? File ?: return
                
                val pending = registry.remove(fileName) ?: return
                activeFlows.remove(pending.fileId)?.let { flow ->
                    VibeLogger.i("TelegramMediaService", "DOWNLOAD SUCCESS: ${pending.fileId} -> ${localFile.name}")
                    flow.tryEmit(DownloadStatus(pending.fileId, 1f, true, localFile.absolutePath))
                    VibeLogger.d("TelegramMediaService", "FLOW REMOVED (Success): ${pending.fileId}")
                }
            }
            NotificationCenter.fileLoadFailed -> {
                if (args.size < 2) return
                val fileName = args[0] as? String ?: return
                val reason = args[1] as? Int ?: 0
                
                val pending = registry.remove(fileName) ?: return
                activeFlows.remove(pending.fileId)?.let { flow ->
                    VibeLogger.e("TelegramMediaService", "DOWNLOAD FAILED: ${pending.fileId} (reason: $reason)")
                    flow.tryEmit(DownloadStatus(pending.fileId, 0f, false, null, "Error code: $reason"))
                    VibeLogger.d("TelegramMediaService", "FLOW REMOVED (Failed): ${pending.fileId}")
                }
            }
            NotificationCenter.fileLoadProgressChanged -> {
                if (args.size < 3) return
                val fileName = args[0] as? String ?: return
                val loaded = args[1] as? Long ?: return
                val total = args[2] as? Long ?: return
                
                val pending = registry[fileName] ?: return
                val progress = if (total > 0) loaded.toFloat() / total else 0f
                activeFlows[pending.fileId]?.tryEmit(
                    DownloadStatus(pending.fileId, progress, false, null)
                )
            }
        }
    }
    
    fun cleanup() {
        serviceScope.cancel()
        registry.clear()
        activeFlows.clear()
    }
}
