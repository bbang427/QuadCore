package com.example.mokathon

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)

        // 어댑터 연결
        viewPager.adapter = OnboardingPagerAdapter(this)

        // ViewPager2 <-> TabLayout 연결
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        /*
        // 🔥 핵심! tabView의 사이즈를 강제로 정사각형으로 설정해서 원형 drawable이 눌리지 않게 함
        tabLayout.post {
            for (i in 0 until tabLayout.tabCount) {
                val tabView = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(i)
                val params = tabView.layoutParams as ViewGroup.MarginLayoutParams
                params.width = dpToPx(8)   // 원하는 원 사이즈
                params.height = dpToPx(8)
                tabView.layoutParams = params
            }
        }
        */
    }

    // dp -> px 변환 함수
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
