package com.example.mokathon

import com.google.firebase.firestore.Exclude
import java.io.Serializable
import java.util.Date

data class Post (
    val title: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Date? = null,
    var likeCount: Int = 0,
    var commentCount: Int = 0,
    val likers: List<String> = emptyList(),
    @Exclude var isLiked: Boolean = false,
    @Exclude var postId: String = ""
) : Serializable