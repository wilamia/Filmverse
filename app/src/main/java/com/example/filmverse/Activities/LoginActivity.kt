package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.filmverse.R
import com.example.filmverse.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var recoverPass: TextView
    private lateinit var editTextPassword: EditText
    private var isPasswordVisible = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editTextPassword = binding.editTextPassword
        initView()
        setDrawableEnd()

        recoverPass = findViewById(R.id.textView6)
        recoverPass.setOnClickListener {
            startActivity(Intent(this, RecoverPasswordActivity::class.java))
            finish()
        }

        binding.textView8.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        editTextPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd = editTextPassword.compoundDrawablesRelative[2]
                if (drawableEnd != null && event.rawX >= (editTextPassword.right - editTextPassword.paddingEnd - drawableEnd.bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }


        binding.loginBtn.setOnClickListener {
            val username = binding.editTextText.text.toString()
            val pass = editTextPassword.text.toString()

            if (username.isNotEmpty() && pass.isNotEmpty()) {
                database.reference.child("users").child(username).get()
                    .addOnSuccessListener { dataSnapshot ->
                        if (dataSnapshot.exists()) {
                            val storedEmail =
                                dataSnapshot.child("email").getValue(String::class.java)

                            if (storedEmail != null) {
                                firebaseAuth.signInWithEmailAndPassword(storedEmail, pass)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = firebaseAuth.currentUser
                                            user?.getIdToken(true)
                                                ?.addOnCompleteListener { tokenTask ->
                                                    if (tokenTask.isSuccessful) {
                                                        val token = tokenTask.result?.token
                                                        if (token != null) {
                                                            saveToken(token)
                                                        }
                                                    }
                                                }

                                            saveUserSession(username, storedEmail)
                                            startActivity(Intent(this, MainActivity::class.java))
                                            finish()
                                        } else {
                                            Toast.makeText(
                                                this,
                                                "Неверный пароль",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                    this,
                                    "Ошибка получения email пользователя",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Ошибка получения данных: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                Toast.makeText(this, "Поля не заполнены", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible

        editTextPassword.inputType = if (isPasswordVisible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        editTextPassword.setSelection(editTextPassword.text.length)
        setDrawableEnd()
    }


    private fun setDrawableEnd() {
        val drawableRes = if (isPasswordVisible) R.drawable.open_eye else R.drawable.close_eye

        val drawable = ContextCompat.getDrawable(this, drawableRes)
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            editTextPassword.setCompoundDrawablesRelative(null, null, drawable, null)
        }
    }

    private fun initView() {
        database = FirebaseDatabase.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
    }

    private fun saveToken(token: String) {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("token", token)
        editor.apply()
    }

    private fun saveUserSession(username: String, email: String) {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("username", username)
        editor.putString("email", email)
        editor.apply()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}