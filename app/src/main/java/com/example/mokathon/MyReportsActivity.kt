package com.example.mokathon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mokathon.databinding.ActivityMyReportsBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyReportsBinding
    private lateinit var myReportsAdapter: MyReportsAdapter
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadReportedNumbers()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        myReportsAdapter = MyReportsAdapter(emptyList()) { report ->
            deleteReport(report)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyReportsActivity)
            adapter = myReportsAdapter
        }
    }

    private fun loadReportedNumbers() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // Handle user not logged in
            return
        }

        db.collection("reported_numbers")
            .whereEqualTo("reporterUid", userId)
            .get()
            .addOnSuccessListener { documents ->
                val reportList = documents.map { 
                    val report = it.toObject(Report::class.java)
                    report.id = it.id
                    report
                }
                myReportsAdapter.updateData(reportList)
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }

    private fun deleteReport(report: Report) {
        db.collection("reported_numbers").document(report.id)
            .delete()
            .addOnSuccessListener {
                // Decrement the report count in the user's profile
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val userRef = db.collection("users").document(userId)
                    userRef.update("reportCount", FieldValue.increment(-1))
                }
                loadReportedNumbers() // Reload the list after deletion
            }
            .addOnFailureListener { e ->
                // Handle error
            }
    }
}
