package com.example.mokathon

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messages: List<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount() = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.textView_message)
        private val messageLayout: LinearLayout = itemView.findViewById(R.id.message_layout)

        fun bind(message: Message) {
            messageText.text = message.text
            if (message.sender == "user") {
                messageLayout.gravity = Gravity.END
                messageText.setBackgroundResource(R.drawable.chat_bubble_user)
                messageText.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
            } else {
                messageLayout.gravity = Gravity.START
                messageText.setBackgroundResource(R.drawable.chat_bubble_bot)
                messageText.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
            }
        }
    }
}