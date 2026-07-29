package com.vibe.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vibe.bridge.model.VibeMessage
import com.vibe.ui.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for message list.
 */
class MessageListAdapter : ListAdapter<VibeMessage, MessageListAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.isOutgoing) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutRes = if (viewType == VIEW_TYPE_OUTGOING) {
            R.layout.vibe_item_message_outgoing
        } else {
            R.layout.vibe_item_message_incoming
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val messageTime: TextView = itemView.findViewById(R.id.message_time)

        fun bind(message: VibeMessage) {
            messageText.text = message.text
            messageTime.text = formatTime(message.date)
        }

        private fun formatTime(timestamp: Long): String {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp * 1000))
        }
    }

    companion object {
        const val VIEW_TYPE_OUTGOING = 1
        const val VIEW_TYPE_INCOMING = 2
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<VibeMessage>() {
        override fun areItemsTheSame(oldItem: VibeMessage, newItem: VibeMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VibeMessage, newItem: VibeMessage): Boolean {
            return oldItem == newItem
        }
    }
}
