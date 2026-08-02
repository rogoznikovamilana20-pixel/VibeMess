package com.vibe.bridge.gateway

import com.vibe.bridge.api.*
import com.vibe.bridge.internal.account.TelegramAccountService
import com.vibe.bridge.internal.chat.TelegramChatService
import com.vibe.bridge.internal.contact.TelegramContactService
import com.vibe.bridge.internal.message.TelegramMessageService
import com.vibe.bridge.internal.notification.TelegramNotificationService
import com.vibe.bridge.internal.search.TelegramSearchService
import com.vibe.bridge.internal.user.TelegramUserService

/**
 * Implementation of [ITelegramGateway] for the Vibe Bridge.
 */
class TelegramGateway internal constructor(
    private val notificationService: TelegramNotificationService,
    private val accountService: TelegramAccountService,
    private val chatService: TelegramChatService,
    private val messageService: IMessageService,
    private val userService: TelegramUserService,
    private val contactService: TelegramContactService,
    private val mediaService: IMediaService,
    private val searchService: TelegramSearchService
) : ITelegramGateway {
    override val messages: IMessageService get() = messageService
    override val users: IUserService get() = userService
    override val contacts: IContactService get() = contactService
    override val accounts: IAccountService get() = accountService
    override val chats: IChatService get() = chatService
    override val media: IMediaService get() = mediaService
    override val notifications: INotificationService get() = notificationService
    override val search: ISearchService get() = searchService
}
