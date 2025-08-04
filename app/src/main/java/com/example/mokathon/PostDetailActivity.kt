package com.example.mokathon

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.FieldValue
import android.os.Build
import android.util.Log

class PostDetailActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rvComments: RecyclerView
    private lateinit var etCommentInput: EditText
    private lateinit var ivSendComment: ImageView
    private lateinit var tvCommentHeaderCount: TextView // 댓글 목록 헤더의 댓글 수 TextView

    private lateinit var commentAdapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()

    // 추가된 UI 요소
    private lateinit var tvDetailLikeCount: TextView
    private lateinit var ivDetailLikeIcon: ImageView
    private lateinit var tvDetailCommentCount: TextView // 본문 하단 댓글 수

    private var currentPost: Post? = null
    private var currentPostId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        val toolbar: Toolbar = findViewById(R.id.toolbar_post_detail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "게시글 상세"

        // 게시글 본문 UI 요소 바인딩
        val titleTextView: TextView = findViewById(R.id.tv_detail_title)
        val authorTextView: TextView = findViewById(R.id.tv_detail_author)
        val timestampTextView: TextView = findViewById(R.id.tv_detail_timestamp)
        val contentTextView: TextView = findViewById(R.id.tv_detail_content)

        // 좋아요/댓글 UI 요소 바인딩
        tvDetailLikeCount = findViewById(R.id.tv_detail_like_count)
        ivDetailLikeIcon = findViewById(R.id.iv_detail_like_icon)
        tvDetailCommentCount = findViewById(R.id.tv_detail_comment_count)

        // 댓글 관련 UI 요소 바인딩
        rvComments = findViewById(R.id.rv_comments)
        etCommentInput = findViewById(R.id.et_comment_input)
        ivSendComment = findViewById(R.id.iv_send_comment)
        tvCommentHeaderCount = findViewById(R.id.tv_comment_count) // 댓글 목록 헤더

        val post = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("post", Post::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("post") as? Post
        }
        currentPost = post
        currentPostId = post?.postId

        if (post != null) {
            titleTextView.text = post.title
            authorTextView.text = post.authorName
            contentTextView.text = post.content
            post.createdAt?.let {
                timestampTextView.text = formatRelativeTime(it)
            }
            updateLikeUI(post)
            updateCommentCountUI(post)
        }

        // 좋아요 버튼 클릭 리스너
        ivDetailLikeIcon.setOnClickListener {
            currentPost?.let { post ->
                toggleLike(post)
            }
        }

        rvComments.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(
            commentList,
            onEditClick = { comment -> showEditCommentDialog(comment) },
            onDeleteClick = { comment -> showDeleteCommentDialog(comment) },
            onLikeClick = { comment -> toggleLike(comment) },
            onReplyClick = { comment -> showReplyDialog(comment) }
        )
        rvComments.adapter = commentAdapter

        // 댓글 작성 버튼 클릭 리스너
        ivSendComment.setOnClickListener {
            val commentText = etCommentInput.text.toString().trim()
            if (commentText.isNotEmpty() && currentPostId != null && auth.currentUser != null) {
                addCommentToFirestore(currentPostId!!, commentText)
                etCommentInput.text.clear()
            }
        }

        if (currentPostId != null) {
            listenForPostChanges(currentPostId!!)
            loadComments(currentPostId!!)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun listenForPostChanges(postId: String) {
        db.collection("posts").document(postId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("PostDetailActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    currentPost = snapshot.toObject(Post::class.java)
                    currentPost?.let {
                        it.postId = snapshot.id
                        updateLikeUI(it)
                        updateCommentCountUI(it)
                    }
                } else {
                    Log.d("PostDetailActivity", "Current post data: null")
                }
            }
    }

    private fun updateLikeUI(post: Post) {
        val currentUser = auth.currentUser
        val isLiked = currentUser?.uid?.let { post.likers.contains(it) } ?: false

        tvDetailLikeCount.text = post.likeCount.toString()
        if (isLiked) {
            ivDetailLikeIcon.setImageResource(R.drawable.ic_like_filled)
            ivDetailLikeIcon.setColorFilter(ContextCompat.getColor(this, R.color.red))
            tvDetailLikeCount.setTextColor(ContextCompat.getColor(this, R.color.red))
        } else {
            ivDetailLikeIcon.setImageResource(R.drawable.ic_like_border)
            ivDetailLikeIcon.setColorFilter(ContextCompat.getColor(this, R.color.dark_gray))
            tvDetailLikeCount.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
        }
    }

    private fun updateCommentCountUI(post: Post) {
        tvDetailCommentCount.text = post.commentCount.toString()
        tvCommentHeaderCount.text = "(${post.commentCount})"
    }

    private fun loadComments(postId: String) {
        db.collection("posts").document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    for (change in snapshots.documentChanges) {
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                val comment = change.document.toObject(Comment::class.java).apply {
                                    commentId = change.document.id
                                }
                                commentList.add(change.newIndex, comment)
                                commentAdapter.notifyItemInserted(change.newIndex)
                            }
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                val comment = change.document.toObject(Comment::class.java).apply {
                                    commentId = change.document.id
                                }
                                if (change.oldIndex == change.newIndex) {
                                    commentList[change.oldIndex] = comment
                                    commentAdapter.notifyItemChanged(change.oldIndex)
                                } else {
                                    commentList.removeAt(change.oldIndex)
                                    commentList.add(change.newIndex, comment)
                                    commentAdapter.notifyItemMoved(change.oldIndex, change.newIndex)
                                    commentAdapter.notifyItemChanged(change.newIndex)
                                }
                            }
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                commentList.removeAt(change.oldIndex)
                                commentAdapter.notifyItemRemoved(change.oldIndex)
                            }
                        }
                    }
                    if (commentList.isNotEmpty()) {
                        rvComments.scrollToPosition(commentList.size - 1)
                    }
                }
            }
    }

    private fun addCommentToFirestore(postId: String, content: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return
        }

        val authorName = currentUser.displayName ?: "익명"

        val newComment = Comment(
            postId = postId,
            authorId = currentUser.uid,
            authorName = authorName,
            content = content,
            createdAt = Date()
        )

        val postRef = db.collection("posts").document(postId)

        db.collection("posts").document(postId).collection("comments")
            .add(newComment)
            .addOnSuccessListener {
                postRef.update("commentCount", FieldValue.increment(1))
            }
            .addOnFailureListener {
            }
    }

    private fun toggleLike(post: Post) {
        val currentUser = auth.currentUser ?: return
        val postRef = db.collection("posts").document(post.postId)
        val isCurrentlyLiked = post.likers.contains(currentUser.uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likers = snapshot.get("likers") as? MutableList<String> ?: mutableListOf()
            val newLikeCount = if (isCurrentlyLiked) {
                likers.remove(currentUser.uid)
                likers.size
            } else {
                likers.add(currentUser.uid)
                likers.size
            }
            transaction.update(postRef, "likers", likers)
            transaction.update(postRef, "likeCount", newLikeCount.toLong())
            null
        }.addOnSuccessListener {
        }.addOnFailureListener { e ->
            Log.e("PostDetailActivity", "Like transaction failed.", e)
        }
    }

    private fun showEditCommentDialog(comment: Comment) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogLayout = inflater.inflate(R.layout.dialog_edit_comment, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.et_edit_comment_input)
        editText.setText(comment.content)

        builder.setTitle("댓글 수정")
            .setView(dialogLayout)
            .setPositiveButton("수정") { dialog, which ->
                val updatedContent = editText.text.toString().trim()
                if (updatedContent.isNotEmpty()) {
                    updateComment(comment, updatedContent)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteCommentDialog(comment: Comment) {
        AlertDialog.Builder(this)
            .setTitle("댓글 삭제")
            .setMessage("정말로 이 댓글을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteComment(comment)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateComment(comment: Comment, updatedContent: String) {
        if (comment.commentId.isEmpty()) return
        db.collection("posts").document(comment.postId).collection("comments")
            .document(comment.commentId)
            .update("content", updatedContent)
            .addOnSuccessListener {
            }
            .addOnFailureListener {
            }
    }

    private fun deleteComment(comment: Comment) {
        if (comment.commentId.isEmpty()) return
        db.collection("posts").document(comment.postId).collection("comments")
            .document(comment.commentId)
            .delete()
            .addOnSuccessListener {
                db.collection("posts").document(comment.postId)
                    .update("commentCount", FieldValue.increment(-1))
            }
            .addOnFailureListener {
            }
    }

    private fun toggleLike(comment: Comment) {
        if (comment.commentId.isEmpty()) return
        val commentRef = db.collection("posts").document(comment.postId).collection("comments").document(comment.commentId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val newLikes = (snapshot.getDouble("likes") ?: 0.0) + 1
            transaction.update(commentRef, "likes", newLikes)
            null
        }.addOnSuccessListener {  }
            .addOnFailureListener {  }
    }

    private fun showReplyDialog(comment: Comment) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogLayout = inflater.inflate(R.layout.dialog_edit_comment, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.et_edit_comment_input)

        builder.setTitle("대댓글 작성")
            .setView(dialogLayout)
            .setPositiveButton("작성") { _, _ ->
                val replyText = editText.text.toString().trim()
                if (replyText.isNotEmpty()) {
                    addReplyToComment(comment, replyText)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun addReplyToComment(comment: Comment, replyText: String) {
        val currentUser = auth.currentUser ?: return
        val authorName = currentUser.displayName ?: "익명"

        val reply = Comment(
            postId = comment.postId,
            authorId = currentUser.uid,
            authorName = authorName,
            content = replyText,
            createdAt = Date()
        )

        val commentRef = db.collection("posts").document(comment.postId).collection("comments").document(comment.commentId)
        commentRef.update("replies", FieldValue.arrayUnion(reply))
    }

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