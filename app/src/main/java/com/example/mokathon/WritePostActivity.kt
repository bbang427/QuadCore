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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_post)

        // UI 요소 초기화
        etTitle = findViewById(R.id.et_post_title) // 레이아웃 파일에 EditText ID가 있다고 가정
        etContent = findViewById(R.id.et_post_content) // 레이아웃 파일에 EditText ID가 있다고 가정

        val ivBack: ImageView = findViewById(R.id.iv_back)
        ivBack.setOnClickListener {
            finish()
        }

        val btnSubmitPost: Button = findViewById(R.id.btn_submit_post)
        btnSubmitPost.setOnClickListener {
            // 여기에서 Toast를 대체할 실제 게시물 저장 로직을 호출합니다.
            savePost()
        }
    }

    private fun savePost() {
        val title = etTitle.text.toString()
        val content = etContent.text.toString()

        // 게시물 제목이나 내용이 비어있는지 확인합니다.
        if (title.isBlank() || content.isBlank()) {
            Toast.makeText(this, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 현재 로그인된 사용자의 정보를 가져옵니다.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "로그인 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        val authorName = if (currentUser.displayName.isNullOrBlank()) {
                "익명" // displayName이 없을 경우 기본값 설정
            } else {
                currentUser.displayName
            }


        val post = hashMapOf(
            "title" to title,
            "content" to content,
            "authorId" to currentUser.uid,
            "authorName" to authorName,
            "createdAt" to Date(),
            "likes" to 0
        )

        db.collection("posts")
            .add(post)
            .addOnSuccessListener {
                // 데이터베이스 저장 성공 시
                Toast.makeText(this, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                finish() // 게시물 등록 후 화면 종료
            }
            .addOnFailureListener { e ->
                // 데이터베이스 저장 실패 시
                Log.e("WritePostActivity", "게시물 등록 실패", e)
                Toast.makeText(this, "게시물 등록에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
    }
}