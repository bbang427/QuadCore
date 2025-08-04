package com.example.mokathon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CommentAdapter(
    private val commentList: MutableList<Comment>,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit,
    private val onLikeClick: (Comment) -> Unit,
    private val onReplyClick: (Comment) -> Unit
) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorTextView: TextView = itemView.findViewById(R.id.tv_comment_author)
        val timestampTextView: TextView = itemView.findViewById(R.id.tv_comment_timestamp)
        val contentTextView: TextView = itemView.findViewById(R.id.tv_comment_content)
        val editButton: TextView = itemView.findViewById(R.id.tv_edit_comment)
        val deleteButton: TextView = itemView.findViewById(R.id.tv_delete_comment)
        val likeButton: ImageButton = itemView.findViewById(R.id.button_like)
        val likeCountTextView: TextView = itemView.findViewById(R.id.text_like_count)
        val replyButton: ImageButton = itemView.findViewById(R.id.button_reply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]
        holder.authorTextView.text = comment.authorName
        holder.contentTextView.text = comment.content
        holder.likeCountTextView.text = comment.likes.toString()

        comment.createdAt?.let {
            holder.timestampTextView.text = formatRelativeTime(it)
        }

        if (comment.authorId == currentUserId) {
            holder.editButton.visibility = View.VISIBLE
            holder.deleteButton.visibility = View.VISIBLE
            holder.editButton.setOnClickListener { onEditClick(comment) }
            holder.deleteButton.setOnClickListener { onDeleteClick(comment) }
        } else {
            holder.editButton.visibility = View.GONE
            holder.deleteButton.visibility = View.GONE
        }

        holder.likeButton.setOnClickListener { onLikeClick(comment) }
        holder.replyButton.setOnClickListener { onReplyClick(comment) }
    }

    override fun getItemCount(): Int = commentList.size

    // 시간 포맷팅 함수 (PostAdapter와 동일)
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
