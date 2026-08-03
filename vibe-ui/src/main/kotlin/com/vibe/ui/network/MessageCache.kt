package com.vibe.ui.network

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class MessageCache(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("vibe_message_cache", Context.MODE_PRIVATE)

    fun saveMessages(chatId: String, messages: List<SupabaseClient.Message>) {
        val arr = JSONArray()
        messages.forEach { msg ->
            val obj = JSONObject().apply {
                put("id", msg.id)
                put("chat_id", msg.chatId)
                put("sender_id", msg.senderId)
                put("content", msg.content)
                put("message_type", msg.messageType)
                put("created_at", msg.createdAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString("chat_$chatId", arr.toString()).apply()
    }

    fun getMessages(chatId: String): List<SupabaseClient.Message> {
        val str = prefs.getString("chat_$chatId", null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SupabaseClient.Message(
                    id = obj.getString("id"),
                    chatId = obj.getString("chat_id"),
                    senderId = obj.getString("sender_id"),
                    content = obj.getString("content"),
                    messageType = obj.optString("message_type", "text"),
                    createdAt = obj.optString("created_at", "")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun saveChats(chats: List<SupabaseClient.Chat>) {
        val arr = JSONArray()
        chats.forEach { chat ->
            val obj = JSONObject().apply {
                put("id", chat.id)
                put("title", chat.title)
                put("is_group", chat.isGroup)
                put("last_message", chat.lastMessage)
                put("last_message_at", chat.lastMessageAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString("chats", arr.toString()).apply()
    }

    fun getChats(): List<SupabaseClient.Chat> {
        val str = prefs.getString("chats", null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SupabaseClient.Chat(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    isGroup = obj.optBoolean("is_group", false),
                    lastMessage = obj.optString("last_message", ""),
                    lastMessageAt = obj.optString("last_message_at", "")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun addMessage(message: SupabaseClient.Message) {
        val current = getMessages(message.chatId).toMutableList()
        if (current.none { it.id == message.id }) {
            current.add(message)
            saveMessages(message.chatId, current)
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
