package com.example.mokathon

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieAnimationView

class ReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_report)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 뒤로가기 버튼 활성화
        val backBtn = findViewById<ImageView>(R.id.btn_back)
        backBtn.setOnClickListener {
            finish()
        }

        // 키보드 포커스 해제
        findViewById<ConstraintLayout>(R.id.main).setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                currentFocus?.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
            false
        }

        // 애니메이션 로직
        val et = findViewById<EditText>(R.id.tv_search)
        val btnUpload = findViewById<View>(R.id.btn_checkupload)
        val animView  = findViewById<LottieAnimationView>(R.id.checkmarkAnimationView)
        val tvNoti    = findViewById<TextView>(R.id.tv_register_noti)

        // 초기 상태: 둘 다 숨김
        animView.visibility = View.GONE
        tvNoti.visibility   = View.GONE

        btnUpload.setOnClickListener {
            // 1) EditText 내용 지우기 → 힌트 보이기
            et.text.clear()
            et.clearFocus()

            // 3) 기존 애니메이션 & 텍스트뷰 표시 로직
            animView.visibility = View.VISIBLE
            tvNoti.visibility   = View.VISIBLE
            animView.playAnimation()
        }
        animView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                // 애니메이션 끝나면 둘 다 숨김
                animView.visibility = View.GONE
                tvNoti.visibility   = View.GONE
            }
        })
    }
}