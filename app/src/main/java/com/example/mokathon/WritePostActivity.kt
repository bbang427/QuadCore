package com.example.mokathon

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth // Firebase 인증을 위한 import
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class WritePostActivity : AppCompatActivity() {

    // Firebase Firestore와 Auth 인스턴스를 미리 선언합니다.
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // UI 요소들을 전역 변수로 선언하여 다른 함수에서도 사용 가능하게 합니다.
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText

    private var existingPost: Post? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_post)

        etTitle = findViewById(R.id.et_post_title)
        etContent = findViewById(R.id.et_post_content)
        val btnSubmitPost: Button = findViewById(R.id.btn_submit_post)

        existingPost = intent.getSerializableExtra("post") as? Post

        if (existingPost != null) {
            etTitle.setText(existingPost!!.title)
            etContent.setText(existingPost!!.content)
            btnSubmitPost.text = "수정"
        }

        val ivBack: ImageView = findViewById(R.id.iv_back)
        ivBack.setOnClickListener { finish() }

        btnSubmitPost.setOnClickListener { savePost() }
    }

    private fun savePost() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (existingPost != null) {
            // 게시물 수정
            val updatedData = mapOf(
                "title" to title,
                "content" to content
            )
            db.collection("posts").document(existingPost!!.postId)
                .update(updatedData)
                .addOnSuccessListener {
                    Toast.makeText(this, "게시물이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "게시물 수정에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        } else {
            // 새 게시물 작성
            val currentUser = auth.currentUser ?: return
            val authorName = currentUser.displayName ?: "익명"

            val newPost = Post(
                title = title,
                content = content,
                authorId = currentUser.uid,
                authorName = authorName,
                createdAt = Date()
            )

            db.collection("posts")
                .add(newPost)
                .addOnSuccessListener {
                    Toast.makeText(this, "게시물이 작성되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "게시물 작성에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}