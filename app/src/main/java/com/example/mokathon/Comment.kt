package com.example.mokathon

import java.io.Serializable
import java.util.Date

data class Comment(
    var commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val createdAt: Date? = null,

    // 수정된 부분: 좋아요를 누른 사용자의 ID 리스트 추가
    var likers: MutableList<String> = mutableListOf(),

    // 수정된 부분: 좋아요 카운트 필드명 변경 및 타입 수정
    var likeCount: Long = 0L,

    val replies: MutableList<Comment> = mutableListOf()
) : Serializable