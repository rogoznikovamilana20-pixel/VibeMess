package com.vibe.bridge.internal.account

import com.vibe.bridge.api.IAccountService
import com.vibe.bridge.mapper.TelegramMapper
import com.vibe.bridge.model.VibeAccount
import org.telegram.messenger.UserConfig

/**
 * Implementation of [IAccountService] using Telegram internal classes.
 */
internal class TelegramAccountService(
    private val mapper: TelegramMapper
) : IAccountService {

    override fun getCurrentAccount(): VibeAccount {
        val index = UserConfig.selectedAccount
        val config = UserConfig.getInstance(index)
        return mapper.mapAccount(index, config)
    }

    override fun getActiveAccounts(): List<VibeAccount> {
        val accounts = mutableListOf<VibeAccount>()
        val count = UserConfig.getActivatedAccountsCount()
        for (i in 0 until count) {
            val config = UserConfig.getInstance(i)
            accounts.add(mapper.mapAccount(i, config))
        }
        return accounts
    }
}
