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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var auth: FirebaseAuth

    private lateinit var viewPagerNews: ViewPager2
    private lateinit var progressBar: ProgressBar
    private val newsAdapter = NewsCardAdapter()

    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private val displaySize = 10

    // ▼▼▼ 날짜 기준 캐시 관리 변수로 수정 ▼▼▼
    private val prefsName = "news_prefs"
    private val lastUpdateDateKey = "last_update_date"
    // ▲▲▲ 날짜 기준 캐시 관리 변수로 수정 ▲▲▲

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Firebase Auth 및 인사말 코드 ---
        auth = Firebase.auth
        val tvGreeting: TextView = view.findViewById(R.id.tv_greeting)
        val user = auth.currentUser
        val name = user?.displayName ?: user?.email?.substringBefore("@") ?: "사용자"
        tvGreeting.text = "안녕하세요, $name 님"

        // --- 메뉴 버튼 클릭 리스너 ---
        val clFirst = view.findViewById<ConstraintLayout>(R.id.cl_menu_first)
        val clSecond = view.findViewById<ConstraintLayout>(R.id.cl_menu_second)
        val clThird = view.findViewById<ConstraintLayout>(R.id.cl_menu_third)
        val clFourth = view.findViewById<ConstraintLayout>(R.id.cl_menu_fourth)

        clFirst.setOnClickListener {
            val intent = Intent(requireContext(), UploadActivity::class.java)
            startActivity(intent)
        }
        clSecond.setOnClickListener {
            val intent = Intent(requireContext(), SearchAccActivity::class.java)
            startActivity(intent)
        }
        clThird.setOnClickListener {
            val intent = Intent(requireContext(), SearchNumActivity::class.java)
            startActivity(intent)
        }
        clFourth.setOnClickListener {
            val intent = Intent(requireContext(), ReportActivity::class.java)
            startActivity(intent)
        }

        // --- 뉴스 카드 로직 초기화 ---
        viewPagerNews = view.findViewById(R.id.viewpager_news)
        progressBar = view.findViewById(R.id.progressBar)
        viewPagerNews.adapter = newsAdapter

        setupScrollListener()
        initializeNewsFeed()
    }

    private fun initializeNewsFeed() {
        // 현재 뉴스 목록이 비어있거나, 날짜가 바뀌었다면 새로고침
        if (newsAdapter.currentList.isEmpty() || shouldUpdateByDate()) {
            resetAndFetchFirstPage()
        }
    }

    // ▼▼▼ 날짜 비교 로직 함수 ▼▼▼
    private fun shouldUpdateByDate(): Boolean {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val lastUpdateDate = prefs.getString(lastUpdateDateKey, null)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())

        // 마지막으로 저장된 날짜가 오늘 날짜와 다르면 true 반환
        return lastUpdateDate != todayDate
    }
    // ▲▲▲ 날짜 비교 로직 함수 ▲▲▲

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
                    // 첫 페이지 로드 성공 시에만 마지막 업데이트 날짜 저장
                    if (page == 1) {
                        saveLastUpdateDate()
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

    // ▼▼▼ 날짜 저장 함수로 수정 ▼▼▼
    private fun saveLastUpdateDate() {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
        prefs.putString(lastUpdateDateKey, todayDate)
        prefs.apply()
    }
    // ▲▲▲ 날짜 저장 함수로 수정 ▲▲▲
}