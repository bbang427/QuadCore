package com.example.mokathon

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.TooltipCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true

        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeactivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fragment change
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_container, HomeFragment())
            .commit()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            val frag = when(item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_community -> CommunityFragment()
                R.id.nav_chatbot -> ChatbotFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> null
            }
            frag?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_container, it)
                    .commit()
                true
            } ?: false
        }

        // Tooltip text delete
        listOf(
            R.id.nav_home,
            R.id.nav_chatbot,
            R.id.nav_community,
            R.id.nav_profile
        ).forEach { itemId ->
            bottomNav.findViewById<View>(itemId)?.let { itemView ->
                TooltipCompat.setTooltipText(itemView, null)
                itemView.setOnLongClickListener { true }
            }
        }
    }
}
//안녕하세요 반가워요 잘있어요 다시만나요.