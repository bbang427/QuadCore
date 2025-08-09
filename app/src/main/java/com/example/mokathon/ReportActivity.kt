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
import com.example.mokathon.databinding.ActivityReportBinding // ⚠️ View Binding import
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ReportActivity : AppCompatActivity() {

    // View Binding 객체 선언
    private lateinit var binding: ActivityReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View Binding 초기화 및 화면 설정
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 기존의 Edge-to-Edge 및 시스템 바 설정 (그대로 유지)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 뒤로가기 버튼 활성화 (binding 사용)
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 화면 터치 시 키보드 숨기기 (binding 사용, 기존 로직 유지)
        binding.main.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                currentFocus?.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
            false
        }

        // 애니메이션 뷰와 알림 텍스트 초기 상태 설정
        binding.checkmarkAnimationView.visibility = View.GONE
        binding.tvRegisterNoti.visibility = View.GONE

        // '등록하기' 버튼 클릭 리스너 (Firestore 연동 로직 적용)
        binding.btnCheckupload.setOnClickListener {
            val phoneNumber = binding.tvSearch.text.toString().trim()

            // 키보드 숨기기
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            binding.tvSearch.clearFocus()

            if (phoneNumber.isNotEmpty()) {
                // 전화번호가 입력되었으면 Firestore에 업로드
                uploadPhoneNumberToFirestore(phoneNumber)
            } else {
                // 입력되지 않았으면 안내 메시지 표시
                Toast.makeText(this, "전화번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 애니메이션 종료 리스너 (기존 로직 유지)
        binding.checkmarkAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                // 애니메이션이 끝나면 뷰들을 다시 숨김
                binding.checkmarkAnimationView.visibility = View.GONE
                binding.tvRegisterNoti.visibility = View.GONE
            }
        })
    }


    private fun uploadPhoneNumberToFirestore(number: String) {
        val db = Firebase.firestore
        val reportData = hashMapOf(
            "phoneNumber" to number,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("reported_numbers")
            .add(reportData)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot added with ID: ${it.id}")

                binding.tvSearch.text.clear() // 입력창 비우기
                binding.checkmarkAnimationView.visibility = View.VISIBLE
                binding.tvRegisterNoti.visibility = View.VISIBLE
                binding.checkmarkAnimationView.playAnimation()
            }
            .addOnFailureListener { e ->
                // 실패 시: 로그 출력 및 사용자에게 실패 알림
                Log.w("Firestore", "Error adding document", e)
                Toast.makeText(this, "등록에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
    }
}