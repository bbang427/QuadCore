package com.example.mokathon

import java.io.Serializable
import java.util.Date

data class Post (
    val title: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Date? = null,
    val likes: Int = 0
)   : Serializable