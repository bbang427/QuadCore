package com.example.mokathon

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mokathon.databinding.ActivitySearchAccBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.CookieManager

data class QueryResult(val isDangerous: Boolean, val displayMessage: String)

class SearchAccActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchAccBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchAccBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.ivSearch.setOnClickListener { handleSearch() }
        binding.tvSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleSearch()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

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

    private fun resetResultViews() {
        binding.tvResultNumber.visibility = View.GONE
        binding.tvResultMessageMain.visibility = View.GONE
        binding.viewResultIndicatorDanger.visibility = View.GONE
        binding.viewResultIndicatorSafe.visibility = View.GONE
        binding.tvResultDanger.visibility = View.GONE
        binding.tvResultSafe.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private suspend fun scrapeAccountInfo(accountNumber: String): QueryResult? {
        return withContext(Dispatchers.IO) {
            try {
                // --- 쿠키 관리 기능과 User-Agent가 모두 포함된 버전 ---
                val cookieJar = JavaNetCookieJar(CookieManager())
                val client = OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .build()

                val formBody = FormBody.Builder()
                    .add("key", "P")
                    .add("no", accountNumber)
                    .add("ftype", "A")
                    .build()

                val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

                val request = Request.Builder()
                    .url("https://www.police.go.kr/user/cyber/fraud.do")
                    .header("User-Agent", userAgent)
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