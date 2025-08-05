package com.example.mokathon

data class Message(
    val text: String,
    val role: String,
    val timestamp: Long = 0
)