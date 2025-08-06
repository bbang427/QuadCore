package com.example.mokathon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

import android.widget.ImageView
import android.widget.PopupMenu
import com.google.firebase.auth.FirebaseAuth

class ReplyAdapter(
    private val parentCommentId: String,
    private val replyList: MutableList<Comment>,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (String, Comment) -> Unit
) :
    RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorTextView: TextView = itemView.findViewById(R.id.tv_reply_author)
        val timestampTextView: TextView = itemView.findViewById(R.id.tv_reply_timestamp)
        val contentTextView: TextView = itemView.findViewById(R.id.tv_reply_content)
        val optionsButton: ImageView = itemView.findViewById(R.id.iv_reply_options)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_reply, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        val reply = replyList[position]

        holder.authorTextView.text = reply.authorName
        holder.contentTextView.text = reply.content

        reply.createdAt?.let {
            holder.timestampTextView.text = formatRelativeTime(it)
        }

        if (reply.authorId == currentUserId) {
            holder.optionsButton.visibility = View.VISIBLE
            holder.optionsButton.setOnClickListener { view ->
                val popup = PopupMenu(holder.itemView.context, view)
                popup.menuInflater.inflate(R.menu.post_options_menu, popup.menu)

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_edit_post -> {
                            onEditClick(reply)
                            true
                        }
                        R.id.action_delete_post -> {
                            onDeleteClick(parentCommentId, reply)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        } else {
            holder.optionsButton.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = replyList.size

    private fun formatRelativeTime(date: Date): String {
        val now = Date()
        val diffInMillis = now.time - date.time

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return when {
            minutes < 1 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            hours < 24 -> "${hours}시간 전"
            days < 7 -> "${days}일 전"
            else -> {
                SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(date)
            }
        }
    }
}