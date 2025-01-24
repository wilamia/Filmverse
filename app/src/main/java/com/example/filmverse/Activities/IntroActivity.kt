package com.example.filmverse.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.filmverse.R

class IntroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_intro)
        loggedIn()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.movieCountry)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val getInBtn: Button = findViewById(R.id.getInBtn)
        getInBtn.setOnClickListener { v ->
            startActivity(Intent(this, LoginActivity::class.java))
        }

    }

    private fun loggedIn() {
        if (isUserLoggedIn()) {
            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            val username = sharedPreferences.getString("username", "")
            val email = sharedPreferences.getString("email", "")
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("EXTRA_USERNAME", username)
            intent.putExtra("EXTRA_EMAIL", email)
            startActivity(intent)
            finish()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPreferences.getString("username", null) != null && sharedPreferences.getString(
            "email",
            null
        ) != null
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}