package com.vibe.bridge.mapper

import com.vibe.bridge.internal.telegram.TelegramCoreAdapter
import com.vibe.bridge.model.VibeChat
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ChatObject
import org.telegram.messenger.FileLoader
import org.telegram.messenger.ImageReceiver
import org.telegram.messenger.MessagesController
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import java.io.File

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
            "$fn $ln".trim().ifEmpty { user.phone ?: "Без имени" }
        } else if (chat != null) {
            chat.title ?: "Без имени"
        } else {
            "Без имени"
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

        val avatarPath = when {
            user != null -> resolveAvatar(user, currentAccount)
            chat != null -> resolveAvatar(chat, currentAccount)
            else -> null
        }

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
            lastActivityDate = TelegramCoreAdapter.getDialogLastMessageDate(dialog),
            avatarPath = avatarPath
        )
    }

    private fun resolveAvatar(tlObject: TLObject, currentAccount: Int): String? {
        val small: TLRPC.FileLocation? = when (tlObject) {
            is TLRPC.User -> tlObject.photo?.photo_small
            is TLRPC.Chat -> tlObject.photo?.photo_small
            else -> null
        }
        if (small == null) return null
        val file = FileLoader.getInstance(currentAccount).getPathToAttach(small, true) ?: return null
        if (file.exists()) return file.absolutePath
        // Avatar not cached yet — ask Telegram core to download it; the UI
        // falls back to initials until the file appears on the next sync emit.
        AndroidUtilities.runOnUIThread {
            val receiver = ImageReceiver()
            receiver.setForUserOrChat(tlObject, null, tlObject)
        }
        return null
    }
}
