import org.gradle.kotlin.dsl.implementation
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.android.gms.oss-licenses-plugin")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.mokathon"
    compileSdk = 35

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.reader().use { localProperties.load(it) }
    }

    defaultConfig {
        applicationId = "com.example.mokathon"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // local.properties에서 Gemini API 키를 가져와 BuildConfig 필드로 설정
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY")}\"")

        buildConfigField("String", "NAVER_CLIENT_ID", "\"${localProperties.getProperty("NAVER_CLIENT_ID")}\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"${localProperties.getProperty("NAVER_CLIENT_SECRET")}\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-oss-licenses:17.3.0")
    // Firebase BOM은 다른 Firebase 라이브러리들보다 먼저 선언
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))

    // AndroidX & UI libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Firebase libraries
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx") // Kotlin KTX 버전 사용
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-functions-ktx")

    // Google Sign-in & Authentication
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0") // 최신 버전만 남김
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Image loading (Coil 3 - Android 전용)
    implementation("io.coil-kt.coil3:coil-android:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    // Unit & Instrumentation tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)

    // Gemini API
    implementation("com.google.ai.client.generativeai:generativeai:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit (네트워크 통신)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0") // HTML/Text 응답을 받기 위해 필요
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // JSON 변환기
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // 통신 로그 확인용 (선택사항)
    implementation("com.squareup.okhttp3:okhttp:4.11.0") // toMediaTypeOrNull 함수가 포함된 핵심 라이브러리

// Jsoup (HTML 파싱)
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Coroutines (비동기 처리)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") // lifecycleScope 사용

// Lottie
    implementation("com.airbnb.android:lottie:6.6.7")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.10.0")

    //viewPager2
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // OkHttp Logging Interceptor (통신 로그 확인용 - 개발에 유용)
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    implementation("com.google.android.material:material:1.11.0")

    implementation("androidx.core:core-splashscreen:1.0.1")
}