/*
package com.example.mokathon

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    // Firebase 인증 및 CredentialManager 선언
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    // UI 요소 선언
    private lateinit var viewPager: ViewPager2
    private lateinit var googleLoginButton: Button
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // Firebase Auth 및 CredentialManager 초기화
        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)

        // UI 요소 초기화
        initializeUI()

        // Google 로그인 버튼 클릭 리스너 설정
        googleLoginButton.setOnClickListener {
            launchCredentialManager()
        }

        // ViewPager 설정
        setupViewPager()
    }

    override fun onStart() {
        super.onStart()
        // 앱 시작 시, 사용자가 이미 로그인되어 있는지 확인하고 UI를 업데이트합니다.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // 로그인 되어 있다면 바로 메인 액티비티(홈 화면)로 이동합니다.
            navigateToMain()
        }
    }

    /**
     * UI 관련 요소를 초기화하고 설정합니다.
     */
    private fun initializeUI() {
        // 상태바 아이콘 색상을 밝게 설정합니다.
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true

        // XML 레이아웃의 뷰들을 바인딩합니다.
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)
        googleLoginButton = findViewById(R.id.btn_google_login)
    }

    /**
     * ViewPager와 TabLayout을 설정하고 페이지 변경에 따른 UI 변화를 처리합니다.
     */
    private fun setupViewPager() {
        // ViewPager에 어댑터를 설정합니다.
        viewPager.adapter = OnboardingPagerAdapter(this)

        // ViewPager의 페이지 변경 이벤트를 감지하는 콜백을 등록합니다.
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 마지막 페이지일 경우에만 로그인 버튼을 보여줍니다.
                if (position == viewPager.adapter!!.itemCount - 1) {
                    googleLoginButton.visibility = View.VISIBLE
                    tabLayout.visibility = View.GONE // 마지막 페이지에서는 탭 레이아웃 숨김
                } else {
                    googleLoginButton.visibility = View.GONE
                    tabLayout.visibility = View.VISIBLE
                }
            }
        })

        // ViewPager와 TabLayout을 연결하여 페이지 인디케이터 역할을 하도록 합니다.
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()
    }

    /**
     * Credential Manager를 사용하여 Google 로그인을 시작합니다.
     */
    private fun launchCredentialManager() {
        // Google 로그인 옵션을 설정합니다.
        // R.string.default_web_client_id는 google-services.json 파일에 의해 자동으로 생성됩니다.
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false) // 사용자가 기기에 있는 Google 계정 중 하나를 선택하도록 허용합니다.
            .build()

        // Credential Manager에 전달할 요청을 생성합니다.
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // 코루틴을 사용하여 비동기적으로 Credential Manager를 실행합니다.
        lifecycleScope.launch {
            try {
                // Credential Manager UI를 사용자에게 보여주고 결과를 받습니다.
                val result = credentialManager.getCredential(
                    context = this@OnboardingActivity,
                    request = request
                )
                // 로그인 결과를 처리하는 함수를 호출합니다.
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                when (e) {
                    is GetCredentialCancellationException -> {
                        // 사용자가 UI를 닫아서 로그인을 취소한 경우 (오류가 아님)
                        Log.i(TAG, "사용자가 로그인을 취소했습니다.")
                    }
                    is NoCredentialException -> {
                        // 기기에 로그인된 Google 계정이 없는 경우
                        Log.e(TAG, "기기에서 Google 계정을 찾을 수 없습니다.", e)
                        Toast.makeText(this@OnboardingActivity, "기기에 설정된 Google 계정이 없습니다.", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        // 그 외 실제 오류들
                        Log.e(TAG, "Credential Manager 실행에 실패했습니다. 원인:", e)
                        Toast.makeText(this@OnboardingActivity, "로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * Credential Manager로부터 받은 로그인 결과를 처리합니다.
     */
    private fun handleSignIn(credential: Credential) {
        // 반환된 Credential이 Google ID 토큰 타입인지 확인합니다.
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                // Credential 데이터로부터 GoogleIdTokenCredential 객체를 생성합니다.
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken
                // Firebase에 Google ID 토큰으로 인증을 요청합니다.
                firebaseAuthWithGoogle(googleIdToken)
            } catch (e: Exception) {
                Log.e(TAG, "Google ID 토큰 생성에 실패했습니다.", e)
                Toast.makeText(this, "로그인 정보를 처리하는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w(TAG, "처리할 수 없는 타입의 Credential 입니다.")
            Toast.makeText(this, "지원하지 않는 로그인 방식입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 전달받은 Google ID 토큰을 사용하여 Firebase에 인증합니다.
     */
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Firebase 로그인 성공
                    Log.d(TAG, "signInWithCredential:success")
                    Toast.makeText(this, "로그인 되었습니다.", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    // Firebase 로그인 실패
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "로그인에 실패했습니다. 네트워크 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * 메인 액티비티(홈 화면)로 이동하고, 현재 액티비티는 종료합니다.
     */
    private fun navigateToMain() {
        // MainActivity.class는 실제 홈 화면 액티비티 클래스로 변경해야 합니다.
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // OnboardingActivity를 액티비티 스택에서 제거하여 뒤로가기 시 다시 보이지 않게 함
    }

    companion object {
        private const val TAG = "OnboardingActivity"
    }
}
*/

package com.example.mokathon

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

class OnboardingActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    // UI 요소 선언
    private lateinit var viewPager: ViewPager2
    private lateinit var googleLoginButton: Button
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        auth = Firebase.auth

        // --- 전통적인 Google 로그인 설정 ---
        // 1. GoogleSignInOptions 설정: default_web_client_id를 사용하여 토큰을 요청합니다.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // 2. GoogleSignInClient 생성
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 3. Google 로그인 결과를 처리할 ActivityResultLauncher 설정
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    // Google 로그인 성공
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account)
                } catch (e: ApiException) {
                    // Google 로그인 실패
                    Log.w(TAG, "Google sign in failed", e)
                    Toast.makeText(this, "SignIn Failed.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w(TAG, "Google Sign In flow was cancelled by user. Result code: " + result.resultCode)
            }
        }
        // --- 설정 끝 ---

        initializeUI()
        googleLoginButton.setOnClickListener {
            // 버튼 클릭 시 Google 로그인 창을 띄웁니다.
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
        setupViewPager()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToMain()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    //Toast.makeText(this, "로그인 되었습니다.", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Firebase Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun initializeUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)
        googleLoginButton = findViewById(R.id.btn_google_login)
    }

    private fun setupViewPager() {
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
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()
    }

    private fun navigateToMain() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "OnboardingActivity"
    }
}
