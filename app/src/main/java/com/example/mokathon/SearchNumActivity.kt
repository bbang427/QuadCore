package com.example.mokathon

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.mokathon.databinding.ActivitySearchNumBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class PhoneQueryResult(val isDangerous: Boolean, val displayMessage: String)

class SearchNumActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchNumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
        binding = ActivitySearchNumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.ivSearch.setOnClickListener {
            handleSearch()
        }

        binding.tvSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleSearch()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun handleSearch() {
        // 입력된 전화번호를 가져오는 코드
        val phoneNumber = binding.tvSearch.text.toString().trim()

        if (phoneNumber.isBlank()) {
            Toast.makeText(this, "전화번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()
        resetResultViews()
        searchPhoneNumber(phoneNumber)
    }

    private fun searchPhoneNumber(phoneNumber: String) {
        lifecycleScope.launch {
            val result: PhoneQueryResult? = scrapePhoneNumberInfo(phoneNumber)

            if (result != null) {
                updateResultViews(phoneNumber, result)
            } else {
                Toast.makeText(this@SearchNumActivity, "조회에 실패했습니다. 네트워크를 확인해주세요.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- 수정 #1: 상세 메시지를 TextView에 설정하는 코드 추가 ---
    private fun updateResultViews(searchedNumber: String, result: PhoneQueryResult) {
        binding.tvResultNumber.text = searchedNumber
        binding.tvResultNumber.visibility = View.VISIBLE
        binding.tvResultMessageMain.visibility = View.VISIBLE

        if (result.isDangerous) {
            binding.viewResultIndicatorDanger.visibility = View.VISIBLE
            binding.viewResultIndicatorSafe.visibility = View.GONE

            // '위험' 상세 설명 TextView에 상세 결과 메시지 설정
            binding.tvResultDanger.text = result.displayMessage
            binding.tvResultDanger.visibility = View.VISIBLE
            binding.tvResultSafe.visibility = View.GONE
        } else {
            binding.viewResultIndicatorDanger.visibility = View.GONE
            binding.viewResultIndicatorSafe.visibility = View.VISIBLE

            // '안전' 상세 설명 TextView에 결과 메시지 설정
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

    // --- '검색 기간' 정보를 포함하도록 수정된 최종 함수 ---
    private suspend fun scrapePhoneNumberInfo(phoneNumber: String): PhoneQueryResult? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val jsonPayload = "{\"telNum\":\"$phoneNumber\"}"
                val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("https://www.counterscam112.go.kr/main/voiceNumSearchAjax.do")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null

                    val responseBody = response.body?.string() ?: return@withContext null
                    val jsonObject = JSONObject(responseBody)

                    // 1. 어떤 경우든 'searchData'(검색 기간) 값은 항상 먼저 읽어옵니다.
                    //    '검색기간 ' 이라는 단어가 중복되므로 "검색기간 "을 제거하고 사용합니다.
                    val searchPeriod = jsonObject.getString("searchData").replace("검색기간 ", "")

                    val totalCount = jsonObject.getInt("totCnt")

                    if (totalCount > 0) {
                        // '위험' 케이스
                        val voiceCount = jsonObject.getInt("voiceCnt")
                        val smsCount = jsonObject.getInt("smsCnt")

                        // 2. 기존 메시지 아래에 두 줄 띄고 검색 기간 정보를 추가합니다.
                        val message = "총 ${totalCount}건의 신고 이력이 있어요.\n(전화: ${voiceCount}건, 문자: ${smsCount}건)"
                        PhoneQueryResult(isDangerous = true, displayMessage = message)
                    } else {
                        // '안전' 케이스
                        val safeMessage = "안심하고 연락해도 괜찮아요."

                        // 3. 안전 메시지 아래에도 검색 기간 정보를 추가합니다.

                        //val message = "$safeMessage\n\n$searchPeriod"
                        PhoneQueryResult(isDangerous = false, displayMessage = safeMessage)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}