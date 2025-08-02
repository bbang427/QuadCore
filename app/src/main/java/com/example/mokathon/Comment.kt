package com.example.mokathon

import java.io.Serializable
import java.util.Date

data class Comment(
    var commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val createdAt: Date? = null
) : Serializable