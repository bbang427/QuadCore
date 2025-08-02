package com.example.mokathon

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PostAdapter(
    private val postList: MutableList<Post>,
    private val onLikeClickListener: OnLikeClickListener, // 기존 좋아요 클릭 리스너
    private val onCommentClickListener: OnCommentClickListener // 새로운 댓글 클릭 리스너 추가
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    // 좋아요 버튼 클릭 이벤트를 전달하기 위한 인터페이스
    interface OnLikeClickListener {
        fun onLikeClick(position: Int, post: Post)
    }

    // 댓글 버튼 클릭 이벤트를 전달하기 위한 인터페이스 (추가)
    interface OnCommentClickListener {
        fun onCommentClick(post: Post)
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.tv_post_title)
        val contentTextView: TextView = itemView.findViewById(R.id.tv_post_content)
        val timestampTextView: TextView = itemView.findViewById(R.id.tv_post_timestamp)
        val authorTextView: TextView = itemView.findViewById(R.id.tv_post_author)

        // 좋아요 관련 뷰
        val likeButton: ImageView = itemView.findViewById(R.id.iv_like)
        val likeCountTextView: TextView = itemView.findViewById(R.id.tv_like_count)

        // 댓글 관련 뷰 (추가)
        val commentButton: ImageView = itemView.findViewById(R.id.iv_comment)
        val commentCountTextView: TextView = itemView.findViewById(R.id.tv_comment_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        holder.titleTextView.text = post.title
        holder.contentTextView.text = post.content
        holder.authorTextView.text = post.authorName

        post.createdAt?.let {
            holder.timestampTextView.text = formatRelativeTime(it)
        }

        // 좋아요 상태에 따라 아이콘 및 텍스트 업데이트
        holder.likeCountTextView.text = post.likeCount.toString()
        if (post.isLiked) {
            holder.likeButton.setImageResource(R.drawable.ic_like_filled)
            holder.likeButton.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.red))
        } else {
            holder.likeButton.setImageResource(R.drawable.ic_like_border)
            holder.likeButton.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.dark_gray))
        }

        // 좋아요 버튼 클릭 리스너 설정
        holder.likeButton.setOnClickListener {
            onLikeClickListener.onLikeClick(position, post)
        }

        // 댓글 수 업데이트 (Post 데이터 클래스에 commentCount 필드가 추가되어야 합니다.)
        holder.commentCountTextView.text = post.commentCount.toString()

        // 댓글 버튼 클릭 리스너 설정 (추가)
        holder.commentButton.setOnClickListener {
            onCommentClickListener.onCommentClick(post)
        }

        // 아이템 전체 클릭 리스너
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PostDetailActivity::class.java)
            intent.putExtra("post", post)
            holder.itemView.context.startActivity(intent)
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