package com.example.mokathon

import com.google.gson.annotations.SerializedName

data class NewsResponse(
    @SerializedName("items")
    val items: List<NewsItem>
)
