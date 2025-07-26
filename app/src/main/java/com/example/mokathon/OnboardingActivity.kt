package com.example.mokathon

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)
        val googleLoginButton: Button = findViewById(R.id.btn_google_login)

        // 어댑터 연결
        viewPager.adapter = OnboardingPagerAdapter(this)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == viewPager.adapter!!.itemCount - 1) {
                    googleLoginButton.visibility = View.VISIBLE
                } else {
                    googleLoginButton.visibility = View.GONE
                    tabLayout.visibility = View.VISIBLE
                }
            }
        })
        // ViewPager2 <-> TabLayout 연결
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()
    }
}
