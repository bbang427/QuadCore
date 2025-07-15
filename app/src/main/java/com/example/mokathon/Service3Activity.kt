package com.example.mokathon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mokathon.databinding.ActivityService3Binding

class Service3Activity : AppCompatActivity() {

    private lateinit var binding: ActivityService3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityService3Binding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
