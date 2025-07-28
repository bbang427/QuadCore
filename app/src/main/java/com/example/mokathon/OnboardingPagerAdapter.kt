package com.example.mokathon

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


class OnboardingPagerAdapter (fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val pageCount = 6

    override fun getItemCount(): Int {
        return pageCount
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            pageCount - 1 -> OnboardingLastPageFragment.newInstance()
            else ->OnboardingPageFragment.newInstance(position)
        }
    }
}