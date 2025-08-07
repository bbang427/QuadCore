package com.example.mokathon

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date
import android.widget.TextView

class WritePostActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnSubmitPost: Button
    private lateinit var ivBack: ImageView

    private var existingPost: Post? = null
    private var isEditing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_post)

        etTitle = findViewById(R.id.et_post_title)
        etContent = findViewById(R.id.et_post_content)
        btnSubmitPost = findViewById(R.id.btn_submit_post)
        ivBack = findViewById(R.id.iv_back)

        // PostDetailActivity에서 전달한 인텐트 데이터 받기
        // isEditing 플래그를 사용하여 수정 모드인지 확인
        isEditing = intent.getBooleanExtra("isEditing", false)

        // 안드로이드 버전별로 직렬화된 Post 객체를 안전하게 가져오는 코드
        existingPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("post", Post::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("post") as? Post
        }

        // 수정 모드일 경우 UI에 기존 게시글 내용 채우기
        if (isEditing && existingPost != null) {
            etTitle.setText(existingPost!!.title)
            etContent.setText(existingPost!!.content)
            btnSubmitPost.text = "수정"
            findViewById<TextView>(R.id.tv_toolbar_title).text = "게시글 수정"
        } else {
            btnSubmitPost.text = "작성"
            findViewById<TextView>(R.id.tv_toolbar_title).text = "게시글 작성"
        }

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

        if (isEditing && existingPost != null) {
            // 게시물 수정 로직
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
                    Log.e("WritePostActivity", "게시물 수정 실패", e)
                    Toast.makeText(this, "게시물 수정에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        } else {
            // 새 게시물 작성 로직
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }
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
                    Log.e("WritePostActivity", "게시물 작성 실패", e)
                    Toast.makeText(this, "게시물 작성에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}