package com.vibe.bridge.mapper

import com.vibe.bridge.model.*
import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import org.telegram.messenger.MessageObject

/**
 * Mapper for Message related conversions.
 *
 * All mapping is pure and allocation-light: it only reads immutable fields of
 * [MessageObject] / [TLRPC.Message] and never mutates Telegram core state.
 */
internal class MessageMapper {

    fun mapMessage(mo: MessageObject): VibeMessage {
        val chatId = TelegramCoreAdapter.getPeerId(mo)
        val senderId = TelegramCoreAdapter.getFromId(mo)
        val type = mapMessageType(mo)
        val attachment = if (TelegramCoreAdapter.hasMedia(mo)) mapAttachment(mo) else null
        val replyId = mo.replyMsgId.toLong().takeIf { it != 0L }

        val isOutgoing = mo.isOut
        var deliveryStatus: VibeDeliveryStatus? = null
        var localId: Long? = null

        if (isOutgoing) {
            localId = TelegramCoreAdapter.getMessageLocalId(mo).toLong().takeIf { it != 0L }
            deliveryStatus = when (TelegramCoreAdapter.getMessageSendState(mo)) {
                1 -> VibeDeliveryStatus.PENDING // MESSAGE_SEND_STATE_SENDING
                2 -> VibeDeliveryStatus.ERROR   // MESSAGE_SEND_STATE_SEND_ERROR
                0 -> VibeDeliveryStatus.SENT    // MESSAGE_SEND_STATE_SENT
                else -> VibeDeliveryStatus.SENT
            }
        }

        return VibeMessage(
            id = mo.id.toLong(),
            chatId = chatId,
            senderId = senderId,
            text = mo.messageText?.toString() ?: "",
            date = TelegramCoreAdapter.getMessageDate(mo),
            type = type,
            isOutgoing = isOutgoing,
            pinned = TelegramCoreAdapter.isPinned(mo),
            reactions = TelegramCoreAdapter.getVibeReactions(mo),
            replyId = replyId,
            attachment = attachment,
            localId = localId,
            deliveryStatus = deliveryStatus
        )
    }

    fun mapMessagePreview(mo: MessageObject): VibeMessagePreview {
        return VibeMessagePreview(
            id = mo.id.toLong(),
            text = mo.messageText?.toString() ?: "",
            date = TelegramCoreAdapter.getMessageDate(mo),
            isOutgoing = mo.isOut
        )
    }

    private fun mapMessageType(mo: MessageObject): VibeMessageType {
        if (TelegramCoreAdapter.isServiceMessage(mo)) return VibeMessageType.SERVICE

        return when {
            mo.isPhoto -> VibeMessageType.PHOTO
            mo.isVoice -> VibeMessageType.VOICE
            mo.isMusic -> VibeMessageType.AUDIO
            mo.isVideo -> VibeMessageType.VIDEO
            mo.isSticker -> VibeMessageType.STICKER
            TelegramCoreAdapter.isContactMessage(mo) -> VibeMessageType.CONTACT
            TelegramCoreAdapter.isLocationMessage(mo) -> VibeMessageType.LOCATION
            mo.isPoll -> VibeMessageType.POLL
            else -> VibeMessageType.TEXT
        }
    }

    private fun mapAttachment(mo: MessageObject): VibeMessageAttachment? {
        val localPath = TelegramCoreAdapter.getMessageAttachPath(mo)
        return when {
            mo.isPhoto -> {
                VibeMessageAttachment(
                    type = VibeAttachmentType.PHOTO,
                    fileId = TelegramCoreAdapter.getMediaId(mo).toString(),
                    fileName = null,
                    size = TelegramCoreAdapter.getMediaSize(mo),
                    mimeType = "image/jpeg",
                    width = TelegramCoreAdapter.getMediaWidth(mo),
                    height = TelegramCoreAdapter.getMediaHeight(mo),
                    duration = 0,
                    localPath = localPath
                )
            }
            mo.isVoice || mo.isMusic || mo.isVideo || mo.isSticker || TelegramCoreAdapter.isGif(mo) || TelegramCoreAdapter.hasDocument(mo) -> {
                val type = when {
                    mo.isVoice -> VibeAttachmentType.VOICE
                    mo.isMusic -> VibeAttachmentType.AUDIO
                    mo.isVideo -> VibeAttachmentType.VIDEO
                    mo.isSticker -> VibeAttachmentType.STICKER
                    TelegramCoreAdapter.isGif(mo) -> VibeAttachmentType.VIDEO // Or GIF if we had it
                    else -> VibeAttachmentType.DOCUMENT
                }
                VibeMessageAttachment(
                    type = type,
                    fileId = TelegramCoreAdapter.getMediaId(mo).toString(),
                    fileName = mo.fileName,
                    size = TelegramCoreAdapter.getMediaSize(mo),
                    mimeType = mo.mimeType,
                    width = TelegramCoreAdapter.getMediaWidth(mo).takeIf { it != 0 },
                    height = TelegramCoreAdapter.getMediaHeight(mo).takeIf { it != 0 },
                    duration = TelegramCoreAdapter.getMediaDuration(mo),
                    localPath = localPath
                )
            }
            TelegramCoreAdapter.isContactMessage(mo) ->
                VibeMessageAttachment(VibeAttachmentType.CONTACT, "", null, null, "contact", null, null, 0, localPath)
            TelegramCoreAdapter.isLocationMessage(mo) ->
                VibeMessageAttachment(VibeAttachmentType.LOCATION, "", null, null, "geo", null, null, 0, localPath)
            mo.isPoll ->
                VibeMessageAttachment(VibeAttachmentType.POLL, "", null, null, "poll", null, null, 0, localPath)
            else -> null
        }
    }
}
