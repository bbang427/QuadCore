package com.example.mokathon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mokathon.databinding.ActivityService2Binding

class Service2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityService2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityService2Binding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
