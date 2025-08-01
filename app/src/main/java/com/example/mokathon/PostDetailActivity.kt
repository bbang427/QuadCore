package com.example.mokathon

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem // MenuItem import 추가
import android.widget.TextView
import androidx.appcompat.widget.Toolbar // Toolbar import 추가
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PostDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        // Toolbar를 ActionBar로 설정
        val toolbar: Toolbar = findViewById(R.id.toolbar_post_detail)
        setSupportActionBar(toolbar)

        // 뒤로가기 버튼(Up Button) 활성화
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "게시글 상세" // 툴바 제목 설정

        // Intent에서 Post 객체를 가져와 화면에 표시
        val post = intent.getSerializableExtra("post") as? Post

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
    }

    // 뒤로가기 버튼 클릭 이벤트 처리
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // 현재 액티비티를 종료하고 이전 화면으로 돌아감
                true
            }
            else -> super.onOptionsItemSelected(item)
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
}