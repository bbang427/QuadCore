package com.example.mokathon

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mokathon.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "NOOGOO"


        binding.service1.setOnClickListener {
            val intent = Intent(this, Service1Activity::class.java)
            startActivity(intent)
        }

        binding.service2.setOnClickListener {
            val intent = Intent(this, Service2Activity::class.java)
            startActivity(intent)
        }

        binding.service3.setOnClickListener {
            val intent = Intent(this, Service3Activity::class.java)
            startActivity(intent)
        }

        binding.service4.setOnClickListener {
            val intent = Intent(this, Service4Activity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                Toast.makeText(this, "설정 메뉴 클릭됨", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_search -> {
                Toast.makeText(this, "검색 메뉴 클릭됨", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
