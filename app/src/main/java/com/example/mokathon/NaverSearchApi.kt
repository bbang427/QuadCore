package com.example.mokathon

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NaverSearchApi {
    @GET("v1/search/news.json")
    suspend fun getNews(
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String,
        @Query("query") query: String,
        @Query("display") display: Int,
        @Query("start") start: Int
    ): Response<NewsResponse>
}