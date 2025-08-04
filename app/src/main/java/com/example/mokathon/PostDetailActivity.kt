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

class PostDetailActivity : AppCompatActivity() {

    // Firebase Firestore 및 Authentication 인스턴스
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // UI 요소 바인딩 변수
    private lateinit var rvComments: RecyclerView
    private lateinit var etCommentInput: EditText
    private lateinit var ivSendComment: ImageView
    private lateinit var tvCommentCount: TextView // 댓글 수 TextView

    // 댓글 기능 관련 데이터
    private lateinit var commentAdapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()

    // 현재 게시글 ID를 저장할 변수
    private var currentPostId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        // Toolbar를 ActionBar로 설정
        val toolbar: Toolbar = findViewById(R.id.toolbar_post_detail)
        setSupportActionBar(toolbar)

        // 뒤로가기 버튼(Up Button) 활성화
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "게시글 상세"

        // Intent에서 Post 객체와 postId를 가져와 화면에 표시
        val post = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("post", Post::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("post") as? Post
        }
        currentPostId = post?.postId

        if (post != null) {
            val titleTextView: TextView = findViewById(R.id.tv_detail_title)
            val authorTextView: TextView = findViewById(R.id.tv_detail_author)
            val timestampTextView: TextView = findViewById(R.id.tv_detail_timestamp)
            val contentTextView: TextView = findViewById(R.id.tv_detail_content)

            titleTextView.text = post.title
            authorTextView.text = post.authorName
            contentTextView.text = post.content

            post.createdAt?.let {
                timestampTextView.text = formatRelativeTime(it)
            }
        }

        // 댓글 기능 UI 요소 바인딩 및 리스너 설정
        rvComments = findViewById(R.id.rv_comments)
        etCommentInput = findViewById(R.id.et_comment_input)
        ivSendComment = findViewById(R.id.iv_send_comment)
        tvCommentCount = findViewById(R.id.tv_comment_count) // 댓글 수 TextView 바인딩

        // RecyclerView 설정
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

        // 댓글 데이터 실시간 로드
        if (currentPostId != null) {
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

    private fun loadComments(postId: String) {
        db.collection("posts").document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    // 오류 처리
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    // 댓글 목록 변경 사항 처리
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

                    // 모든 변경 사항이 처리된 후 스크롤 및 댓글 수 업데이트
                    if (commentList.isNotEmpty()) {
                        rvComments.scrollToPosition(commentList.size - 1)
                    }
                    updateCommentCount(commentList.size)
                }
            }
    }

    private fun updateCommentCount(count: Int) {
        tvCommentCount.text = "($count)"
    }

    private fun addCommentToFirestore(postId: String, content: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // 사용자에게 로그인 필요 메시지 표시
            return
        }

        // 사용자의 이름을 Firestore에서 조회하거나, 다른 방식으로 가져오는 로직이 필요
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
                // 실패
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
                // 성공
            }
            .addOnFailureListener {
                // 실패
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
                // 실패
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