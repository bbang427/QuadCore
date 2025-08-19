package com.example.mokathon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import android.graphics.Color


// --- 네트워크 관련 코드 ---
data class PredictionResult(
    val result: String?,
    val confidence: Double?
)

data class ServerResponse(
    val status: String?,
    val transcribed_text: String?,
    val prediction: PredictionResult?
)

interface ApiService {
    @Multipart
    @POST("/analyze")
    suspend fun uploadAudio(
        @Part audioFile: MultipartBody.Part
    ): Response<ServerResponse>
}
// --- 네트워크 관련 코드 끝 ---


class UploadActivity : AppCompatActivity() {

    private val apiService: ApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        Retrofit.Builder()
            .baseUrl("http://34.168.112.9:5000/") // ⚠️ 실제 서버 주소를 입력하세요!
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // --- UI 변수 선언 ---
    private lateinit var uploadButton: ConstraintLayout
    private lateinit var analyzeButton: ConstraintLayout
    private lateinit var fileNameTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var groupInitialUpload: Group
    // 새로 추가된 UI 변수들
    private lateinit var resultGroup: ConstraintLayout
    private lateinit var resultCard: MaterialCardView // ✅ 추가됨
    private lateinit var resultIcon: ImageView
    private lateinit var resultStatusTextView: TextView
    private lateinit var resetButton: Button

    private var selectedFileUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "권한이 허용되었습니다. 다시 버튼을 눌러주세요.", Toast.LENGTH_SHORT).show()
                openFilePicker()
            } else {
                Toast.makeText(this, "파일 접근 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                showPermissionDeniedDialog()
            }
        }

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedFileUri = it
                fileNameTextView.text = getFileName(it)
                showUiState(UiState.FILE_SELECTED)
                analyzeButton.isEnabled = true
                analyzeButton.alpha = 1.0f
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        // UI 요소 초기화
        val backBtn = findViewById<ImageView>(R.id.btn_back)
        uploadButton = findViewById(R.id.uploadButton)
        analyzeButton = findViewById(R.id.analyzeButton)
        fileNameTextView = findViewById(R.id.fileNameTextView)
        progressBar = findViewById(R.id.progressBar)
        groupInitialUpload = findViewById(R.id.group_initial_upload)
        resultGroup = findViewById(R.id.resultGroup)
        resultCard = findViewById(R.id.resultCard) // ✅ 추가됨
        resultIcon = findViewById(R.id.resultIcon)
        resultStatusTextView = findViewById(R.id.resultStatusTextView)
        resetButton = findViewById(R.id.resetButton)

        // 리스너 설정
        backBtn.setOnClickListener { finish() }
        uploadButton.setOnClickListener { checkPermissionAndOpenFilePicker() }
        analyzeButton.setOnClickListener {
            selectedFileUri?.let { uri ->
                uploadFileAndAnalyze(uri)
            } ?: Toast.makeText(this, "분석할 파일을 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
        }
        resetButton.setOnClickListener {
            showUiState(UiState.INITIAL)
        }

        // 초기 상태 설정
        showUiState(UiState.INITIAL)
    }

    // uploadFileAndAnalyze 함수 전체를 이 코드로 교체하세요.

    private fun uploadFileAndAnalyze(uri: Uri) {
        showUiState(UiState.LOADING)
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val fileRequestBody = inputStream?.readBytes()?.toRequestBody("audio/*".toMediaTypeOrNull())
                inputStream?.close()

                if (fileRequestBody == null) {
                    Toast.makeText(applicationContext, "파일을 읽을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    showUiState(UiState.FILE_SELECTED)
                    return@launch
                }

                val multipartBody = MultipartBody.Part.createFormData("voice_file", getFileName(uri), fileRequestBody)
                val response = apiService.uploadAudio(multipartBody)

                if (response.isSuccessful) {
                    val serverResponse = response.body()
                    if (serverResponse != null && serverResponse.status == "success") {
                        val prediction = serverResponse.prediction
                        val isPhishing = prediction?.result == "보이스피싱"

                        if (isPhishing) {
                            resultIcon.setImageResource(R.drawable.ic_warning)
                            resultStatusTextView.text = "   보이스피싱일 가능성이\n            높습니다!"
                            resultStatusTextView.setTextColor(ContextCompat.getColor(this@UploadActivity, android.R.color.holo_red_dark))
                            resultCard.setCardBackgroundColor(Color.parseColor("#FFEADD"))
                        } else {
                            resultIcon.setImageResource(R.drawable.ic_check_circle)
                            resultStatusTextView.text = "   보이스피싱일 가능성이\n            낮습니다."
                            resultStatusTextView.setTextColor(ContextCompat.getColor(this@UploadActivity, android.R.color.holo_green_dark))
                            resultCard.setCardBackgroundColor(Color.parseColor("#E0F7FA"))
                        }

                    } else {
                        Toast.makeText(this@UploadActivity, "분석 오류: ${serverResponse?.status}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@UploadActivity, "서버 오류: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UploadActivity, "앱 오류: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            } finally {
                val toastMessage = "결과는 참조용으로만 사용해 주세요."
                Toast.makeText(this@UploadActivity, toastMessage, Toast.LENGTH_LONG).show()
                showUiState(UiState.RESULT)
            }
        }
    }

    // ... (이하 나머지 함수들은 보내주신 코드와 동일하게 유지)
    private fun checkPermissionAndOpenFilePicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> openFilePicker()
            shouldShowRequestPermissionRationale(permission) -> showPermissionRationaleDialog()
            else -> requestPermissionLauncher.launch(permission)
        }
    }

    private fun openFilePicker() {
        filePickerLauncher.launch("audio/*")
    }

    private fun getFileName(uri: Uri): String {
        var name = "Unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("파일 접근 권한 필요")
            .setMessage("음성 파일을 업로드하려면 파일 접근 권한이 반드시 필요합니다.")
            .setPositiveButton("권한 허용") { _, _ ->
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else { Manifest.permission.READ_EXTERNAL_STORAGE }
                requestPermissionLauncher.launch(permission)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한 거부됨")
            .setMessage("파일 접근 권한이 거부되었습니다. 앱을 사용하려면 [설정]에서 직접 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    enum class UiState {
        INITIAL, FILE_SELECTED, LOADING, RESULT
    }

    private fun showUiState(state: UiState) {
        groupInitialUpload.visibility = View.GONE
        fileNameTextView.visibility = View.GONE
        progressBar.visibility = View.GONE
        resultGroup.visibility = View.GONE
        resetButton.visibility = View.GONE
        analyzeButton.visibility = View.VISIBLE
        uploadButton.visibility = View.VISIBLE

        when (state) {
            UiState.INITIAL -> {
                groupInitialUpload.visibility = View.VISIBLE
                selectedFileUri = null
                analyzeButton.isEnabled = false
                analyzeButton.alpha = 0.5f
            }
            UiState.FILE_SELECTED -> {
                fileNameTextView.visibility = View.VISIBLE
            }
            UiState.LOADING -> {
                progressBar.visibility = View.VISIBLE
                uploadButton.visibility = View.GONE
                analyzeButton.visibility = View.GONE
            }
            UiState.RESULT -> {
                resultGroup.visibility = View.VISIBLE
                resetButton.visibility = View.VISIBLE
                analyzeButton.visibility = View.GONE
                uploadButton.visibility = View.GONE
            }
        }
    }
}