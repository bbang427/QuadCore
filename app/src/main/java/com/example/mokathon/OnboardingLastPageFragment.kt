package com.example.mokathon

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.method.LinkMovementMethod
import androidx.core.content.ContextCompat
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myTextView = view.findViewById<TextView>(R.id.tv_subT)
        val content = "계속하게 되면, 개인정보 처리방침 및\n이용약관에 동의하게 돼요."
        val spannableString = SpannableString(content)

        // "개인정보 처리방침" 클릭 이벤트 정의
        val privacySpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // PrivacyPolicyActivity로 이동하는 Intent 생성
                val intent = Intent(activity, PersonalInfoActivity::class.java)
                startActivity(intent)
            }

            // 링크 스타일 설정 (밑줄, 색상 등)
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // 밑줄 표시
                // 필요하다면 링크 색상 변경
                ds.color = Color.parseColor("#666666")
            }
        }

        // "이용약관" 클릭 이벤트 정의
        val termsSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // TermsActivity로 이동하는 Intent 생성
                val intent = Intent(activity, TermActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // 밑줄 표시
                ds.color = Color.parseColor("#666666")
            }
        }

        // SpannableString에 위에서 만든 ClickableSpan들을 적용
        spannableString.setSpan(privacySpan, 9, 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(termsSpan, 21, 25, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // TextView에 최종 결과 설정
        myTextView.text = spannableString

        // ★★★ 매우 중요: 이 코드가 있어야 링크 클릭이 동작합니다.
        myTextView.movementMethod = LinkMovementMethod.getInstance()
        myTextView.highlightColor = ContextCompat.getColor(requireContext(), android.R.color.transparent) // 클릭 시 배경색 투명하게
    }

    companion object {
        fun newInstance(): OnboardingLastPageFragment {
            return OnboardingLastPageFragment()
        }
    }
}