package com.example.mokathon


import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

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
        private val profileImage: ImageView = itemView.findViewById(R.id.iv_profile)
        private var typingJob: Job? = null

        fun bind(message: Message) {
            if (message.isLoading) {
                messageText.text = ""
                messageLayout.gravity = Gravity.START
                messageText.setBackgroundResource(R.drawable.chat_bubble_loading)
                profileImage.visibility = View.VISIBLE
                typingJob = CoroutineScope(Dispatchers.Main).launch {
                    val dots = listOf(".", "..", "...")
                    var dotIndex = 0
                    while (isActive) {
                        messageText.text = dots[dotIndex]
                        dotIndex = (dotIndex + 1) % dots.size
                        delay(500)
                    }
                }
            } else {
                typingJob?.cancel()
                messageText.text = message.text
                val params = messageText.layoutParams as LinearLayout.LayoutParams
                if (message.role == "user") {
                    messageLayout.gravity = Gravity.END
                    messageText.setBackgroundResource(R.drawable.chat_bubble_user)
                    messageText.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                    messageText.paint.shader = null
                    profileImage.visibility = View.GONE
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    messageText.layoutParams = params
                } else {
                    messageLayout.gravity = Gravity.START
                    messageText.background = null
                    messageText.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                    messageText.paint.shader = null
                    profileImage.visibility = View.VISIBLE
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    messageText.layoutParams = params
                }
            }
        }
    }
}