package com.example.mokathon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingLastPageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signin_page, container, false) // 레이아웃을 화면으로 만듦
    }

    companion object {
        fun newInstance(): OnboardingLastPageFragment {
            return OnboardingLastPageFragment()
        }
    }
}