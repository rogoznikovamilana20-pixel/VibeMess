package com.vibe.bridge.mapper

import com.vibe.bridge.model.VibeAccount
import org.telegram.messenger.UserConfig

/**
 * Mapper for Account related conversions.
 */
internal class AccountMapper {
    fun mapAccount(index: Int, config: UserConfig): VibeAccount {
        return VibeAccount(
            index = index,
            userId = config.clientUserId,
            phoneNumber = config.clientPhone
        )
    }
}
