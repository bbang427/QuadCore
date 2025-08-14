package com.example.mokathon

import com.google.gson.annotations.SerializedName

data class NewsItem(
    @SerializedName("title")
    val title: String,
    @SerializedName("originallink")
    val originalLink: String,
    @SerializedName("link")
    val link: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("pubDate")
    val pubDate: String
)
