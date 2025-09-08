package com.example.mokathon

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LikedPostsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private var postList = mutableListOf<Post>()
    private lateinit var postAdapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liked_posts)

        // 1) 시스템 바를 컨텐츠 뒤로 확장
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_liked_posts)

        // 2) 상태바 아이콘을 검정색으로 (밝은 배경에 적합)
        val wic = WindowCompat.getInsetsController(window, window.decorView)
        wic.isAppearanceLightStatusBars = true

        // 3) 안전 영역 패딩 부여 (항상 존재하는 content root에 적용)
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar_liked_posts)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "공감한 글"

        recyclerView = findViewById(R.id.rv_liked_posts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        postAdapter = PostAdapter(postList,
            object : PostAdapter.OnLikeClickListener {
                override fun onLikeClick(position: Int, post: Post) {
                    updateLikeInFirestore(post.postId, !post.isLiked, auth.currentUser!!.uid) {
                        fetchLikedPosts() // Refresh list after like
                    }
                }
            },
            object : PostAdapter.OnCommentClickListener {
                override fun onCommentClick(post: Post) {
                    val intent = Intent(this@LikedPostsActivity, PostDetailActivity::class.java)
                    intent.putExtra("post", post)
                    startActivity(intent)
                }
            },
            object : PostAdapter.OnEditClickListener {
                override fun onEditClick(post: Post) {
                    val intent = Intent(this@LikedPostsActivity, WritePostActivity::class.java).apply {
                        putExtra("isEditing", true)
                        putExtra("post", post)
                    }
                    startActivity(intent)
                }
            },
            object : PostAdapter.OnDeleteClickListener {
                override fun onDeleteClick(post: Post) {
                    AlertDialog.Builder(this@LikedPostsActivity)
                        .setTitle("게시물 삭제")
                        .setMessage("정말로 이 게시물을 삭제하시겠습니까?")
                        .setPositiveButton("삭제") { _, _ ->
                            deletePostFromFirestore(post)
                        }
                        .setNegativeButton("취소", null)
                        .show()
                }
            }
        )
        recyclerView.adapter = postAdapter
    }

    override fun onResume() {
        super.onResume()
        fetchLikedPosts()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun fetchLikedPosts() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("posts")
            .whereArrayContains("likers", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                postList.clear()
                val currentUser = auth.currentUser
                for (document in result) {
                    val post = document.toObject(Post::class.java)
                    post.postId = document.id
                    if (currentUser != null) {
                        post.isLiked = post.likers.contains(currentUser.uid)
                    }
                    postList.add(post)
                }
                postAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }

    private fun updateLikeInFirestore(postId: String, isLiked: Boolean, userId: String, onComplete: () -> Unit) {
        val postRef = db.collection("posts").document(postId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likers = snapshot.get("likers") as? MutableList<String> ?: mutableListOf()
            if (isLiked) {
                likers.add(userId)
            } else {
                likers.remove(userId)
            }
            transaction.update(postRef, "likers", likers)
            transaction.update(postRef, "likeCount", likers.size.toLong())
            null
        }.addOnSuccessListener {
            onComplete()
        }.addOnFailureListener {
            // Handle error
        }
    }

    private fun deletePostFromFirestore(post: Post) {
        db.collection("posts").document(post.postId)
            .delete()
            .addOnSuccessListener {
                fetchLikedPosts()
            }
            .addOnFailureListener {
                // Handle error
            }
    }
}
