package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.filmverse.R
import com.example.filmverse.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var editTextPassword: EditText
    private lateinit var editTextPassword2: EditText
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private lateinit var back: ImageView

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        back = findViewById(R.id.backImage5)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextPassword2 = findViewById(R.id.editTextPassword2)
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        back.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
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
        editTextPassword2.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editTextPassword2.compoundDrawablesRelative[2]
                if (drawableEnd != null && event.rawX >= (editTextPassword2.right - editTextPassword2.paddingEnd - drawableEnd.bounds.width())) {
                    toggleConfirmPasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }
        binding.signUp.setOnClickListener {
            val email = binding.editTextText.text.toString()
            val pass = editTextPassword.text.toString()
            val confirmPass = editTextPassword2.text.toString()
            val username = binding.editText3.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty() && username.isNotEmpty()) {
                if (pass == confirmPass) {
                    database.reference.child("users").child(username).get()
                        .addOnSuccessListener { snapshot ->
                            if (!snapshot.exists()) {
                                firebaseAuth.createUserWithEmailAndPassword(email, pass)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = User(username, email)
                                            database.reference.child("users").child(username)
                                                .setValue(user)
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        this,
                                                        "Регистрация успешна!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    startActivity(
                                                        Intent(
                                                            this,
                                                            LoginActivity::class.java
                                                        )
                                                    )
                                                    finish()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(
                                                        this,
                                                        "Ошибка: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        } else {
                                            Toast.makeText(
                                                this,
                                                task.exception?.message ?: "Ошибка регистрации",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                    this,
                                    "Имя пользователя уже используется",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }.addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Ошибка проверки имени пользователя: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
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

    private fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible

        editTextPassword2.inputType = if (isConfirmPasswordVisible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        editTextPassword2.setSelection(editTextPassword2.text.length)
        setDrawableConfirmEnd()
    }

    private fun setDrawableConfirmEnd() {
        val drawableRes =
            if (isConfirmPasswordVisible) R.drawable.open_eye else R.drawable.close_eye

        val drawable = ContextCompat.getDrawable(this, drawableRes)
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            editTextPassword2.setCompoundDrawablesRelative(null, null, drawable, null)
        }
    }

    data class User(val username: String, val email: String)

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}