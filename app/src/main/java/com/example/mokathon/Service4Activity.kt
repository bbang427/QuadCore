package com.example.mokathon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mokathon.databinding.ActivityService4Binding

class Service4Activity : AppCompatActivity() {

    private lateinit var binding: ActivityService4Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityService4Binding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
