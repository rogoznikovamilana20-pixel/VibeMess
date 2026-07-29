package com.vibe.bridge.model

/**
 * Reference to a user's avatar in Telegram.
 * Note: Actual image data is not stored here; only references for downloading/viewing.
 */
data class VibeAvatar(
    val photoId: Long,
    val accessHash: Long,
    val smallPhoto: String?, // Reference/Path to small thumbnail
    val bigPhoto: String?     // Reference/Path to full-size photo
)

/**
 * Vibe-specific representation of a user.
 */
data class VibeUser(
    val id: Long,
    val username: String?,
    val firstName: String,
    val lastName: String?,
    val phone: String?,
    val isBot: Boolean,
    val isPremium: Boolean,
    val avatar: VibeAvatar?
)

/**
 * Vibe-specific representation of a chat.
 */
data class VibeChat(
    val id: Long,
    val title: String,
    val type: ChatType,
    val lastMessage: VibeMessagePreview?,
    val unreadCount: Int,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val draftText: String?,
    val lastActivityDate: Long
) {
    enum class ChatType {
        PRIVATE, GROUP, CHANNEL, SUPERGROUP
    }
}

/**
 * Lightweight preview of a message for chat list.
 */
data class VibeMessagePreview(
    val id: Long,
    val text: String,
    val date: Long,
    val isOutgoing: Boolean
)

/**
 * High-level classification of a Telegram message.
 * Mapped internally from [org.telegram.tgnet.TLRPC.Message] action/media.
 */
enum class VibeMessageType {
    TEXT,
    PHOTO,
    VIDEO,
    DOCUMENT,
    VOICE,
    AUDIO,
    STICKER,
    CONTACT,
    LOCATION,
    POLL,
    SERVICE,
    UNSUPPORTED,
    UNKNOWN
}

/**
 * Coarse classification of an attachment payload.
 */
enum class VibeAttachmentType {
    PHOTO,
    VIDEO,
    DOCUMENT,
    VOICE,
    AUDIO,
    STICKER,
    CONTACT,
    LOCATION,
    POLL
}

/**
 * Rich, read-only description of a message attachment.
 *
 * All fields are derived inside the bridge from Telegram core structures.
 * No Telegram mutable state is exposed.
 */
data class VibeMessageAttachment(
    val type: VibeAttachmentType,
    val fileId: String,
    val fileName: String?,
    val size: Long?,
    val mimeType: String?,
    val width: Int?,
    val height: Int?,
    val duration: Int?,
    val localPath: String? = null
)

/**
 * Represents the current status of a media download.
 */
data class DownloadStatus(
    val fileId: String,
    val progress: Float,
    val completed: Boolean,
    val localPath: String?,
    val error: String? = null
)

/**
 * High-level types of user activity in a chat.
 */
enum class VibeTypingType {
    TYPING,
    RECORD_AUDIO,
    RECORD_VIDEO,
    UPLOAD_VIDEO,
    UPLOAD_AUDIO,
    UPLOAD_PHOTO,
    UPLOAD_DOCUMENT,
    CHOOSE_STICKER,
    UNKNOWN
}

/**
 * Current typing status of a specific user.
 */
data class VibeTypingStatus(
    val userId: Long,
    val type: VibeTypingType
)

/**
 * Represents the state of message reading in a chat.
 */
data class VibeReadState(
    val chatId: Long,
    val inboxReadMessageId: Int,
    val outboxReadMessageId: Int,
    val maxReadMessageId: Int,
    val unreadCount: Int
)

/**
 * Vibe-specific representation of a message reaction.
 */
data class VibeReaction(
    val emoji: String,
    val count: Int,
    val isChosen: Boolean
)

/**
 * Status of an outgoing message.
 */
enum class VibeDeliveryStatus {
    PENDING,
    SENT,
    ERROR,
    CANCELLED
}

/**
 * Vibe-specific read-only representation of a message.
 */
data class VibeMessage(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val text: String,
    val date: Long,
    val type: VibeMessageType,
    val isOutgoing: Boolean,
    val pinned: Boolean,
    val reactions: List<VibeReaction>,
    val replyId: Long?,
    val attachment: VibeMessageAttachment?,
    val localId: Long? = null,
    val deliveryStatus: VibeDeliveryStatus? = null
)

/**
 * Represents a batch of message deletions.
 */
data class MessageDeletion(
    val accountIndex: Int,
    val chatId: Long?,
    val messageIds: List<Long>
)

/**
 * Paging cursor for history navigation.
 *
 * The cursor is fully owned by the caller. It is produced by [VibeMessagePage.nextCursor]
 * and fed back into [com.vibe.bridge.api.IMessageService.getMessageHistory].
 *
 * @property chatId        dialog the cursor belongs to.
 * @property maxId         load messages strictly older than this id (exclusive upper bound).
 *                         A value of `0` means "start from the newest message".
 * @property offsetDate    optional unix-seconds anchor used together with [maxId].
 * @property isEnd         hint copied from the page that produced this cursor.
 */
data class VibeHistoryCursor(
    val chatId: Long,
    val maxId: Long,
    val offsetDate: Long,
    val isEnd: Boolean
)

/**
 * A single batch (page) of message history.
 *
 * @property chatId     dialog this page belongs to.
 * @property messages   messages of this page, ordered newest-first (matching Telegram order).
 * @property nextCursor cursor to load the next (older) page, or `null` when the beginning is reached.
 * @property isEnd      `true` when no more history is available.
 * @property fromCache  `true` when the page was served from the local database (no network).
 */
data class VibeMessagePage(
    val chatId: Long,
    val messages: List<VibeMessage>,
    val nextCursor: VibeHistoryCursor?,
    val isEnd: Boolean,
    val fromCache: Boolean
)

/**
 * Vibe-specific representation of an account.
 */
data class VibeAccount(
    val index: Int,
    val userId: Long,
    val phoneNumber: String?
)
