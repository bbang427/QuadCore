package com.example.mokathon

import android.os.Bundle
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
    }
}
