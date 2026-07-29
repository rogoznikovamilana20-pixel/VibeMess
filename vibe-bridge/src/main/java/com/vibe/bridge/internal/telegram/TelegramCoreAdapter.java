package com.vibe.bridge.internal.telegram;

import java.util.ArrayList;
import java.util.List;

import com.vibe.bridge.internal.media.IMediaRegistry;
import com.vibe.bridge.model.VibeAvatar;
import com.vibe.bridge.model.VibeReaction;
import com.vibe.bridge.model.VibeReadState;
import com.vibe.bridge.model.VibeTypingStatus;
import com.vibe.bridge.model.VibeTypingType;
import com.vibe.bridge.model.VibeUser;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import androidx.core.view.inputmethod.InputContentInfoCompat;

/**
 * Pure Java mediator to perform mapping of Telegram Core objects.
 * This prevents Kotlin's IDE analyzer from having to index massive Java files (like TLRPC.java).
 */
public class TelegramCoreAdapter {

    private static IMediaRegistry mediaRegistry;

    public static void setMediaRegistry(IMediaRegistry registry) {
        mediaRegistry = registry;
    }

    public static VibeUser mapUser(Object userObj) {
        if (!(userObj instanceof TLRPC.User)) return null;
        TLRPC.User user = (TLRPC.User) userObj;

        VibeAvatar avatar = null;
        if (user.photo != null) {
            avatar = new VibeAvatar(
                user.photo.photo_id,
                0L,
                null,
                null
            );
        }

        return new VibeUser(
            user.id,
            user.username,
            user.first_name != null ? user.first_name : "",
            user.last_name,
            user.phone,
            UserObject.isBot(user),
            user.premium,
            avatar
        );
    }

    public static List<VibeUser> mapContacts(List<?> contacts, int account) {
        MessagesController mc = MessagesController.getInstance(account);
        List<VibeUser> result = new ArrayList<>();
        for (Object obj : contacts) {
            if (obj instanceof TLRPC.TL_contact) {
                long userId = ((TLRPC.TL_contact) obj).user_id;
                VibeUser vu = mapUser(mc.getUser(userId));
                if (vu != null) result.add(vu);
            }
        }
        return result;
    }

    public static VibeUser getUser(long userId, int account) {
        return mapUser(MessagesController.getInstance(account).getUser(userId));
    }

    public static List<VibeUser> getUsers(List<Long> userIds, int account) {
        MessagesController mc = MessagesController.getInstance(account);
        List<VibeUser> result = new ArrayList<>();
        for (Long id : userIds) {
            VibeUser vu = mapUser(mc.getUser(id));
            if (vu != null) result.add(vu);
        }
        return result;
    }

    // --- Message Helpers (Iteration 1) ---

    public static long getMessageDate(org.telegram.messenger.MessageObject mo) {
        return mo.messageOwner.date;
    }

    public static boolean isServiceMessage(org.telegram.messenger.MessageObject mo) {
        return mo.messageOwner.action != null;
    }

    public static boolean hasMedia(org.telegram.messenger.MessageObject mo) {
        return mo.messageOwner.media != null;
    }

    public static boolean isContactMessage(org.telegram.messenger.MessageObject mo) {
        return mo.messageOwner.media instanceof TLRPC.TL_messageMediaContact;
    }

    public static boolean isLocationMessage(org.telegram.messenger.MessageObject mo) {
        return mo.messageOwner.media instanceof TLRPC.TL_messageMediaGeo ||
               mo.messageOwner.media instanceof TLRPC.TL_messageMediaGeoLive ||
               mo.messageOwner.media instanceof TLRPC.TL_messageMediaVenue;
    }

    public static boolean hasDocument(org.telegram.messenger.MessageObject mo) {
        return mo.getDocument() != null;
    }

    public static long getPeerId(org.telegram.messenger.MessageObject mo) {
        if (mo.messageOwner.peer_id == null) return 0;
        if (mo.messageOwner.peer_id.user_id != 0) return mo.messageOwner.peer_id.user_id;
        if (mo.messageOwner.peer_id.chat_id != 0) return -mo.messageOwner.peer_id.chat_id;
        if (mo.messageOwner.peer_id.channel_id != 0) return -mo.messageOwner.peer_id.channel_id;
        return 0;
    }

    public static long getFromId(org.telegram.messenger.MessageObject mo) {
        if (mo.messageOwner.from_id == null) return 0;
        if (mo.messageOwner.from_id.user_id != 0) return mo.messageOwner.from_id.user_id;
        if (mo.messageOwner.from_id.chat_id != 0) return -mo.messageOwner.from_id.chat_id;
        if (mo.messageOwner.from_id.channel_id != 0) return -mo.messageOwner.from_id.channel_id;
        return 0;
    }

    public static long getMediaId(org.telegram.messenger.MessageObject mo) {
        if (mo.isPhoto() && mo.messageOwner.media != null && mo.messageOwner.media.photo != null) {
            TLRPC.Photo photo = mo.messageOwner.media.photo;
            TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 1280);
            if (size != null) {
                ImageLocation location = ImageLocation.getForPhoto(size, photo);
                registerMediaInternal(String.valueOf(photo.id), mo.currentAccount, location);
            }
            return photo.id;
        } else if (mo.getDocument() != null) {
            TLRPC.Document doc = mo.getDocument();
            registerMediaInternal(String.valueOf(doc.id), mo.currentAccount, doc);
            return doc.id;
        }
        return 0;
    }

    private static void registerMediaInternal(String fileId, int account, Object tgObject) {
        if (mediaRegistry != null) {
            mediaRegistry.registerMedia(fileId, account, tgObject);
        }
    }

    public static long getMediaSize(org.telegram.messenger.MessageObject mo) {
        if (mo.isPhoto() && mo.messageOwner.media != null && mo.messageOwner.media.photo != null) {
            TLRPC.Photo photo = mo.messageOwner.media.photo;
            if (photo.sizes != null && !photo.sizes.isEmpty()) {
                return photo.sizes.get(photo.sizes.size() - 1).size;
            }
        } else if (mo.getDocument() != null) {
            return mo.getDocument().size;
        }
        return 0;
    }

    public static int getMediaWidth(org.telegram.messenger.MessageObject mo) {
        if (mo.isPhoto() && mo.messageOwner.media != null && mo.messageOwner.media.photo != null) {
            TLRPC.Photo photo = mo.messageOwner.media.photo;
            if (photo.sizes != null && !photo.sizes.isEmpty()) {
                return photo.sizes.get(photo.sizes.size() - 1).w;
            }
        }
        return 0;
    }

    public static int getMediaHeight(org.telegram.messenger.MessageObject mo) {
        if (mo.isPhoto() && mo.messageOwner.media != null && mo.messageOwner.media.photo != null) {
            TLRPC.Photo photo = mo.messageOwner.media.photo;
            if (photo.sizes != null && !photo.sizes.isEmpty()) {
                return photo.sizes.get(photo.sizes.size() - 1).h;
            }
        }
        return 0;
    }

    public static String getAttachFileName(Object document) {
        if (document instanceof ImageLocation) {
            ImageLocation location = (ImageLocation) document;
            if (location.path != null) {
                return location.path;
            } else if (location.photoSize != null) {
                return FileLoader.getAttachFileName(location.photoSize);
            } else if (location.document != null) {
                return FileLoader.getAttachFileName(location.document);
            }
        }
        return FileLoader.getAttachFileName((TLObject) document);
    }

    public static void startDownload(int account, Object media, int priority) {
        if (media instanceof TLRPC.Document) {
            FileLoader.getInstance(account).loadFile((TLRPC.Document) media, null, priority, 0);
        } else if (media instanceof ImageLocation) {
            FileLoader.getInstance(account).loadFile((ImageLocation) media, null, null, priority, 0);
        }
    }

    public static void cancelDownload(int account, Object media) {
        if (media instanceof TLRPC.Document) {
            FileLoader.getInstance(account).cancelLoadFile((TLRPC.Document) media);
        } else if (media instanceof ImageLocation) {
            ImageLocation location = (ImageLocation) media;
            if (location.path != null) {
                FileLoader.getInstance(account).cancelLoadFile(location.path);
            } else if (location.location != null) {
                FileLoader.getInstance(account).cancelLoadFile((TLRPC.FileLocation) location.location, null);
            }
        }
    }

    public static int getMediaDuration(org.telegram.messenger.MessageObject mo) {
        TLRPC.Document document = mo.getDocument();
        if (document != null && document.attributes != null) {
            for (int a = 0; a < document.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                    return (int) ((TLRPC.TL_documentAttributeAudio) attribute).duration;
                } else if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                    return (int) ((TLRPC.TL_documentAttributeVideo) attribute).duration;
                }
            }
        }
        return 0;
    }

    public static boolean isGif(org.telegram.messenger.MessageObject mo) {
        TLRPC.Document document = mo.getDocument();
        if (document != null && document.attributes != null) {
            for (int a = 0; a < document.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeAnimated) {
                    return true;
                }
            }
        }
        return "image/gif".equalsIgnoreCase(mo.getMimeType());
    }

    public static boolean isPinned(org.telegram.messenger.MessageObject mo) {
        return mo.messageOwner.pinned;
    }

    public static List<VibeReaction> getVibeReactions(org.telegram.messenger.MessageObject mo) {
        List<VibeReaction> result = new ArrayList<>();
        if (mo.messageOwner.reactions != null && mo.messageOwner.reactions.results != null) {
            for (TLRPC.ReactionCount rc : mo.messageOwner.reactions.results) {
                String emoji = null;
                if (rc.reaction instanceof TLRPC.TL_reactionEmoji) {
                    emoji = ((TLRPC.TL_reactionEmoji) rc.reaction).emoticon;
                }
                if (emoji != null) {
                    result.add(new VibeReaction(emoji, rc.count, rc.chosen));
                }
            }
        }
        return result;
    }

    public static boolean isDialogMuted(long dialogId, int account) {
        return MessagesController.getInstance(account).isDialogMuted(dialogId, 0);
    }

    public static String getDraftText(long dialogId, int account) {
        Object draftObj = MediaDataController.getInstance(account).getDraft(dialogId, 0);
        if (draftObj instanceof TLRPC.DraftMessage) {
            TLRPC.DraftMessage draft = (TLRPC.DraftMessage) draftObj;
            return draft.message;
        }
        return null;
    }

    public static long getDialogLastMessageDate(Object dialogObj) {
        if (dialogObj instanceof TLRPC.Dialog) {
            return ((TLRPC.Dialog) dialogObj).last_message_date;
        }
        return 0;
    }

    public static List<VibeTypingStatus> getTypingUsers(long chatId, int account) {
        List<VibeTypingStatus> result = new ArrayList<>();
        MessagesController controller = MessagesController.getInstance(account);
        java.util.concurrent.ConcurrentHashMap<Integer, ArrayList<MessagesController.PrintingUser>> threads = controller.printingUsers.get(chatId);
        if (threads != null) {
            ArrayList<MessagesController.PrintingUser> arr = threads.get(0); // 0 is default thread
            if (arr != null) {
                for (int a = 0; a < arr.size(); a++) {
                    MessagesController.PrintingUser pu = arr.get(a);
                    VibeTypingType type = VibeTypingType.UNKNOWN;
                    if (pu.action instanceof TLRPC.TL_sendMessageTypingAction) {
                        type = VibeTypingType.TYPING;
                    } else if (pu.action instanceof TLRPC.TL_sendMessageRecordAudioAction) {
                        type = VibeTypingType.RECORD_AUDIO;
                    } else if (pu.action instanceof TLRPC.TL_sendMessageRecordRoundAction) {
                        type = VibeTypingType.RECORD_VIDEO;
                    } else if (pu.action instanceof TLRPC.TL_sendMessageUploadVideoAction) {
                        type = VibeTypingType.UPLOAD_VIDEO;
                    } else if (pu.action instanceof TLRPC.TL_sendMessageUploadAudioAction) {
                        type = VibeTypingType.UPLOAD_AUDIO;
                    } else if (pu.action instanceof TLRPC.TL_sendMessageUploadPhotoAction) {
                        type = VibeTypingType.UPLOAD_PHOTO;
                    } else if (pu.action instanceof TLRPC.TL_sendMessageUploadDocumentAction) {
                        type = VibeTypingType.UPLOAD_DOCUMENT;
                    } else if (pu.action instanceof TLRPC.TL_sendMessageChooseStickerAction) {
                        type = VibeTypingType.CHOOSE_STICKER;
                    }
                    result.add(new VibeTypingStatus(pu.userId, type));
                }
            }
        }
        return result;
    }

    public static VibeReadState getReadState(long chatId, int account) {
        MessagesController controller = MessagesController.getInstance(account);
        Object dialogObj = controller.dialogs_dict.get(chatId);
        if (dialogObj instanceof TLRPC.Dialog) {
            TLRPC.Dialog dialog = (TLRPC.Dialog) dialogObj;
            int maxReadId = Math.max(dialog.read_inbox_max_id, dialog.read_outbox_max_id);
            return new VibeReadState(
                chatId,
                dialog.read_inbox_max_id,
                dialog.read_outbox_max_id,
                maxReadId,
                dialog.unread_count
            );
        }
        return new VibeReadState(chatId, 0, 0, 0, 0);
    }

    public static int getMessageLocalId(org.telegram.messenger.MessageObject mo) {
        Object owner = mo.messageOwner;
        if (owner instanceof TLRPC.Message) {
            return ((TLRPC.Message) owner).local_id;
        }
        return 0;
    }

    public static int getMessageSendState(org.telegram.messenger.MessageObject mo) {
        Object owner = mo.messageOwner;
        if (owner instanceof TLRPC.Message) {
            return ((TLRPC.Message) owner).send_state;
        }
        return 0;
    }

    public static int getNextLocalId(int account) {
        return org.telegram.messenger.UserConfig.getInstance(account).lastSendMessageId;
    }

    public static String getMessageAttachPath(org.telegram.messenger.MessageObject mo) {
        Object owner = mo.messageOwner;
        if (owner instanceof TLRPC.Message) {
            return ((TLRPC.Message) owner).attachPath;
        }
        return null;
    }

    public static org.telegram.messenger.MessageObject getMessageById(int messageId, int account) {
        return org.telegram.messenger.MessagesController.getInstance(account).dialogMessagesByIds.get(messageId);
    }

    public static org.telegram.messenger.MessageObject sendTextMessage(int account, long chatId, String text, Long replyToMsgId) {
        org.telegram.messenger.MessageObject replyToMsg = null;
        if (replyToMsgId != null) {
            replyToMsg = org.telegram.messenger.MessagesController.getInstance(account).dialogMessagesByIds.get(replyToMsgId.intValue());
            if (replyToMsg == null) {
                return null;
            }
        }

        SendMessagesHelper.SendMessageParams params;
        if (replyToMsg != null) {
            params = SendMessagesHelper.SendMessageParams.of(text, chatId, replyToMsg, null, null, true, null, null, null, true, 0, 0, null, false);
        } else {
            params = SendMessagesHelper.SendMessageParams.of(text, chatId);
        }

        int expectedId = org.telegram.messenger.UserConfig.getInstance(account).lastSendMessageId;
        SendMessagesHelper.getInstance(account).sendMessage(params);

        return org.telegram.messenger.MessagesController.getInstance(account).dialogMessagesByIds.get(expectedId);
    }

    public static void prepareAndSendPhoto(int accountIndex, long chatId, String path, String caption, Long replyToMsgId) {
        AndroidUtilities.runOnUIThread(() -> {
            org.telegram.messenger.MessageObject replyToMsg = replyToMsgId != null ? 
                org.telegram.messenger.MessagesController.getInstance(accountIndex).dialogMessagesByIds.get(replyToMsgId.intValue()) : null;
            
            SendMessagesHelper.prepareSendingPhoto(
                AccountInstance.getInstance(accountIndex),
                path,
                null, // thumbPath
                null, // imageUri
                chatId,
                replyToMsg,
                null, // replyToTopMsg
                null, // storyItem
                null, // quote
                null, // entities
                null, // stickers
                null, // inputContent
                0,    // ttl
                null, // editingMessageObject
                null, // videoEditedInfo
                true, // notify
                0,    // scheduleDate
                0,    // scheduleRepeatPeriod
                0,    // mode
                false, // forceDocument
                caption,
                null, // quickReplyShortcut
                0,    // quickReplyShortcutId
                0,    // effectId
                0,    // payStars
                0,    // monoForumPeerId
                null  // suggestionParams
            );
        });
    }

    public static void prepareAndSendVideo(int accountIndex, long chatId, String path, String caption, Long replyToMsgId) {
        AndroidUtilities.runOnUIThread(() -> {
            org.telegram.messenger.MessageObject replyToMsg = replyToMsgId != null ? 
                org.telegram.messenger.MessagesController.getInstance(accountIndex).dialogMessagesByIds.get(replyToMsgId.intValue()) : null;
            
            SendMessagesHelper.prepareSendingVideo(
                AccountInstance.getInstance(accountIndex),
                path,
                null, // VideoEditedInfo
                null, // coverPath
                null, // coverPhoto
                chatId,
                replyToMsg,
                null, // replyToTopMsg
                null, // storyItem
                null, // quote
                null, // entities
                0,    // ttl
                null, // editingMessageObject
                true, // notify
                0,    // scheduleDate
                0,    // scheduleRepeatPeriod
                false, // forceDocument
                false, // hasMediaSpoilers
                caption,
                null, // quickReplyShortcut
                0,    // quickReplyShortcutId
                0,    // effectId
                0,    // stars
                0,    // monoForumPeerId
                null, // suggestionParams
                false // invertMedia
            );
        });
    }

    public static void prepareAndSendDocument(int accountIndex, long chatId, String path, String caption, Long replyToMsgId) {
        AndroidUtilities.runOnUIThread(() -> {
            org.telegram.messenger.MessageObject replyToMsg = replyToMsgId != null ? 
                org.telegram.messenger.MessagesController.getInstance(accountIndex).dialogMessagesByIds.get(replyToMsgId.intValue()) : null;
            
            SendMessagesHelper.prepareSendingDocument(
                AccountInstance.getInstance(accountIndex),
                path,
                path, // originalPath
                null, // uri
                caption,
                null, // mine
                chatId,
                replyToMsg,
                null, // replyToTopMsg
                null, // storyItem
                null, // quote
                null, // editingMessageObject
                true, // notify
                0,    // scheduleDate
                null, // inputContent
                null, // quickReplyShortcut
                0,    // quickReplyShortcutId
                false // invertMedia
            );
        });
    }

    public static boolean cancelSendingMessage(int accountIndex, int messageId) {
        org.telegram.messenger.MessageObject msgObj = org.telegram.messenger.MessagesController.getInstance(accountIndex).dialogMessagesByIds.get(messageId);
        if (msgObj == null) {
            return false;
        }
        SendMessagesHelper.getInstance(accountIndex).cancelSendingMessage(msgObj);
        return true;
    }

    public static List<org.telegram.messenger.MessageObject> forwardMessages(int account, long fromChatId, List<Long> messageIds, long toChatId) {
        return forwardMessagesInternal(account, fromChatId, messageIds, toChatId, false);
    }

    public static List<org.telegram.messenger.MessageObject> forwardMessagesAsCopy(int account, long fromChatId, List<Long> messageIds, long toChatId) {
        return forwardMessagesInternal(account, fromChatId, messageIds, toChatId, true);
    }

    private static List<org.telegram.messenger.MessageObject> forwardMessagesInternal(int account, long fromChatId, List<Long> messageIds, long toChatId, boolean asCopy) {
        ArrayList<org.telegram.messenger.MessageObject> sourceMessages = new ArrayList<>();
        org.telegram.messenger.MessagesController controller = org.telegram.messenger.MessagesController.getInstance(account);
        
        for (Long id : messageIds) {
            org.telegram.messenger.MessageObject msg = controller.dialogMessagesByIds.get(id.intValue());
            if (msg == null || msg.getDialogId() != fromChatId) {
                return null;
            }
            sourceMessages.add(msg);
        }

        if (sourceMessages.isEmpty()) {
            return null;
        }

        int startId = org.telegram.messenger.UserConfig.getInstance(account).lastSendMessageId;
        // 3rd param is forwardFromMyName (true for copy)
        SendMessagesHelper.getInstance(account).sendMessage(sourceMessages, toChatId, asCopy, false, true, 0, 0);
        
        List<org.telegram.messenger.MessageObject> results = new ArrayList<>();
        int count = sourceMessages.size();
        for (int i = 0; i < count; i++) {
            org.telegram.messenger.MessageObject result = controller.dialogMessagesByIds.get(startId - i);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    public static org.telegram.messenger.MessageObject editMessage(int account, long chatId, long messageId, String newText) {
        org.telegram.messenger.MessagesController controller = org.telegram.messenger.MessagesController.getInstance(account);
        org.telegram.messenger.MessageObject msgObj = controller.dialogMessagesByIds.get((int) messageId);
        if (msgObj == null || msgObj.getDialogId() != chatId) {
            return null;
        }

        // Standard Telegram logic: check if we can edit
        if (!msgObj.canEditMessage(controller.getChat(-chatId))) {
            return null;
        }

        msgObj.editingMessage = newText;
        SendMessagesHelper.getInstance(account).editMessage(msgObj, null, null, null, null, null, null, false, false, null);
        return msgObj;
    }

    public static boolean deleteMessages(int account, long chatId, List<Long> messageIds, boolean revoke) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (Long id : messageIds) {
            ids.add(id.intValue());
        }

        if (ids.isEmpty()) return false;

        org.telegram.messenger.MessagesController controller = org.telegram.messenger.MessagesController.getInstance(account);
        controller.deleteMessages(ids, null, null, chatId, 0, revoke, 0);
        return true;
    }
}
