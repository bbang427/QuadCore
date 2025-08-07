package com.example.mokathon

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.FieldValue
import android.os.Build
import android.util.Log
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.view.View
import android.widget.PopupMenu
import android.content.Intent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class PostDetailActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rvComments: RecyclerView
    private lateinit var etCommentInput: EditText
    private lateinit var ivSendComment: ImageView
    private lateinit var tvCommentHeaderCount: TextView

    private lateinit var commentAdapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()

    private lateinit var tvDetailLikeCount: TextView
    private lateinit var ivDetailLikeIcon: ImageView
    private lateinit var tvDetailCommentCount: TextView

    private var currentPost: Post? = null
    private var currentPostId: String? = null

    private var isEditingComment = false
    private var editingCommentId: String? = null
    private var isReplyingToComment = false
    private var replyingToCommentId: String? = null
    private var isEditingReply = false
    private var editingReplyId: String? = null

    private lateinit var editModeHeader: LinearLayout
    private lateinit var editCancelButton: TextView
    private lateinit var postOptionsButton: ImageView

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout // 새로 추가된 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        val toolbar: Toolbar = findViewById(R.id.toolbar_post_detail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "게시글 상세"

        val titleTextView: TextView = findViewById(R.id.tv_detail_title)
        val authorTextView: TextView = findViewById(R.id.tv_detail_author)
        val timestampTextView: TextView = findViewById(R.id.tv_detail_timestamp)
        val contentTextView: TextView = findViewById(R.id.tv_detail_content)

        tvDetailLikeCount = findViewById(R.id.tv_detail_like_count)
        ivDetailLikeIcon = findViewById(R.id.iv_detail_like_icon)
        tvDetailCommentCount = findViewById(R.id.tv_detail_comment_count)

        rvComments = findViewById(R.id.rv_comments)
        etCommentInput = findViewById(R.id.et_comment_input)
        ivSendComment = findViewById(R.id.iv_send_comment)
        tvCommentHeaderCount = findViewById(R.id.tv_comment_count)

        editModeHeader = findViewById(R.id.edit_mode_header)
        editCancelButton = findViewById(R.id.tv_edit_cancel)

        postOptionsButton = findViewById(R.id.iv_post_options)

        // SwipeRefreshLayout 초기화 및 리스너 설정
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

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

            val postAuthorId = post.authorId
            val currentUserId = auth.currentUser?.uid

            if (postAuthorId != null && postAuthorId == currentUserId) {
                postOptionsButton.visibility = View.VISIBLE
            } else {
                postOptionsButton.visibility = View.GONE
            }

            postOptionsButton.setOnClickListener { view ->
                showPostOptionsMenu(view)
            }
        } else {
            postOptionsButton.visibility = View.GONE
        }

        ivDetailLikeIcon.setOnClickListener {
            currentPost?.let { post ->
                toggleLike(post)
            }
        }

        setupCommentsRecyclerView()
        setupCommentInput()

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

    private fun showPostOptionsMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.post_options_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_post -> {
                    val intent = Intent(this, WritePostActivity::class.java).apply {
                        putExtra("isEditing", true)
                        putExtra("post", currentPost)
                    }
                    startActivity(intent)
                    return@setOnMenuItemClickListener true
                }
                R.id.action_delete_post -> {
                    showDeletePostDialog()
                    return@setOnMenuItemClickListener true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showDeletePostDialog() {
        AlertDialog.Builder(this)
            .setTitle("게시글 삭제")
            .setMessage("정말로 이 게시글을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deletePost()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deletePost() {
        currentPostId?.let { postId ->
            db.collection("posts").document(postId)
                .delete()
                .addOnSuccessListener {
                    Log.d("PostDetailActivity", "게시글이 성공적으로 삭제되었습니다.")
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("PostDetailActivity", "게시글 삭제 실패", e)
                }
        }
    }

    private fun setupCommentsRecyclerView() {
        val onEditClick: (Comment) -> Unit = { comment ->
            // 답글/댓글 수정 로직 통합
            val isReply = commentList.any { parentComment -> parentComment.replies.contains(comment) }

            if (isReply) {
                isEditingReply = true
                editingReplyId = comment.commentId
                isEditingComment = false
                editingCommentId = null
                isReplyingToComment = false
                replyingToCommentId = null

                editModeHeader.visibility = View.VISIBLE
                etCommentInput.setText(comment.content)
                etCommentInput.hint = "답글을 수정하세요."
            } else { // It's a comment
                isEditingComment = true
                editingCommentId = comment.commentId
                isReplyingToComment = false
                replyingToCommentId = null
                isEditingReply = false
                editingReplyId = null

                editModeHeader.visibility = View.VISIBLE
                etCommentInput.setText(comment.content)
                etCommentInput.hint = "댓글을 수정하세요."
            }

            etCommentInput.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etCommentInput, InputMethodManager.SHOW_IMPLICIT)
        }

        val onReplyClick: (Comment) -> Unit = { comment ->
            isReplyingToComment = true
            replyingToCommentId = comment.commentId
            isEditingComment = false
            editingCommentId = null

            etCommentInput.text.clear()
            etCommentInput.hint = "${comment.authorName}님에게 답글을 남겨주세요."

            editModeHeader.visibility = View.GONE

            etCommentInput.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etCommentInput, InputMethodManager.SHOW_IMPLICIT)
        }

        val onDeleteClick: (String, Comment) -> Unit = { parentCommentId, comment -> showDeleteCommentDialog(parentCommentId, comment) }
        val onLikeClick: (Comment) -> Unit = { comment -> toggleCommentLike(comment) }

        rvComments.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(
            commentList,
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick,
            onLikeClick = onLikeClick,
            onReplyClick = onReplyClick
        )
        rvComments.adapter = commentAdapter

        editCancelButton.setOnClickListener {
            resetCommentInputUI()
        }
    }

    private fun setupCommentInput() {
        ivSendComment.setOnClickListener {
            val commentText = etCommentInput.text.toString().trim()
            if (commentText.isNotEmpty() && currentPostId != null && auth.currentUser != null) {
                if (isEditingComment) {
                    updateComment(editingCommentId!!, commentText)
                } else if (isReplyingToComment) {
                    addReplyToComment(replyingToCommentId!!, commentText)
                } else if (isEditingReply) {
                    updateReply(editingReplyId!!, commentText)
                } else {
                    addCommentToFirestore(currentPostId!!, commentText)
                }

                resetCommentInputUI()
            }
        }
    }

    private fun resetCommentInputUI() {
        isEditingComment = false
        editingCommentId = null
        isReplyingToComment = false
        replyingToCommentId = null

        editModeHeader.visibility = View.GONE
        etCommentInput.hint = "댓글을 입력하세요."
        etCommentInput.text.clear()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etCommentInput.windowToken, 0)
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

    private fun updateReply(replyId: String, updatedContent: String) {
        // This is a simplified implementation. You might need to adjust it based on your data structure.
        // This assumes that you can uniquely identify the reply with replyId.
        // You might need to find the parent comment first and then update the specific reply.
        // For now, we will assume a similar structure to updateComment.
        db.collection("posts").document(currentPostId!!).collection("comments")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val comment = document.toObject(Comment::class.java)
                    val reply = comment.replies.find { it.commentId == replyId }
                    if (reply != null) {
                        val updatedReplies = comment.replies.map {
                            if (it.commentId == replyId) {
                                it.copy(content = updatedContent)
                            } else {
                                it
                            }
                        }
                        db.collection("posts").document(currentPostId!!).collection("comments").document(document.id)
                            .update("replies", updatedReplies)
                            .addOnSuccessListener {
                                Log.d("PostDetailActivity", "Reply updated successfully")
                                refreshData()
                            }
                            .addOnFailureListener { e ->
                                Log.e("PostDetailActivity", "Error updating reply", e)
                            }
                        break
                    }
                }
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

    private fun updateComment(commentId: String, updatedContent: String) {
        db.collection("posts").document(currentPostId!!).collection("comments")
            .document(commentId)
            .update("content", updatedContent)
            .addOnSuccessListener {
                Log.d("PostDetailActivity", "Comment updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("PostDetailActivity", "Error updating comment", e)
            }
    }

    private fun showDeleteCommentDialog(parentCommentId: String, comment: Comment) {
        AlertDialog.Builder(this)
            .setTitle("삭제")
            .setMessage("정말로 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteComment(parentCommentId, comment)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteComment(parentCommentId: String, comment: Comment) {
        if (comment.commentId.isEmpty() && comment.content.isEmpty()) return

        if (parentCommentId == comment.commentId) { // It's a comment
            db.collection("posts").document(comment.postId).collection("comments")
                .document(comment.commentId)
                .delete()
                .addOnSuccessListener {
                    db.collection("posts").document(comment.postId)
                        .update("commentCount", FieldValue.increment(-(1 + comment.replies.size).toLong()))
                    refreshData()
                }
                .addOnFailureListener {
                }
        } else { // It's a reply
            val commentRef = db.collection("posts").document(currentPostId!!).collection("comments").document(parentCommentId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(commentRef)
                val replies = snapshot.get("replies") as? MutableList<HashMap<String, Any>> ?: mutableListOf()

                val replyToRemove = if (comment.commentId.isNotEmpty()) {
                    replies.find { it["commentId"] == comment.commentId }
                } else {
                    // Fallback for old replies without a commentId
                    replies.find {
                        val content = it["content"] as? String
                        val createdAt = (it["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                        content == comment.content && createdAt == comment.createdAt
                    }
                }

                if (replyToRemove != null) {
                    replies.remove(replyToRemove)
                    transaction.update(commentRef, "replies", replies)
                }
                null
            }.addOnSuccessListener {
                db.collection("posts").document(currentPostId!!)
                    .update("commentCount", FieldValue.increment(-1))
                Log.d("PostDetailActivity", "Reply deleted successfully")
            }.addOnFailureListener { e ->
                Log.e("PostDetailActivity", "Error deleting reply", e)
            }
        }
    }

    private fun toggleCommentLike(comment: Comment) {
        if (comment.commentId.isEmpty()) return
        val commentRef = db.collection("posts").document(comment.postId).collection("comments").document(comment.commentId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val likers = snapshot.get("likers") as? MutableList<String> ?: mutableListOf()
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                if (likers.contains(currentUserId)) {
                    likers.remove(currentUserId)
                } else {
                    likers.add(currentUserId)
                }
                transaction.update(commentRef, "likers", likers)
                transaction.update(commentRef, "likeCount", likers.size.toLong())
            }
            null
        }.addOnSuccessListener {  }
            .addOnFailureListener {  }
    }

    private fun addReplyToComment(commentId: String, replyText: String) {
        val currentUser = auth.currentUser ?: return
        val authorName = currentUser.displayName ?: "익명"

        val replyId = db.collection("posts").document().id // Generate a unique ID for the reply
        val reply = Comment(
            commentId = replyId, // Assign the unique ID
            postId = currentPostId!!,
            authorId = currentUser.uid,
            authorName = authorName,
            content = replyText,
            createdAt = Date()
        )

        val commentRef = db.collection("posts").document(currentPostId!!).collection("comments").document(commentId)
        commentRef.update("replies", FieldValue.arrayUnion(reply))
            .addOnSuccessListener {
                val postRef = db.collection("posts").document(currentPostId!!)
                postRef.update("commentCount", FieldValue.increment(1))
                refreshData()
            }
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

    /**
     * 데이터를 새로고침하는 함수. SwipeRefreshLayout에 연결됩니다.
     */
    private fun refreshData() {
        Log.d("PostDetailActivity", "Refreshing data...")
        if (currentPostId != null) {
            // 게시글 데이터 재로드
            db.collection("posts").document(currentPostId!!)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        currentPost = snapshot.toObject(Post::class.java)?.apply {
                            postId = snapshot.id
                        }
                        currentPost?.let {
                            // 1. 게시글의 UI를 업데이트하는 코드 추가
                            findViewById<TextView>(R.id.tv_detail_title).text = it.title
                            findViewById<TextView>(R.id.tv_detail_author).text = it.authorName
                            findViewById<TextView>(R.id.tv_detail_content).text = it.content
                            it.createdAt?.let { date ->
                                findViewById<TextView>(R.id.tv_detail_timestamp).text = formatRelativeTime(date)
                            }

                            // 2. 좋아요 및 댓글 카운트 UI 업데이트 (기존 코드)
                            updateLikeUI(it)
                            updateCommentCountUI(it)
                        }
                    }

                    // 댓글 목록 초기화 후 재로드 (기존 코드)
                    commentList.clear()
                    commentAdapter.notifyDataSetChanged()
                    loadComments(currentPostId!!)

                    // 새로고침 완료 후 아이콘 숨기기
                    swipeRefreshLayout.isRefreshing = false
                    Log.d("PostDetailActivity", "Refresh complete.")
                }
                .addOnFailureListener { e ->
                    Log.e("PostDetailActivity", "Refresh failed", e)
                    // 실패 시에도 아이콘 숨기기
                    swipeRefreshLayout.isRefreshing = false
                }
        } else {
            // 게시글 ID가 없을 경우 바로 새로고침 중지
            swipeRefreshLayout.isRefreshing = false
        }
    }

}
