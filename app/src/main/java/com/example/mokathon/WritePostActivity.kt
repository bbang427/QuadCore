package com.example.mokathon

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WritePostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_post)

        val ivBack: ImageView = findViewById(R.id.iv_back)
        ivBack.setOnClickListener {
            finish()
        }

        val btnSubmitPost: Button = findViewById(R.id.btn_submit_post)
        btnSubmitPost.setOnClickListener {
            // Here you can add the logic to save the post content.
            Toast.makeText(this, "게시글이 등록되었습니다", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}