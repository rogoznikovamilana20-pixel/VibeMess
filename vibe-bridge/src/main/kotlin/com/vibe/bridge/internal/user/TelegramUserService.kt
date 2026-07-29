package com.vibe.bridge.internal.user

import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import com.vibe.bridge.api.IUserService
import com.vibe.bridge.model.VibeUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig
import com.vibe.common.logging.VibeLogger

/**
 * Implementation of [IUserService] that wraps Telegram's user logic.
 */
internal class TelegramUserService : IUserService, NotificationCenter.NotificationCenterDelegate {

    private val _userUpdates = MutableSharedFlow<VibeUser>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        // We register for updates in Application or a global scope, 
        // but here we just prepare the Flow.
    }

    override suspend fun getUser(userId: Long): VibeUser? = withContext(Dispatchers.Default) {
        val account = UserConfig.selectedAccount
        TelegramCoreAdapter.getUser(userId, account)
    }

    override suspend fun getUsers(userIds: List<Long>): List<VibeUser> = withContext(Dispatchers.Default) {
        val account = UserConfig.selectedAccount
        TelegramCoreAdapter.getUsers(userIds, account)
    }

    override fun observeUserUpdates(): Flow<VibeUser> = _userUpdates.asSharedFlow()

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        when (id) {
            NotificationCenter.userInfoDidLoad -> {
                if (args.isEmpty()) {
                    VibeLogger.w("TelegramUserService", "userInfoDidLoad: empty args")
                    return
                }
                val userId = args[0] as? Long ?: return
                TelegramCoreAdapter.getUser(userId, account)?.let { _userUpdates.tryEmit(it) }
            }
            NotificationCenter.mainUserInfoChanged -> {
                val currentUserId = UserConfig.getInstance(account).clientUserId
                TelegramCoreAdapter.getUser(currentUserId, account)?.let { _userUpdates.tryEmit(it) }
            }
            NotificationCenter.updateInterfaces -> {
                if (args.isEmpty()) return
                val mask = args[0] as? Int ?: 0
                if (mask and MessagesController.UPDATE_MASK_AVATAR != 0 || 
                    mask and MessagesController.UPDATE_MASK_NAME != 0 ||
                    mask and MessagesController.UPDATE_MASK_STATUS != 0) {
                    
                    VibeLogger.d("TelegramUserService", "updateInterfaces received with mask $mask")
                    // Note: updateInterfaces usually means generic UI refresh. 
                    // Individual user updates are handled via userInfoDidLoad.
                }
            }
        }
    }
}
