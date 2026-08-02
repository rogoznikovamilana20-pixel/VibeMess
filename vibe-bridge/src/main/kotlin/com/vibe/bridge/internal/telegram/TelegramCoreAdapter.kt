package com.vibe.bridge.internal.telegram

import android.os.Looper
import com.vibe.bridge.internal.media.IMediaRegistry
import com.vibe.bridge.model.VibeAvatar
import com.vibe.bridge.model.VibeReaction
import com.vibe.bridge.model.VibeReadState
import com.vibe.bridge.model.VibeTypingStatus
import com.vibe.bridge.model.VibeTypingType
import com.vibe.bridge.model.VibeUser
import org.telegram.messenger.AccountInstance
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.DialogObject
import org.telegram.messenger.FileLoader
import org.telegram.messenger.ImageLocation
import org.telegram.messenger.MediaDataController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.SendMessagesHelper
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ChatActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal object TelegramCoreAdapter {

    private var mediaRegistry: IMediaRegistry? = null

    fun setMediaRegistry(registry: IMediaRegistry?) {
        mediaRegistry = registry
    }

    // ── User helpers ────────────────────────────────────────────────────────

    fun getUser(userId: Long, account: Int): VibeUser? {
        var user: VibeUser? = null
        runOnUiThreadAndWait {
            user = MessagesController.getInstance(account).getUser(userId)?.let { mapUser(it) }
        }
        return user
    }

    fun getUsers(userIds: List<Long>, account: Int): List<VibeUser> {
        val result = ArrayList<VibeUser>(userIds.size)
        runOnUiThreadAndWait {
            val controller = MessagesController.getInstance(account)
            for (userId in userIds) {
                controller.getUser(userId)?.let { raw -> mapUser(raw)?.let { result.add(it) } }
            }
        }
        return result
    }

    fun mapUser(user: TLRPC.User?): VibeUser? {
        if (user == null) return null
        return VibeUser(
            id = user.id,
            username = user.username,
            firstName = user.first_name ?: "",
            lastName = user.last_name,
            phone = user.phone,
            isBot = user.bot,
            isPremium = user.premium,
            avatar = mapAvatar(user.photo)
        )
    }

    fun mapContacts(contacts: ArrayList<TLRPC.TL_contact>?, controller: MessagesController): List<VibeUser> {
        if (contacts.isNullOrEmpty()) return emptyList()
        return contacts.mapNotNull { contact ->
            controller.getUser(contact.user_id)?.let { mapUser(it) }
        }
    }

    fun mapContacts(contacts: ArrayList<TLRPC.TL_contact>?, account: Int): List<VibeUser> {
        if (contacts.isNullOrEmpty()) return emptyList()
        val result = ArrayList<VibeUser>(contacts.size)
        runOnUiThreadAndWait {
            val controller = MessagesController.getInstance(account)
            for (contact in contacts) {
                controller.getUser(contact.user_id)?.let { raw -> mapUser(raw)?.let { result.add(it) } }
            }
        }
        return result
    }

    // ── MessageObject helpers ───────────────────────────────────────────────

    fun getPeerId(mo: MessageObject): Long = mo.dialogId

    fun getFromId(mo: MessageObject): Long {
        val fromId = mo.messageOwner.from_id ?: return 0L
        return DialogObject.getPeerDialogId(fromId)
    }

    fun hasMedia(mo: MessageObject): Boolean = mo.messageOwner.media != null

    fun getMessageLocalId(mo: MessageObject): Int = mo.messageOwner.id

    fun getMessageSendState(mo: MessageObject): Int = mo.messageOwner.send_state

    fun getMessageDate(mo: MessageObject): Long = mo.messageOwner.date.toLong() * 1000L

    fun isPinned(mo: MessageObject): Boolean = mo.messageOwner.pinned

    fun isServiceMessage(mo: MessageObject): Boolean = mo.messageOwner.action != null

    fun isContactMessage(mo: MessageObject): Boolean {
        return mo.messageOwner.media is TLRPC.TL_messageMediaContact
    }

    fun isLocationMessage(mo: MessageObject): Boolean {
        return mo.messageOwner.media is TLRPC.TL_messageMediaGeo ||
               mo.messageOwner.media is TLRPC.TL_messageMediaGeoLive ||
               mo.messageOwner.media is TLRPC.TL_messageMediaVenue
    }

    fun isGif(mo: MessageObject): Boolean = mo.isGif

    fun hasDocument(mo: MessageObject): Boolean = mo.isDocument

    fun getMessageAttachPath(mo: MessageObject): String? = mo.messageOwner.attachPath

    fun getMediaId(mo: MessageObject): Int {
        val media = mo.messageOwner.media ?: return 0
        val document = getDocumentFromMedia(media) ?: return 0
        return document.id.toInt()
    }

    fun getMediaSize(mo: MessageObject): Long? {
        val media = mo.messageOwner.media ?: return null
        val document = getDocumentFromMedia(media) ?: return null
        return document.size
    }

    fun getMediaWidth(mo: MessageObject): Int {
        val media = mo.messageOwner.media ?: return 0
        if (media is TLRPC.TL_messageMediaPhoto && media.photo != null) {
            val size = media.photo.sizes?.lastOrNull() ?: return 0
            return size.w
        }
        val document = getDocumentFromMedia(media) ?: return 0
        for (attr in document.attributes) {
            if (attr is TLRPC.TL_documentAttributeImageSize) return attr.w
            if (attr is TLRPC.TL_documentAttributeVideo) return attr.w
        }
        return 0
    }

    fun getMediaHeight(mo: MessageObject): Int {
        val media = mo.messageOwner.media ?: return 0
        if (media is TLRPC.TL_messageMediaPhoto && media.photo != null) {
            val size = media.photo.sizes?.lastOrNull() ?: return 0
            return size.h
        }
        val document = getDocumentFromMedia(media) ?: return 0
        for (attr in document.attributes) {
            if (attr is TLRPC.TL_documentAttributeImageSize) return attr.h
            if (attr is TLRPC.TL_documentAttributeVideo) return attr.h
        }
        return 0
    }

    fun getMediaDuration(mo: MessageObject): Int {
        val media = mo.messageOwner.media ?: return 0
        val document = getDocumentFromMedia(media) ?: return 0
        for (attr in document.attributes) {
            if (attr is TLRPC.TL_documentAttributeAudio) return attr.duration.toInt()
            if (attr is TLRPC.TL_documentAttributeVideo) return attr.duration.toInt()
        }
        return 0
    }

    fun getVibeReactions(mo: MessageObject): List<VibeReaction> {
        val msgReactions = mo.messageOwner.reactions ?: return emptyList()
        val results = msgReactions.results ?: return emptyList()
        return results.mapNotNull { rc ->
            val emoji = when (val reaction = rc.reaction) {
                is TLRPC.TL_reactionEmoji -> reaction.emoticon
                else -> null
            } ?: return@mapNotNull null
            VibeReaction(
                emoji = emoji,
                count = rc.count,
                isChosen = rc.chosen_order >= 0 || rc.chosen
            )
        }
    }

    // ── Chat helpers ────────────────────────────────────────────────────────

    fun isDialogMuted(chatId: Long, currentAccount: Int): Boolean {
        var muted = false
        runOnUiThreadAndWait {
            muted = MessagesController.getInstance(currentAccount).isDialogMuted(chatId, 0L)
        }
        return muted
    }

    fun getDraftText(chatId: Long, currentAccount: Int): String? {
        var text: String? = null
        runOnUiThreadAndWait {
            val draft = MediaDataController.getInstance(currentAccount).getDraft(chatId, 0L)
            val draftText = draft?.message
            text = if (draftText.isNullOrEmpty()) null else draftText
        }
        return text
    }

    fun getDialogLastMessageDate(dialog: TLRPC.Dialog): Long {
        return DialogObject.getLastMessageOrDraftDate(dialog, null) * 1000L
    }

    // ── Typing / Read state ─────────────────────────────────────────────────

    fun getTypingUsers(chatId: Long, accountIndex: Int): List<VibeTypingStatus> {
        val result = ArrayList<VibeTypingStatus>()
        runOnUiThreadAndWait {
            val types = MessagesController.getInstance(accountIndex)
                .printingUsers[chatId]?.get(0) ?: return@runOnUiThreadAndWait
            for (pu in types) {
                if (pu.userId > 0) {
                    result.add(VibeTypingStatus(userId = pu.userId, type = mapActionToTypingType(pu.action)))
                }
            }
        }
        return result
    }

    fun getReadState(chatId: Long, accountIndex: Int): VibeReadState {
        var state = VibeReadState(chatId = chatId, inboxReadMessageId = 0, outboxReadMessageId = 0, maxReadMessageId = 0, unreadCount = 0)
        runOnUiThreadAndWait {
            val dialog = MessagesController.getInstance(accountIndex).dialogs_dict.get(chatId)
            state = VibeReadState(
                chatId = chatId,
                inboxReadMessageId = dialog?.read_inbox_max_id ?: 0,
                outboxReadMessageId = dialog?.read_outbox_max_id ?: 0,
                maxReadMessageId = maxOf(dialog?.read_inbox_max_id ?: 0, dialog?.read_outbox_max_id ?: 0),
                unreadCount = dialog?.unread_count ?: 0
            )
        }
        return state
    }

    // ── Message lookup ──────────────────────────────────────────────────────

    fun getMessageById(msgId: Int, account: Int): MessageObject? {
        var msg: MessageObject? = null
        runOnUiThreadAndWait {
            msg = MessagesController.getInstance(account).dialogMessagesByIds.get(msgId)
        }
        return msg
    }

    // ── Media ───────────────────────────────────────────────────────────────

    fun getAttachFileName(telegramObject: Any): String? {
        return when (telegramObject) {
            is TLRPC.Document -> MessageObject.getFileName(telegramObject)
            is TLRPC.TL_photo -> {
                val size = telegramObject.sizes?.firstOrNull()
                if (size != null) "photo_${telegramObject.id}.jpg" else null
            }
            else -> null
        }
    }

    fun startDownload(account: Int, telegramObject: Any, priority: Int) {
        val fileLoader = AccountInstance.getInstance(account).fileLoader
        when (telegramObject) {
            is TLRPC.Document -> fileLoader.loadFile(telegramObject, telegramObject, priority, 0)
            is TLRPC.TL_photo -> {
                val size = telegramObject.sizes?.lastOrNull()
                if (size != null) {
                    val location = ImageLocation.getForPhoto(size, telegramObject)
                    fileLoader.loadFile(location, telegramObject, null, priority, 0)
                }
            }
        }
    }

    fun cancelDownload(account: Int, telegramObject: Any) {
        val fileLoader = AccountInstance.getInstance(account).fileLoader
        when (telegramObject) {
            is TLRPC.Document -> fileLoader.cancelLoadFile(telegramObject)
            is TLRPC.TL_photo -> {
                val size = telegramObject.sizes?.lastOrNull()
                if (size != null) {
                    fileLoader.cancelLoadFile(size.location, "jpg")
                }
            }
        }
    }

    // ── Message send / edit / delete ────────────────────────────────────────

    fun sendTextMessage(account: Int, chatId: Long, text: String, replyToMsgId: Long?): MessageObject? {
        var result: MessageObject? = null
        runOnUiThreadAndWait {
            val params = SendMessagesHelper.SendMessageParams.of(text, chatId)
            params.replyToMsg = resolveReplyTo(account, replyToMsgId)
            SendMessagesHelper.getInstance(account).sendMessage(params)
            val controller = MessagesController.getInstance(account)
            result = controller.dialogMessage.get(chatId)
                ?.firstOrNull { it.isOut && it.messageOwner.message == text }
        }
        return result
    }

    fun prepareAndSendPhoto(account: Int, chatId: Long, path: String, caption: String?, replyToMsgId: Long?) {
        val accountInstance = AccountInstance.getInstance(account)
        val replyToMsg = resolveReplyTo(account, replyToMsgId)
        runOnUiThreadAndWait {
            SendMessagesHelper.prepareSendingPhoto(
                accountInstance, path, null as android.net.Uri?, chatId,
                replyToMsg, null, null, caption, null, null, null, 0,
                null, true, 0, 0, null, 0
            )
        }
    }

    fun prepareAndSendVideo(account: Int, chatId: Long, path: String, caption: String?, replyToMsgId: Long?) {
        val accountInstance = AccountInstance.getInstance(account)
        val replyToMsg = resolveReplyTo(account, replyToMsgId)
        runOnUiThreadAndWait {
            SendMessagesHelper.prepareSendingVideo(
                accountInstance, path, null, null, null, chatId,
                replyToMsg, null, null, null, null, 0,
                null, true, 0, 0, false, false, caption,
                null, 0, 0L, 0L
            )
        }
    }

    fun prepareAndSendDocument(account: Int, chatId: Long, path: String, caption: String?, replyToMsgId: Long?) {
        val accountInstance = AccountInstance.getInstance(account)
        val replyToMsg = resolveReplyTo(account, replyToMsgId)
        runOnUiThreadAndWait {
            SendMessagesHelper.prepareSendingDocument(
                accountInstance, path, null, null, caption ?: "", null,
                chatId, replyToMsg, null, null, null,
                null, true, 0, null, null, 0, false
            )
        }
    }

    fun forwardMessages(account: Int, _fromChatId: Long, messageIds: List<Long>, toChatId: Long): List<MessageObject>? {
        var msgs: List<MessageObject>? = null
        runOnUiThreadAndWait {
            val controller = MessagesController.getInstance(account)
            val found = messageIds.mapNotNull { id ->
                controller.dialogMessagesByIds.get(id.toInt())
            }
            if (found.isEmpty()) return@runOnUiThreadAndWait
            val sendHelper = SendMessagesHelper.getInstance(account)
            sendHelper.sendMessage(
                ArrayList(found), toChatId, false, false, true,
                0, 0, null, 0, 0L, 0L, null
            )
            msgs = found
        }
        return msgs
    }

    fun forwardMessagesAsCopy(account: Int, _fromChatId: Long, messageIds: List<Long>, toChatId: Long): List<MessageObject>? {
        var msgs: List<MessageObject>? = null
        runOnUiThreadAndWait {
            val controller = MessagesController.getInstance(account)
            val found = messageIds.mapNotNull { id ->
                controller.dialogMessagesByIds.get(id.toInt())
            }
            if (found.isEmpty()) return@runOnUiThreadAndWait
            val sendHelper = SendMessagesHelper.getInstance(account)
            sendHelper.sendMessage(
                ArrayList(found), toChatId, false, true, true,
                0, 0, null, 0, 0L, 0L, null
            )
            msgs = found
        }
        return msgs
    }

    fun cancelSendingMessage(account: Int, messageId: Int): Boolean {
        var cancelled = false
        runOnUiThreadAndWait {
            val msgObj = MessagesController.getInstance(account).dialogMessagesByIds.get(messageId)
            if (msgObj != null) {
                SendMessagesHelper.getInstance(account).cancelSendingMessage(msgObj)
                cancelled = true
            }
        }
        return cancelled
    }

    fun editMessage(account: Int, chatId: Long, messageId: Long, newText: String): MessageObject? {
        var result: MessageObject? = null
        val latch = CountDownLatch(1)
        runOnUiThreadAndWait {
            val msgObj = MessagesController.getInstance(account).dialogMessagesByIds.get(messageId.toInt())
            result = msgObj
            if (msgObj == null) {
                latch.countDown()
                return@runOnUiThreadAndWait
            }
            val accountInstance = AccountInstance.getInstance(account)
            val req = TLRPC.TL_messages_editMessage()
            req.peer = MessagesController.getInstance(account).getInputPeer(chatId)
            req.message = newText
            req.flags = req.flags or 2048
            req.id = messageId.toInt()
            accountInstance.connectionsManager.sendRequest(req) { response, _ ->
                if (response is TLRPC.Updates) {
                    AndroidUtilities.runOnUIThread {
                        MessagesController.getInstance(account).processUpdates(response, false)
                    }
                }
                latch.countDown()
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        return result
    }

    fun deleteMessages(account: Int, chatId: Long, messageIds: List<Long>, revoke: Boolean): Boolean {
        if (messageIds.isEmpty()) return false
        val ids = ArrayList(messageIds.map { it.toInt() })
        runOnUiThreadAndWait {
            MessagesController.getInstance(account).deleteMessages(
                ids, null, null, chatId, 0, revoke, 0
            )
        }
        return true
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun resolveReplyTo(account: Int, replyToMsgId: Long?): MessageObject? {
        if (replyToMsgId == null) return null
        var reply: MessageObject? = null
        runOnUiThreadAndWait {
            reply = MessagesController.getInstance(account).dialogMessagesByIds.get(replyToMsgId.toInt())
        }
        return reply
    }

    private fun getDocumentFromMedia(media: TLRPC.MessageMedia): TLRPC.Document? {
        if (media is TLRPC.TL_messageMediaWebPage && media.webpage != null) {
            return media.webpage.document
        }
        if (media is TLRPC.TL_messageMediaGame && media.game != null) {
            return media.game.document
        }
        return media.document
    }

    private fun mapAvatar(photo: TLRPC.UserProfilePhoto?): VibeAvatar? {
        if (photo == null || photo.photo_id == 0L) return null
        return VibeAvatar(
            photoId = photo.photo_id,
            accessHash = 0L,
            smallPhoto = photo.photo_small?.let { "photo_${photo.photo_id}_small" },
            bigPhoto = photo.photo_big?.let { "photo_${photo.photo_id}_big" }
        )
    }

    private fun mapActionToTypingType(action: TLRPC.SendMessageAction?): VibeTypingType {
        return when (action) {
            is TLRPC.TL_sendMessageTypingAction -> VibeTypingType.TYPING
            is TLRPC.TL_sendMessageRecordVideoAction -> VibeTypingType.RECORD_VIDEO
            is TLRPC.TL_sendMessageRecordAudioAction -> VibeTypingType.RECORD_AUDIO
            is TLRPC.TL_sendMessageUploadVideoAction -> VibeTypingType.UPLOAD_VIDEO
            is TLRPC.TL_sendMessageUploadAudioAction -> VibeTypingType.UPLOAD_AUDIO
            is TLRPC.TL_sendMessageUploadPhotoAction -> VibeTypingType.UPLOAD_PHOTO
            is TLRPC.TL_sendMessageUploadDocumentAction -> VibeTypingType.UPLOAD_DOCUMENT
            is TLRPC.TL_sendMessageChooseStickerAction -> VibeTypingType.CHOOSE_STICKER
            else -> VibeTypingType.TYPING
        }
    }

    private fun runOnUiThreadAndWait(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            val latch = CountDownLatch(1)
            AndroidUtilities.runOnUIThread {
                try { action() } finally { latch.countDown() }
            }
            latch.await(10, TimeUnit.SECONDS)
        }
    }
}
