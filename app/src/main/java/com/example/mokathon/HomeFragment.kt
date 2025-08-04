package com.example.mokathon

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
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

        val clFirst = view.findViewById<ConstraintLayout>(R.id.cl_menu_first)
        val clSecond = view.findViewById<ConstraintLayout>(R.id.cl_menu_second)
        val clThird = view.findViewById<ConstraintLayout>(R.id.cl_menu_third)
        val clFourth = view.findViewById<ConstraintLayout>(R.id.cl_menu_fourth)

        // 2. 클릭 리스너 설정
        clFirst.setOnClickListener {
            //Toast.makeText(requireContext(), "통화 녹음 업로드 클릭됨", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), UploadActivity::class.java)
            startActivity(intent)
        }
        clSecond.setOnClickListener {
            //Toast.makeText(requireContext(), "계좌번호 조회 클릭됨", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), SearchAccActivity::class.java)
            startActivity(intent)
        }
        clThird.setOnClickListener {
            //Toast.makeText(requireContext(), "전화번호 조회 클릭됨", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), SearchNumActivity::class.java)
            startActivity(intent)
        }
        clFourth.setOnClickListener {
            //Toast.makeText(requireContext(), "의심 번호 제보 클릭됨", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), ReportActivity::class.java)
            startActivity(intent)
        }

        //3. 네이버뉴스 바로가기 밑줄표시
        val tv = view.findViewById<TextView>(R.id.tv_news_link)
        tv.paintFlags = tv.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }
}