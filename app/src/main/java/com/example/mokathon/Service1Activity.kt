package com.example.mokathon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mokathon.databinding.ActivityService1Binding

class Service1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityService1Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityService1Binding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
