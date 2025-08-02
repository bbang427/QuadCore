package com.example.mokathon

import com.google.firebase.firestore.Exclude // 이 한 줄을 추가합니다.
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
    @Exclude var isLiked: Boolean = false,
    @Exclude var postId: String = ""
) : Serializable