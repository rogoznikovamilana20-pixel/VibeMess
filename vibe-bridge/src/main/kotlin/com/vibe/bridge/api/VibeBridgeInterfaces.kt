package com.vibe.bridge.api

import com.vibe.bridge.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Interface for message-related operations.
 *
 * This layer is strictly READ-ONLY. Implementations must never send, edit,
 * delete, or otherwise mutate Telegram data. It is designed so that a future
 * AI component (Aurion) can consume the message stream directly through this
 * interface without touching Telegram core.
 */
interface IMessageService {

    companion object {
        /** Default number of messages requested per history page. */
        const val DEFAULT_HISTORY_PAGE_SIZE = 50

        /** Hard upper bound to protect memory on very large chats. */
        const val MAX_HISTORY_PAGE_SIZE = 200
    }

    /**
     * Loads a single page of message history for [chatId].
     *
     * Pagination is cursor-based: pass `null` (or a cursor with [VibeHistoryCursor.maxId] == 0)
     * to start from the newest message, then feed [VibeMessagePage.nextCursor] back to load
     * progressively older messages. Each call emits exactly one [VibeMessagePage] and completes.
     *
     * All work happens on Telegram's storage/stage threads; mapping is offloaded to [kotlinx.coroutines.Dispatchers.IO].
     *
     * @param chatId dialog id (positive for users, negative for chats/channels).
     * @param cursor pagination cursor from a previous page, or `null` for the newest page.
     * @param limit  maximum number of messages in the page.
     */
    fun getMessageHistory(
        chatId: Long,
        cursor: VibeHistoryCursor? = null,
        limit: Int = DEFAULT_HISTORY_PAGE_SIZE
    ): Flow<VibeMessagePage>

    /**
     * Convenience wrapper that emits only the messages of the newest page.
     */
    fun getRecentMessages(
        chatId: Long,
        limit: Int = DEFAULT_HISTORY_PAGE_SIZE
    ): Flow<List<VibeMessage>>

    /**
     * Sends a plain text message to the specified chat.
     * The local version of the message will be emitted via observeNewMessages() almost immediately.
     * Server confirmation (ID replacement) will arrive via observeMessageEdits().
     */
    suspend fun sendTextMessage(
        chatId: Long,
        text: String,
        replyToMsgId: Long? = null
    ): Result<VibeMessage>

    /**
     * Forwards existing messages to the specified chat.
     * Original authorship is preserved.
     */
    suspend fun forwardMessages(
        fromChatId: Long,
        messageIds: List<Long>,
        toChatId: Long
    ): Result<List<VibeMessage>>

    /**
     * Forwards existing messages to the specified chat as copies.
     * Original authorship is not preserved.
     */
    suspend fun forwardMessagesAsCopy(
        fromChatId: Long,
        messageIds: List<Long>,
        toChatId: Long
    ): Result<List<VibeMessage>>

    suspend fun sendPhoto(
        chatId: Long,
        path: String,
        caption: String? = null,
        replyToMsgId: Long? = null
    ): Result<Unit>

    suspend fun sendVideo(
        chatId: Long,
        path: String,
        caption: String? = null,
        replyToMsgId: Long? = null
    ): Result<Unit>

    suspend fun sendDocument(
        chatId: Long,
        path: String,
        caption: String? = null,
        replyToMsgId: Long? = null
    ): Result<Unit>

    /**
     * Edits the text of an existing message.
     * Only own messages or messages in channels where the user is admin can be edited.
     * Result.success means the operation was initiated.
     * The actual update will arrive via observeMessageEdits().
     */
    suspend fun editMessage(
        chatId: Long,
        messageId: Long,
        newText: String
    ): Result<VibeMessage>

    /**
     * Deletes one or more messages.
     * @param revoke if true, deletes for everyone (if permitted).
     * Result.success means the operation was initiated.
     * The actual deletion will arrive via observeMessageDeletions().
     */
    suspend fun deleteMessages(
        chatId: Long,
        messageIds: List<Long>,
        revoke: Boolean
    ): Result<Unit>

    /**
     * Observes the upload progress of a media message.
     * Emits values from 0.0 to 1.0.
     */
    fun observeUploadProgress(chatId: Long, messageId: Long): Flow<Float>

    /**
     * Cancels an outgoing message that is currently being sent.
     */
    suspend fun cancelSending(chatId: Long, messageId: Long): Result<Unit>
}

/**
 * Interface for user-related operations.
 */
interface IUserService {
    suspend fun getUser(userId: Long): VibeUser?
    suspend fun getUsers(userIds: List<Long>): List<VibeUser>
    fun observeUserUpdates(): Flow<VibeUser>
}

/**
 * Interface for contact-related operations.
 */
interface IContactService {
    suspend fun getContacts(): List<VibeUser>
    fun observeContacts(): Flow<List<VibeUser>>
}

/**
 * Interface for account-related operations.
 */
interface IAccountService {
    fun getCurrentAccount(): VibeAccount
    fun getActiveAccounts(): List<VibeAccount>
}

/**
 * Interface for chat-related operations.
 */
interface IChatService {
    suspend fun getChat(chatId: Long): VibeChat?
    fun getActiveChats(): Flow<List<VibeChat>>
    fun observeTyping(chatId: Long): Flow<List<VibeTypingStatus>>
    fun observeReadState(chatId: Long): Flow<VibeReadState>
}

/**
 * Interface for media-related operations.
 */
interface IMediaService {
    /**
     * Starts downloading the media attachment.
     * Use [observeDownload] to track progress and completion.
     */
    fun downloadMedia(attachment: VibeMessageAttachment)

    /**
     * Cancels an ongoing download.
     */
    fun cancelDownload(attachment: VibeMessageAttachment)

    /**
     * Returns a Flow to observe the download status of a specific attachment.
     */
    fun observeDownload(attachment: VibeMessageAttachment): Flow<DownloadStatus>
}

/**
 * Interface for receiving real-time notifications from the core.
 */
interface INotificationService {
    fun observeNewMessages(): Flow<List<VibeMessage>>
    fun observeMessageEdits(): Flow<VibeMessage>
    fun observeMessageDeletions(): Flow<MessageDeletion>
    fun observeHistoryCleared(): Flow<Long>
    fun observeChatUpdates(): Flow<VibeChat>
}

/**
 * Main entry point for the Vibe Bridge.
 */
interface ITelegramGateway {
    val messages: IMessageService
    val users: IUserService
    val contacts: IContactService
    val accounts: IAccountService
    val chats: IChatService
    val media: IMediaService
    val notifications: INotificationService
}
