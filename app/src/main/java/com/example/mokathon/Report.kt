package com.example.mokathon

import com.google.firebase.Timestamp

data class Report(
    var id: String = "", // Document ID
    val phoneNumber: String = "",
    val timestamp: Timestamp? = null,
    val reporterUid: String = ""
)
