package com.example.mokathon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
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

        // 수정/삭제 버튼을 대체하는 '...' 아이콘
        val optionsButton: ImageView = itemView.findViewById(R.id.iv_comment_options)

        val likeButton: ImageView = itemView.findViewById(R.id.button_like)
        val likeCountTextView: TextView = itemView.findViewById(R.id.text_like_count)

        // 답글 달기 버튼 (아이콘 + 텍스트)
        val replyActionLayout: LinearLayout = itemView.findViewById(R.id.reply_action_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]
        val context = holder.itemView.context

        holder.authorTextView.text = comment.authorName
        holder.contentTextView.text = comment.content

        comment.createdAt?.let {
            holder.timestampTextView.text = formatRelativeTime(it)
        }

        // 좋아요 UI 업데이트 로직 추가
        holder.likeCountTextView.text = comment.likeCount.toString()
        val isLiked = currentUserId?.let { comment.likers.contains(it) } ?: false

        if (isLiked) {
            holder.likeButton.setImageResource(R.drawable.ic_like_filled)
            holder.likeButton.setColorFilter(ContextCompat.getColor(context, R.color.red))
            holder.likeCountTextView.setTextColor(ContextCompat.getColor(context, R.color.red))
        } else {
            holder.likeButton.setImageResource(R.drawable.ic_like_border)
            holder.likeButton.setColorFilter(ContextCompat.getColor(context, R.color.dark_gray))
            holder.likeCountTextView.setTextColor(ContextCompat.getColor(context, R.color.dark_gray))
        }

        // '...' 아이콘으로 수정/삭제 기능 통합
        if (comment.authorId == currentUserId) {
            holder.optionsButton.visibility = View.VISIBLE
            holder.optionsButton.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                popup.menuInflater.inflate(R.menu.post_options_menu, popup.menu)

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_edit_post -> {
                            onEditClick(comment)
                            true
                        }
                        R.id.action_delete_post -> {
                            onDeleteClick(comment)
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

        // 좋아요 버튼 클릭 리스너
        holder.likeButton.setOnClickListener { onLikeClick(comment) }

        // 답글 달기 버튼 클릭 리스너 (LinearLayout에 연결)
        holder.replyActionLayout.setOnClickListener { onReplyClick(comment) }
    }

    override fun getItemCount(): Int = commentList.size

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