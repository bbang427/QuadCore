package com.example.mokathon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.view.ContextThemeWrapper

class PostAdapter(
    private val postList: MutableList<Post>,
    private val onLikeClickListener: OnLikeClickListener,
    private val onCommentClickListener: OnCommentClickListener,
    private val onEditClickListener: OnEditClickListener,
    private val onDeleteClickListener: OnDeleteClickListener
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    interface OnLikeClickListener {
        fun onLikeClick(position: Int, post: Post)
    }

    interface OnCommentClickListener {
        fun onCommentClick(post: Post)
    }

    interface OnEditClickListener {
        fun onEditClick(post: Post)
    }

    interface OnDeleteClickListener {
        fun onDeleteClick(post: Post)
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.tv_post_title)
        val contentTextView: TextView = itemView.findViewById(R.id.tv_post_content)
        val authorTextView: TextView = itemView.findViewById(R.id.tv_post_author)
        val timestampTextView: TextView = itemView.findViewById(R.id.tv_post_timestamp)
        val likeButton: ImageView = itemView.findViewById(R.id.iv_like)
        val likeCountTextView: TextView = itemView.findViewById(R.id.tv_like_count)
        val commentButton: ImageView = itemView.findViewById(R.id.iv_comment)
        val commentCountTextView: TextView = itemView.findViewById(R.id.tv_comment_count)

        // 수정/삭제 버튼을 대체하는 '...' 아이콘
        val optionsButton: ImageView = itemView.findViewById(R.id.iv_post_options)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        val context = holder.itemView.context
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        holder.titleTextView.text = post.title
        holder.contentTextView.text = post.content
        holder.authorTextView.text = post.authorName
        post.createdAt?.let {
            holder.timestampTextView.text = formatRelativeTime(it)
        }

        // 좋아요 상태에 따라 아이콘과 텍스트 색상 변경
        holder.likeCountTextView.text = post.likeCount.toString()
        if (post.isLiked) {
            holder.likeButton.setImageResource(R.drawable.ic_like_filled)
            holder.likeButton.setColorFilter(ContextCompat.getColor(context, R.color.red))
            holder.likeCountTextView.setTextColor(ContextCompat.getColor(context, R.color.red))
        } else {
            holder.likeButton.setImageResource(R.drawable.ic_like_border)
            holder.likeButton.setColorFilter(ContextCompat.getColor(context, R.color.black))
            holder.likeCountTextView.setTextColor(ContextCompat.getColor(context, R.color.black))
        }

        // 댓글 수 업데이트
        holder.commentCountTextView.text = post.commentCount.toString()

        // 좋아요 버튼 클릭 리스너
        holder.likeButton.setOnClickListener {
            onLikeClickListener.onLikeClick(position, post)
        }

        // 댓글 버튼 클릭 리스너 (PostDetailActivity로 이동)
        holder.commentButton.setOnClickListener {
            onCommentClickListener.onCommentClick(post)
        }

        // 아이템 전체 클릭 시 PostDetailActivity로 이동
        holder.itemView.setOnClickListener {
            onCommentClickListener.onCommentClick(post)
        }

        // '...' 아이콘으로 수정/삭제 기능 통합
        if (post.authorId == currentUserId) {
            holder.optionsButton.visibility = View.VISIBLE
            holder.optionsButton.setOnClickListener { view ->
                // Context에 커스텀 스타일을 적용하여 PopupMenu를 생성합니다.
                val contextThemeWrapper = ContextThemeWrapper(context, R.style.CustomPopupMenuStyle)
                val popup = PopupMenu(contextThemeWrapper, view)

                popup.menuInflater.inflate(R.menu.post_options_menu, popup.menu)

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_edit_post -> {
                            onEditClickListener.onEditClick(post)
                            true
                        }
                        R.id.action_delete_post -> {
                            onDeleteClickListener.onDeleteClick(post)
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

    override fun getItemCount(): Int = postList.size

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