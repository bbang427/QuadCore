package com.example.mokathon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var auth: FirebaseAuth

    private lateinit var viewPagerNews: ViewPager2
    private lateinit var progressBar: ProgressBar
    private val newsAdapter = NewsCardAdapter()

    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private val displaySize = 10

    // ▼▼▼ 캐시 시간 관리 변수 수정 ▼▼▼
    private val prefsName = "news_prefs"
    private val lastFetchTimeKey = "last_fetch_time"
    private val cacheDurationMillis = 24 * 60 * 60 * 1000L // 1일 (24시간)
    // ▲▲▲ 캐시 시간 관리 변수 수정 ▲▲▲

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 기존 코드 (수정 없음) ---
        auth = Firebase.auth
        val tvGreeting: TextView = view.findViewById(R.id.tv_greeting)
        val user = auth.currentUser
        val name = user?.displayName ?: user?.email?.substringBefore("@") ?: "사용자"
        tvGreeting.text = "안녕하세요, $name 님"
        // ... (메뉴 버튼 클릭 리스너 코드) ...
        // ------------------------------

        viewPagerNews = view.findViewById(R.id.viewpager_news)
        progressBar = view.findViewById(R.id.progressBar)
        viewPagerNews.adapter = newsAdapter

        setupScrollListener()
        initializeNewsFeed()
    }

    private fun initializeNewsFeed() {
        val lastFetchTime = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getLong(lastFetchTimeKey, 0)
        val currentTime = System.currentTimeMillis()

        if ((currentTime - lastFetchTime > cacheDurationMillis) || newsAdapter.currentList.isEmpty()) {
            resetAndFetchFirstPage()
        }
    }

    private fun resetAndFetchFirstPage() {
        currentPage = 1
        isLastPage = false
        newsAdapter.submitList(emptyList())
        fetchNewsFromApi(currentPage)
    }

    private fun setupScrollListener() {
        viewPagerNews.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (!isLoading && !isLastPage && position == newsAdapter.itemCount - 1) {
                    currentPage++
                    fetchNewsFromApi(currentPage)
                }
            }
        })
    }

    private fun fetchNewsFromApi(page: Int) {
        isLoading = true
        if (page == 1) {
            progressBar.visibility = View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val startPosition = (page - 1) * displaySize + 1
                val response = RetrofitClient.instance.getNews(
                    clientId = BuildConfig.NAVER_CLIENT_ID,
                    clientSecret = BuildConfig.NAVER_CLIENT_SECRET,
                    query = "디지털 범죄 보이스피싱",
                    display = displaySize,
                    start = startPosition
                )

                if (response.isSuccessful && response.body() != null) {
                    val newItems = response.body()!!.items
                    if (newItems.isEmpty()) {
                        isLastPage = true
                    } else {
                        val currentList = newsAdapter.currentList.toMutableList()
                        currentList.addAll(newItems)
                        newsAdapter.submitList(currentList)
                    }
                    if (page == 1) {
                        saveLastFetchTimestamp()
                    }
                } else {
                    Toast.makeText(requireContext(), "뉴스를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun saveLastFetchTimestamp() {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
        prefs.putLong(lastFetchTimeKey, System.currentTimeMillis())
        prefs.apply()
    }
}