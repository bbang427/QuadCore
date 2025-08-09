package com.example.mokathon

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyPostsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private var postList = mutableListOf<Post>()
    private lateinit var postAdapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        val toolbar: Toolbar = findViewById(R.id.toolbar_my_posts)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "내가 쓴 글"

        recyclerView = findViewById(R.id.rv_my_posts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        postAdapter = PostAdapter(postList,
            object : PostAdapter.OnLikeClickListener {
                override fun onLikeClick(position: Int, post: Post) {
                    updateLikeInFirestore(post.postId, !post.isLiked, auth.currentUser!!.uid) {
                        fetchMyPosts() // Refresh list after like
                    }
                }
            },
            object : PostAdapter.OnCommentClickListener {
                override fun onCommentClick(post: Post) {
                    val intent = Intent(this@MyPostsActivity, PostDetailActivity::class.java)
                    intent.putExtra("post", post)
                    startActivity(intent)
                }
            },
            object : PostAdapter.OnEditClickListener {
                override fun onEditClick(post: Post) {
                    val intent = Intent(this@MyPostsActivity, WritePostActivity::class.java).apply {
                        putExtra("isEditing", true)
                        putExtra("post", post)
                    }
                    startActivity(intent)
                }
            },
            object : PostAdapter.OnDeleteClickListener {
                override fun onDeleteClick(post: Post) {
                    AlertDialog.Builder(this@MyPostsActivity)
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

        fetchMyPosts()
    }

    override fun onResume() {
        super.onResume()
        fetchMyPosts()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun fetchMyPosts() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                postList.clear()
                for (document in result) {
                    val post = document.toObject(Post::class.java)
                    post.postId = document.id
                    post.isLiked = post.likers.contains(userId)
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
                fetchMyPosts()
            }
            .addOnFailureListener { 
                // Handle error
            }
    }
}
