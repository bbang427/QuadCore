package com.example.mokathon

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mokathon.databinding.ActivityReportBinding
import com.google.firebase.auth.ktx.auth // [수정 1] Firebase Auth import 추가
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- 기존 UI 설정 코드는 그대로 유지 ---
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.main.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                currentFocus?.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
            false
        }
        binding.checkmarkAnimationView.visibility = View.GONE
        binding.tvRegisterNoti.visibility = View.GONE



        binding.btnCheckupload.setOnClickListener {
            val phoneNumber = binding.tvSearch.text.toString().trim()

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            binding.tvSearch.clearFocus()

            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "전화번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = Firebase.auth.currentUser
            if (user != null) {
                // 로그인 상태이면 UID를 업로드 함수에 전달
                uploadDataToFirestore(phoneNumber, user.uid)
            } else {
                // 비로그인 상태이면 사용자에게 알림
                Toast.makeText(this, "신고를 하려면 로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.checkmarkAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                binding.checkmarkAnimationView.visibility = View.GONE
                binding.tvRegisterNoti.visibility = View.GONE
            }
        })
    }

    private fun uploadDataToFirestore(number: String, userId: String) {
        val db = Firebase.firestore

        val reportData = hashMapOf(
            "phoneNumber" to number,
            "timestamp" to FieldValue.serverTimestamp(),
            "reporterUid" to userId
        )

        db.collection("reported_numbers")
            .add(reportData)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot added with ID: ${it.id}")
                binding.tvSearch.text.clear()
                binding.checkmarkAnimationView.visibility = View.VISIBLE
                binding.tvRegisterNoti.visibility = View.VISIBLE
                binding.checkmarkAnimationView.playAnimation()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
                Toast.makeText(this, "등록에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
    }
}