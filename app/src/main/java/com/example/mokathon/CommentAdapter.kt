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

class CommentAdapter(private val commentList: MutableList<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorTextView: TextView = itemView.findViewById(R.id.tv_comment_author)
        val timestampTextView: TextView = itemView.findViewById(R.id.tv_comment_timestamp)
        val contentTextView: TextView = itemView.findViewById(R.id.tv_comment_content)
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

        comment.createdAt?.let {
            holder.timestampTextView.text = formatRelativeTime(it)
        }
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
