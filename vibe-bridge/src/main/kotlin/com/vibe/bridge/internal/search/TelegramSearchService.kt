package com.vibe.bridge.internal.search

import com.vibe.bridge.api.ISearchService
import com.vibe.bridge.mapper.MessageMapper
import com.vibe.bridge.model.VibeSearchHit
import com.vibe.common.logging.VibeLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC

/**
 * Server-side message search backed by Telegram core.
 *
 * Follows the pattern of [org.telegram.ui.Adapters.DialogsSearchAdapter]:
 *  - global search uses [TLRPC.TL_messages_searchGlobal] (all chats);
 *  - in-chat search uses [TLRPC.TL_messages_search] with a peer;
 *  - each call is a single request/response via [ConnectionsManager.sendRequest],
 *    no NotificationCenter events involved;
 *  - the in-flight request is cancelled when the flow is cancelled.
 *
 * Results carry the resolved chat title so the UI can group hits without
 * additional lookups.
 */
internal class TelegramSearchService(
    private val messageMapper: MessageMapper
) : ISearchService {

    override fun searchMessages(
        query: String,
        chatId: Long?,
        limit: Int
    ): Flow<List<VibeSearchHit>> {
        if (query.isBlank()) return flowOf(emptyList())

        val safeLimit = limit.coerceIn(1, ISearchService.MAX_SEARCH_LIMIT)

        return callbackFlow {
            val account = UserConfig.selectedAccount
            val controller = MessagesController.getInstance(account)
            val connections = ConnectionsManager.getInstance(account)

            val request = if (chatId == null) {
                TLRPC.TL_messages_searchGlobal().apply {
                    q = query
                    this.limit = safeLimit
                    filter = TLRPC.TL_inputMessagesFilterEmpty()
                    flags = flags or 1
                    folder_id = 0
                    offset_rate = 0
                    offset_id = 0
                    offset_peer = TLRPC.TL_inputPeerEmpty()
                }
            } else {
                TLRPC.TL_messages_search().apply {
                    q = query
                    this.limit = safeLimit
                    filter = TLRPC.TL_inputMessagesFilterEmpty()
                    peer = controller.getInputPeer(chatId)
                }
            }

            val reqId = connections.sendRequest(
                request,
                { response, error ->
                    if (error != null) {
                        VibeLogger.w(
                            "TelegramSearchService",
                            "search failed (chatId=$chatId, query='$query'): ${error.text}"
                        )
                        trySend(emptyList())
                        close()
                        return@sendRequest
                    }

                    val res = response as? TLRPC.messages_Messages
                    if (res == null) {
                        trySend(emptyList())
                        close()
                        return@sendRequest
                    }

                    // Cache users/chats the same way Telegram's own search does,
                    // so titles/avatars resolve for the results and beyond.
                    // RPC callbacks run on stageQueue — writes to core must happen on main.
                    org.telegram.messenger.AndroidUtilities.runOnUIThread {
                        try {
                            MessagesStorage.getInstance(account)
                                .putUsersAndChats(res.users, res.chats, true, true)
                            controller.putUsers(res.users, false)
                            controller.putChats(res.chats, false)
                        } catch (e: Exception) {
                            VibeLogger.w("TelegramSearchService", "failed to cache users/chats", e)
                        }
                    }

                    val usersMap = androidx.collection.LongSparseArray<TLRPC.User>()
                    for (user in res.users) usersMap.put(user.id, user)
                    val chatsMap = androidx.collection.LongSparseArray<TLRPC.Chat>()
                    for (chat in res.chats) chatsMap.put(chat.id, chat)

                    val titleByChat = HashMap<Long, String>()
                    for (user in res.users) {
                        titleByChat[user.id] = buildUserTitle(user)
                    }
                    for (chat in res.chats) {
                        titleByChat[chat.id] = chat.title ?: "Без имени"
                    }

                    val hits = ArrayList<VibeSearchHit>(res.messages.size)
                    for (message in res.messages) {
                        val mo = MessageObject(account, message, usersMap, chatsMap, false, true)
                        val hitChatId = MessageObject.getDialogId(message)
                        hits.add(
                            VibeSearchHit(
                                message = messageMapper.mapMessage(mo),
                                chatId = hitChatId,
                                chatTitle = resolveTitle(hitChatId, titleByChat, controller, account)
                            )
                        )
                    }
                    trySend(hits)
                    close()
                },
                ConnectionsManager.RequestFlagFailOnServerErrors
            )

            awaitClose {
                connections.cancelRequest(reqId, true)
            }
        }
    }

    private fun resolveTitle(
        chatId: Long,
        titleByChat: Map<Long, String>,
        controller: MessagesController,
        account: Int
    ): String {
        titleByChat[chatId]?.let { return it }
        if (chatId > 0) {
            controller.getUser(chatId)?.let { return buildUserTitle(it) }
        } else {
            controller.getChat(-chatId)?.let { return it.title ?: "Без имени" }
        }
        return "Без имени"
    }

    private fun buildUserTitle(user: TLRPC.User?): String {
        if (user == null) return "Без имени"
        val name = (user.first_name ?: "") + " " + (user.last_name ?: "")
        return name.trim().ifEmpty { user.phone ?: "Без имени" }
    }
}
