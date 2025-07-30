package com.example.mokathon

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        val tvGreeting: TextView = view.findViewById(R.id.tv_greeting)

        val user = auth.currentUser
        val name = user?.displayName ?: user?.email?.substringBefore("@") ?: "사용자"

        tvGreeting.text = "안녕하세요, $name 님"
    }
}