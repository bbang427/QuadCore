package com.example.mokathon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    private lateinit var resultGroup: ConstraintLayout
    private lateinit var resultCard: MaterialCardView
    private lateinit var resultIcon: ImageView
    private lateinit var resultStatusTextView: TextView
    private lateinit var resetButton: Button

    private var selectedFileUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backBtn = findViewById<ImageView>(R.id.btn_back)
        uploadButton = findViewById(R.id.uploadButton)
        analyzeButton = findViewById(R.id.analyzeButton)
        fileNameTextView = findViewById(R.id.fileNameTextView)
        progressBar = findViewById(R.id.progressBar)
        groupInitialUpload = findViewById(R.id.group_initial_upload)
        resultGroup = findViewById(R.id.resultGroup)
        resultCard = findViewById(R.id.resultCard)
        resultIcon = findViewById(R.id.resultIcon)
        resultStatusTextView = findViewById(R.id.resultStatusTextView)
        resetButton = findViewById(R.id.resetButton)

        backBtn.setOnClickListener { finish() }
        resetButton.setOnClickListener { showUiState(UiState.INITIAL) }
        uploadButton.setOnClickListener { checkPermissionAndOpenFilePicker() }

        // ✅ [수정됨] 파일 변환 로직이 포함된 클릭 리스너
        analyzeButton.setOnClickListener {
            selectedFileUri?.let { uri ->
                lifecycleScope.launch {
                    // 1. 버튼을 누르는 즉시 UI를 로딩 상태로 변경합니다.
                    showUiState(UiState.LOADING)
                    Toast.makeText(this@UploadActivity, "파일 처리 중... 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show()

                    // 2. withContext(Dispatchers.IO)를 사용해 파일 변환/복사 작업을 백그라운드에서 처리합니다.
                    val fileToUpload: File? = withContext(Dispatchers.IO) {
                        val fileName = getFileName(uri)
                        if (fileName.endsWith(".m4a", ignoreCase = true)) {
                            // M4A 파일이면 WAV로 변환
                            convertM4aToWav(uri)
                        } else {
                            // 다른 파일이면 임시 파일로 복사
                            File(cacheDir, fileName).also {
                                contentResolver.openInputStream(uri)?.use { input ->
                                    it.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        }
                    }

                    // 3. 백그라운드 작업이 끝나면, 결과 파일을 서버로 업로드합니다.
                    if (fileToUpload != null) {
                        uploadFileAndAnalyze(fileToUpload)
                    } else {
                        Toast.makeText(this@UploadActivity, "파일 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
                        showUiState(UiState.FILE_SELECTED) // UI를 이전 상태로 복귀
                    }
                }
            } ?: run {
                Toast.makeText(this, "분석할 파일을 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        showUiState(UiState.INITIAL)
    }

    // ✅ [수정됨] 파라미터가 Uri에서 File로 변경됨
    private fun uploadFileAndAnalyze(file: File) {
        lifecycleScope.launch {
            try {
                val fileRequestBody = file.readBytes().toRequestBody("audio/*".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData("voice_file", file.name, fileRequestBody)
                val response = apiService.uploadAudio(multipartBody)

                if (response.isSuccessful) {
                    val serverResponse = response.body()
                    if (serverResponse != null && serverResponse.status == "success") {
                        val prediction = serverResponse.prediction
                        val isPhishing = prediction?.result == "보이스피싱"

                        if (isPhishing) {
                            resultIcon.setImageResource(R.drawable.ic_warning)
                            resultStatusTextView.text = "보이스피싱일 확률이 높습니다!"
                            resultStatusTextView.setTextColor(ContextCompat.getColor(this@UploadActivity, android.R.color.holo_red_dark))
                            resultCard.setCardBackgroundColor(Color.parseColor("#FFEADD"))
                        } else {
                            resultIcon.setImageResource(R.drawable.ic_check_circle)
                            resultStatusTextView.text = "보이스피싱일 확률이 낮습니다."
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
                file.delete() // ✅ 작업이 끝난 후 임시 파일 삭제
            }
        }
    }

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

    // ✅ [추가됨] M4A를 WAV로 변환하는 함수와 헬퍼 함수들
    private fun convertM4aToWav(sourceUri: Uri): File? {
        val tempWavFile = File(cacheDir, "temp_audio_${System.currentTimeMillis()}.wav")
        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(this, sourceUri, null)
            val trackIndex = findAudioTrack(extractor)
            if (trackIndex == -1) return null

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val pcmData = mutableListOf<Byte>()
            var isEOS = false

            while (!isEOS) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                while (outputBufferIndex >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isEOS = true
                    } else if (bufferInfo.size != 0) {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)!!
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        outputBuffer.clear()
                        pcmData.addAll(chunk.toList())
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                }
            }

            val pcmByteArray = pcmData.toByteArray()
            FileOutputStream(tempWavFile).use { out ->
                val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                writeWavHeader(out, pcmByteArray.size, sampleRate, channelCount)
                out.write(pcmByteArray)
            }

            codec.stop()
            codec.release()
            extractor.release()
            tempWavFile
        } catch (e: Exception) {
            e.printStackTrace()
            tempWavFile.delete()
            null
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }

    private fun writeWavHeader(out: FileOutputStream, pcmDataSize: Int, sampleRate: Int, channels: Int) {
        val header = ByteArray(44)
        val totalDataLen = pcmDataSize + 36
        val byteRate = sampleRate * channels * 2 // 16-bit PCM

        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte(); header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte(); header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // Sub-chunk size
        header[20] = 1; header[21] = 0 // PCM
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte(); header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte(); header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte(); header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte(); header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (2 * channels).toByte(); header[33] = 0 // block align
        header[34] = 16; header[35] = 0 // bits per sample
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (pcmDataSize and 0xff).toByte(); header[41] = (pcmDataSize shr 8 and 0xff).toByte()
        header[42] = (pcmDataSize shr 16 and 0xff).toByte(); header[43] = (pcmDataSize shr 24 and 0xff).toByte()
        out.write(header, 0, 44)
    }
}