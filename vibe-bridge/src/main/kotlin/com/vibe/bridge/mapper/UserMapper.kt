package com.vibe.bridge.mapper

import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import com.vibe.bridge.model.VibeUser
import org.telegram.tgnet.TLRPC

/**
 * Mapper for User related conversions.
 */
internal class UserMapper {
    /**
     * Note: IDE may show "Unresolved reference TLRPC". This is a false positive 
     * due to TLRPC.java size. Gradle compiler handles this correctly.
     */
    fun mapUser(user: TLRPC.User?): VibeUser? {
        return TelegramCoreAdapter.mapUser(user)
    }
}
