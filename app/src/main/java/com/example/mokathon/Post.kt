package com.example.mokathon

import java.io.Serializable
import java.util.Date
import com.google.firebase.firestore.Exclude

data class Post (
    val title: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Date? = null,
    var likeCount: Int = 0,
    @Exclude var isLiked: Boolean = false
)   : Serializable