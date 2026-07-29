package com.vibe.bridge.mapper

import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import com.vibe.bridge.model.VibeChat
import org.telegram.messenger.ChatObject
import org.telegram.messenger.MessagesController
import org.telegram.tgnet.TLRPC

/**
 * Mapper for Chat related conversions.
 */
internal class ChatMapper(
    private val messageMapper: MessageMapper
) {
    /**
     * Note: IDE may show "Unresolved reference TLRPC". This is a false positive 
     * due to TLRPC.java size. Gradle compiler handles this correctly.
     */
    fun mapChat(dialog: TLRPC.Dialog, currentAccount: Int): VibeChat {
        val messagesController = MessagesController.getInstance(currentAccount)
        val chatId = dialog.id
        
        val user = if (chatId > 0) messagesController.getUser(chatId) else null
        val chat = if (chatId < 0) messagesController.getChat(-chatId) else null

        val title = if (user != null) {
            val fn = user.first_name ?: ""
            val ln = user.last_name ?: ""
            "$fn $ln".trim()
        } else if (chat != null) {
            chat.title ?: "Unknown"
        } else {
            "Unknown"
        }

        val type = when {
            ChatObject.isChannel(-chatId, currentAccount) -> VibeChat.ChatType.CHANNEL
            chatId < 0 -> VibeChat.ChatType.GROUP
            else -> VibeChat.ChatType.PRIVATE
        }

        val messages = messagesController.dialogMessage.get(chatId)
        val lastMessage = if (messages != null && messages.isNotEmpty()) {
            val mo = messages[0]
            if (mo != null) {
                messageMapper.mapMessagePreview(mo)
            } else null
        } else null

        return VibeChat(
            id = chatId,
            title = title,
            type = type,
            lastMessage = lastMessage,
            unreadCount = dialog.unread_count,
            isMuted = TelegramCoreAdapter.isDialogMuted(chatId, currentAccount),
            isPinned = dialog.pinned,
            isArchived = dialog.folder_id != 0,
            draftText = TelegramCoreAdapter.getDraftText(chatId, currentAccount),
            lastActivityDate = TelegramCoreAdapter.getDialogLastMessageDate(dialog)
        )
    }
}
