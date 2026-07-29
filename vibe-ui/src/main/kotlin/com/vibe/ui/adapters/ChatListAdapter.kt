package com.vibe.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vibe.bridge.model.VibeChat
import com.vibe.ui.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for chat list.
 */
class ChatListAdapter(
    private val onClick: (VibeChat) -> Unit
) : ListAdapter<VibeChat, ChatListAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.vibe_item_chat_row, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatAvatar: View = itemView.findViewById(R.id.chat_avatar)
        private val chatName: TextView = itemView.findViewById(R.id.chat_name)
        private val chatTime: TextView = itemView.findViewById(R.id.chat_time)
        private val chatLastMessage: TextView = itemView.findViewById(R.id.chat_last_message)
        private val chatUnreadBadge: TextView = itemView.findViewById(R.id.chat_unread_badge)

        fun bind(chat: VibeChat) {
            chatName.text = chat.title
            chatLastMessage.text = chat.lastMessage?.text ?: ""
            chatTime.text = formatTime(chat.lastActivityDate)

            // Avatar color based on chat type
            val avatarColor = when (chat.type) {
                VibeChat.ChatType.PRIVATE -> 0xFF7A4DFF.toInt()
                VibeChat.ChatType.GROUP -> 0xFF4CAF50.toInt()
                VibeChat.ChatType.SUPERGROUP -> 0xFF2196F3.toInt()
                VibeChat.ChatType.CHANNEL -> 0xFFFF9800.toInt()
            }
            val avatarBg = chatAvatar.background as? GradientDrawable
            avatarBg?.setColor(avatarColor)

            // Unread badge
            if (chat.unreadCount > 0) {
                chatUnreadBadge.visibility = View.VISIBLE
                chatUnreadBadge.text = chat.unreadCount.toString()
            } else {
                chatUnreadBadge.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(chat) }
        }

        private fun formatTime(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "сейчас"
                diff < 3_600_000 -> "${diff / 60_000} мин"
                diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                else -> SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<VibeChat>() {
        override fun areItemsTheSame(oldItem: VibeChat, newItem: VibeChat): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VibeChat, newItem: VibeChat): Boolean {
            return oldItem == newItem
        }
    }
}
