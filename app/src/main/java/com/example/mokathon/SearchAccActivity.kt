package com.example.mokathon

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.lifecycleScope
import com.example.mokathon.databinding.ActivitySearchAccBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// 전화번호 조회와 계좌번호 조회 결과를 담는 공통 데이터 클래스
data class QueryResult(val isDangerous: Boolean, val displayMessage: String)

class SearchAccActivity : AppCompatActivity() {

    // XML 파일 이름(activity_search_acc.xml)에 따라 자동으로 생성된 바인딩 클래스
    private lateinit var binding: ActivitySearchAccBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchAccBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 검색 아이콘 버튼
        binding.ivSearch.setOnClickListener {
            handleSearch()
        }

        // EditText에서 키보드의 '완료' 버튼을 눌렀을 때
        binding.tvSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleSearch()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    // 검색 로직을 처리하는 함수
    private fun handleSearch() {
        val accountNumber = binding.tvSearch.text.toString().trim()

        if (accountNumber.isBlank()) {
            Toast.makeText(this, "계좌번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()
        resetResultViews()
        performSearch(accountNumber)
    }

    // 계좌번호 검색을 시작하고 결과를 UI에 업데이트하는 함수
    private fun performSearch(accountNumber: String) {
        lifecycleScope.launch {
            val result = scrapeAccountInfo(accountNumber)

            if (result != null) {
                updateResultViews(accountNumber, result)
            } else {
                Toast.makeText(this@SearchAccActivity, "조회에 실패했습니다. 네트워크를 확인해주세요.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 검색 결과에 따라 UI를 업데이트하는 함수
    private fun updateResultViews(searchedNumber: String, result: QueryResult) {
        binding.tvResultNumber.text = searchedNumber
        binding.tvResultNumber.visibility = View.VISIBLE
        binding.tvResultMessageMain.visibility = View.VISIBLE

        if (result.isDangerous) {
            binding.viewResultIndicatorDanger.visibility = View.VISIBLE
            binding.viewResultIndicatorSafe.visibility = View.GONE
            binding.tvResultDanger.text = result.displayMessage
            binding.tvResultDanger.visibility = View.VISIBLE
            binding.tvResultSafe.visibility = View.GONE
        } else {
            binding.viewResultIndicatorDanger.visibility = View.GONE
            binding.viewResultIndicatorSafe.visibility = View.VISIBLE
            binding.tvResultSafe.text = result.displayMessage
            binding.tvResultSafe.visibility = View.VISIBLE
            binding.tvResultDanger.visibility = View.GONE
        }
    }

    // 새로운 검색 전, 결과 관련 뷰들을 모두 숨기는 함수
    private fun resetResultViews() {
        binding.tvResultNumber.visibility = View.GONE
        binding.tvResultMessageMain.visibility = View.GONE
        binding.viewResultIndicatorDanger.visibility = View.GONE
        binding.viewResultIndicatorSafe.visibility = View.GONE
        binding.tvResultDanger.visibility = View.GONE
        binding.tvResultSafe.visibility = View.GONE
    }

    // 키보드를 숨기는 유틸리티 함수
    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    /**
     * 경찰청 사이버안전지킴이에서 계좌번호 사기 이력을 조회합니다.
     * @param accountNumber 조회할 계좌번호 ('-' 제외)
     * @return 조회 결과를 담은 QueryResult 객체 (성공 시), null (실패 시)
     */
    private suspend fun scrapeAccountInfo(accountNumber: String): QueryResult? {
        return withContext(Dispatchers.IO) {
            try {
                // 기본 OkHttpClient 사용 (쿠키 기능 없이)
                val client = OkHttpClient.Builder()
                    .build()

                val formBody = FormBody.Builder()
                    .add("key", "A")
                    .add("no", accountNumber)
                    .add("ftype", "A")
                    .build()

                // 우리 앱이 일반적인 PC의 크롬 브라우저인 것처럼 위장합니다.
                val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

                val request = Request.Builder()
                    .url("https://www.police.go.kr/user/cyber/fraud.do")
                    .header("User-Agent", userAgent) // User-Agent 헤더 추가
                    .header("Referer", "https://www.police.go.kr/www/security/cyber/cyber04.jsp")
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("ScrapeError", "Request failed with code: ${response.code}")
                        return@withContext null
                    }

                    val responseBody = response.body?.string() ?: return@withContext null
                    val jsonObject = JSONObject(responseBody)
                    val valueArray = jsonObject.getJSONArray("value")

                    if (valueArray.length() > 0) {
                        val firstResultObject = valueArray.getJSONObject(0)
                        val count = firstResultObject.optInt("count", 0)

                        if (count > 0) {
                            val message = "총 ${count}건의 사기 피해 신고 이력이 있습니다.\n즉시 거래를 중단하고 확인해보세요."
                            QueryResult(isDangerous = true, displayMessage = message)
                        } else {
                            val message = "최근 3개월 내 사기 피해 신고 이력이 없습니다."
                            QueryResult(isDangerous = false, displayMessage = message)
                        }
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("ScrapeError", "계좌 조회 실패", e)
                e.printStackTrace()
                null
            }
        }
    }
}