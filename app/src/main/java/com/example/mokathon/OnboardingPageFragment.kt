package com.example.mokathon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingPageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_login_page, container, false) // 레이아웃을 화면으로 만듦
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleTextView: TextView=view.findViewById(R.id.tv_mainT)
        val subTextView: TextView=view.findViewById(R.id.tv_subT)
        val imageView: ImageView = view.findViewById(R.id.iv_3dIcon)
        val pageNumber=arguments?.getInt("page_number") ?: 0
        val layoutParams = imageView.layoutParams

        when (pageNumber) {
            0 -> {
                titleTextView.text="보이스피싱 가능성 제시"
                subTextView.text="통화 녹음 파일을 업로드하면 딥러닝 모델을\n통해 보이스피싱인지 아닌지 알려드려요."
                imageView.setImageResource(R.drawable.recorder)
                layoutParams.width = dpToPx(192)
                layoutParams.height = dpToPx(192)
            }
            1 -> {
                titleTextView.text="의심 번호 즉시 조회"
                subTextView.text="전화번호, 계좌번호를 입력하면 경찰청에서\n사기 이력을 조회해 즉시 알려줘요."
                imageView.setImageResource(R.drawable.magnifier)
                layoutParams.width = dpToPx(180)
                layoutParams.height = dpToPx(180)
            }
            2 -> {
                titleTextView.text="함께 쌓는 안전 데이터"
                subTextView.text="클릭 한 번으로 사기가 의심되는 전화번호와\n계좌번호를 직접 제보할 수 있어요."
                imageView.setImageResource(R.drawable.siren)
                layoutParams.width = dpToPx(288)
                layoutParams.height = dpToPx(288)
            }
            3 -> {
                titleTextView.text="경험을 공유하는 공간"
                subTextView.text="나와 비슷한 경험을 겪은 사람들과 직접 소통하며\n모두가 안전해질 수 있도록 힘을 모아주세요."
                imageView.setImageResource(R.drawable.community)
                layoutParams.width = dpToPx(224)
                layoutParams.height = dpToPx(224)
            }
            4 -> {
                titleTextView.text="나만을 위한 맞춤 답변"
                subTextView.text="RAG를 적용해 한 층 더 똑똑해진 생성형 AI에게\n의심되는 부분을 언제든지 물어보세요."
                imageView.setImageResource(R.drawable.gemini)
                layoutParams.width = dpToPx(224)
                layoutParams.height = dpToPx(224)
            }
            5 -> {
                titleTextView.text="이제 시작해 볼까요?"
                subTextView.text="계속하게 되면, 개인정보 처리방침 및\n이용약관에 동의하게 돼요."
                imageView.setImageResource(R.drawable.rocket)
                layoutParams.width = dpToPx(224)
                layoutParams.height = dpToPx(224)
            }
        }
        imageView.layoutParams = layoutParams
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density

        return (dp * density).toInt()
    }

    companion object {
        fun newInstance(pageNumber: Int): OnboardingPageFragment {
            val fragment = OnboardingPageFragment()
            val args = Bundle()

            args.putInt("page_number", pageNumber)
            fragment.arguments = args

            return fragment
        }
    }
}