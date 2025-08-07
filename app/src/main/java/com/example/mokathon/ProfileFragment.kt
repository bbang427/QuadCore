package com.example.mokathon

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val tvProfileName: TextView = view.findViewById(R.id.tv_profile_name)
        val tvProfileEmail: TextView = view.findViewById(R.id.tv_profile_email)
        val tvMyPostsCount: TextView = view.findViewById(R.id.tv_dashboard_first_num)
        val tvLikedPostsCount: TextView = view.findViewById(R.id.tv_dashboard_second_num)
        val myPostsLayout: ConstraintLayout = view.findViewById(R.id.profile_dashboard_first)

        myPostsLayout.setOnClickListener {
            val intent = Intent(activity, MyPostsActivity::class.java)
            startActivity(intent)
        }

        val user = auth.currentUser
        val name = user?.displayName ?: user?.email?.substringBefore("@") ?: "사용자"
        val email = user?.email ?: "이메일 정보 없음"

        tvProfileName.text = "$name 님"
        tvProfileEmail.text = email

        loadUserPostCounts(tvMyPostsCount, tvLikedPostsCount)

        try {
            setupProfileImage(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up profile image", e)
        }
    }

    private fun loadUserPostCounts(myPostsCountTextView: TextView, likedPostsCountTextView: TextView) {
        val userId = auth.currentUser?.uid ?: return

        // 내가 쓴 글 수 가져오기
        db.collection("posts")
            .whereEqualTo("authorId", userId)
            .get()
            .addOnSuccessListener { documents ->
                myPostsCountTextView.text = documents.size().toString()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

        // 공감한 글 수 가져오기
        db.collection("posts")
            .whereArrayContains("likers", userId)
            .get()
            .addOnSuccessListener { documents ->
                likedPostsCountTextView.text = documents.size().toString()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
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