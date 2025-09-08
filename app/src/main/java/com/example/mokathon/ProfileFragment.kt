package com.example.mokathon

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import coil3.load
import coil3.request.placeholder
import coil3.request.error
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.ktx.Firebase

import com.google.firebase.firestore.ktx.firestore

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val TAG = "ProfileFragment"
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private lateinit var tvMyPostsCount: TextView
    private lateinit var tvLikedPostsCount: TextView
    private lateinit var tvReportCount: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val tvProfileName: TextView = view.findViewById(R.id.tv_profile_name)
        val tvProfileEmail: TextView = view.findViewById(R.id.tv_profile_email)
        tvMyPostsCount = view.findViewById(R.id.tv_dashboard_first_num)
        tvLikedPostsCount = view.findViewById(R.id.tv_dashboard_second_num)
        tvReportCount = view.findViewById(R.id.tv_dashboard_third_num)
        val myPostsLayout: ConstraintLayout = view.findViewById(R.id.profile_dashboard_first)
        val likedPostsLayout: ConstraintLayout = view.findViewById(R.id.profile_dashboard_second)
        val reportCountLayout: ConstraintLayout = view.findViewById(R.id.profile_dashboard_third)
        val bottomDashboardButton: ConstraintLayout = view.findViewById(R.id.button_dashboard_bottom)
        val contactLayout: ConstraintLayout = view.findViewById(R.id.profile_settings_4)

        myPostsLayout.setOnClickListener {
            val intent = Intent(activity, MyPostsActivity::class.java)
            startActivity(intent)
        }

        likedPostsLayout.setOnClickListener {
            val intent = Intent(activity, LikedPostsActivity::class.java)
            startActivity(intent)
        }

        reportCountLayout.setOnClickListener {
            val intent = Intent(activity, MyReportsActivity::class.java)
            startActivity(intent)
        }

        bottomDashboardButton.setOnClickListener {
            Toast.makeText(requireContext(), "추후 업데이트로 만나보실 수 있습니다.", Toast.LENGTH_SHORT).show()
        }

        contactLayout.setOnClickListener {
            // 이메일 데이터를 담을 Intent를 생성합니다.
            // ACTION_SENDTO를 사용하면 이메일 앱만 열리도록 할 수 있습니다.
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                // 'mailto:' URI 스킴을 사용하여 이메일 클라이언트를 지정합니다.
                data = Uri.parse("mailto:")

                // 받는 사람 이메일 주소를 배열 형태로 추가합니다.
                putExtra(Intent.EXTRA_EMAIL, arrayOf("kwanwoo2005@knu.ac.kr"))
                // 이메일 제목을 추가합니다.
                //putExtra(Intent.EXTRA_SUBJECT, "여기에 이메일 제목을 입력하세요")
                // 이메일 본문 내용을 추가합니다.
                //putExtra(Intent.EXTRA_TEXT, "여기에 기본 이메일 내용을 입력하세요.")
            }

            // Gmail 앱을 직접 지정하고 싶다면 아래 주석을 해제하세요.
            emailIntent.setPackage("com.google.android.gm")

            try {
                // Intent를 실행하여 이메일 앱을 엽니다.
                startActivity(emailIntent)
            } catch (e: ActivityNotFoundException) {
                // 이메일을 처리할 수 있는 앱이 설치되어 있지 않은 경우 사용자에게 알림을 표시합니다.
                Toast.makeText(requireContext(), "메일을 보낼 수 있는 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        val user = auth.currentUser
        val name = user?.displayName ?: user?.email?.substringBefore("@") ?: "사용자"
        val email = user?.email ?: "이메일 정보 없음"

        tvProfileName.text = "$name 님"
        tvProfileEmail.text = email

        try {
            setupProfileImage(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up profile image", e)
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserDashboardCounts(tvMyPostsCount, tvLikedPostsCount, tvReportCount)
    }

    private fun loadUserDashboardCounts(myPostsCountTextView: TextView, likedPostsCountTextView: TextView, reportCountTextView: TextView) {
        val userId = auth.currentUser?.uid ?: return

        // 내가 쓴 글 수 가져오기
        db.collection("posts")
            .whereEqualTo("authorId", userId)
            .get()
            .addOnSuccessListener {
                myPostsCountTextView.text = it.size().toString()
            }
            .addOnFailureListener { 
                Log.w(TAG, "Error getting documents: ", it)
            }

        // 공감한 글 수 가져오기
        db.collection("posts")
            .whereArrayContains("likers", userId)
            .get()
            .addOnSuccessListener {
                likedPostsCountTextView.text = it.size().toString()
            }
            .addOnFailureListener { 
                Log.w(TAG, "Error getting documents: ", it)
            }

        // 제보 횟수 가져오기
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val reportCount = document.getLong("reportCount") ?: 0
                    reportCountTextView.text = reportCount.toString()
                } else {
                    reportCountTextView.text = "0"
                }
            }
            .addOnFailureListener { 
                Log.w(TAG, "Error getting user document: ", it)
                reportCountTextView.text = "0"
            }
    }

    private fun setupProfileImage(view: View) {
        // 뷰 찾기 with null check
        val imgProfile = view.findViewById<ShapeableImageView>(R.id.iv_profile_pic)
        if (imgProfile == null) {
            Log.e(TAG, "Profile image view not found")
            return
        }

        // 현재 사용자 정보 가져오기 (실시간)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val photoUri = currentUser?.photoUrl

        Log.d(TAG, "Current user: ${currentUser?.email}")
        Log.d(TAG, "Photo URI: $photoUri")

        // 이미지 로딩
        when {
            photoUri != null -> {
                Log.d(TAG, "Loading profile image from URI")
                imgProfile.load(photoUri) {
                    placeholder(R.drawable.ic_default_profile)
                    error(R.drawable.ic_default_profile)
                    transformations(CircleCropTransformation())
                    listener(
                        onStart = { Log.d(TAG, "Image loading started") },
                        onSuccess = { _, _ -> Log.d(TAG, "Image loaded successfully") },
                        onError = { _, throwable ->
                            Log.e(TAG, "Image loading failed", throwable.throwable)
                        }
                    )
                }
            }
            else -> {
                Log.d(TAG, "No photo URI, using default image")
                imgProfile.setImageResource(R.drawable.ic_default_profile)
            }
        }
    }
}