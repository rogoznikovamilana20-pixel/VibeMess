package com.vibe.bridge.mapper

import com.vibe.bridge.model.*
import org.telegram.messenger.MessageObject
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC

/**
 * Facade Mapper for backward compatibility and aggregation.
 */
internal class TelegramMapper {

    private val userMapper = UserMapper()
    private val accountMapper = AccountMapper()
    private val messageMapper = MessageMapper()

    /**
     * Note: IDE may show "Unresolved reference TLRPC". This is a false positive 
     * due to TLRPC.java size. Gradle compiler handles this correctly.
     */
    fun mapUser(user: TLRPC.User): VibeUser = userMapper.mapUser(user)!!
    fun mapAccount(index: Int, config: UserConfig): VibeAccount = accountMapper.mapAccount(index, config)
    fun mapMessage(mo: MessageObject): VibeMessage = messageMapper.mapMessage(mo)
    fun mapMessagePreview(mo: MessageObject): VibeMessagePreview = messageMapper.mapMessagePreview(mo)
}
