package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.example.filmverse.Domian.User
import com.example.filmverse.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {
    private lateinit var userId: String
    private lateinit var profileImg: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        profileImg = findViewById(R.id.profileImg)
        loadProfileImage()
        val usernameFromIntent = intent.getStringExtra("EXTRA_USERNAME")
        val currentUser = FirebaseAuth.getInstance().currentUser


        findViewById<ImageView>(R.id.backImage).setOnClickListener {
            val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)
            finish()
        }
        if (usernameFromIntent != null) {
            val usernameEditText = findViewById<EditText>(R.id.editText3)
            usernameEditText.setText(currentUser?.displayName ?: usernameFromIntent)
            userId = usernameFromIntent
        } else {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.changePassword).setOnClickListener {
            showChangePasswordDialog()
        }
        findViewById<Button>(R.id.signUp).setOnClickListener {
            showChangeDataDialog()
        }
    }

    private fun loadProfileImage() {
        Glide.with(this)
            .load(R.drawable.ic_profile)
            .circleCrop()
            .into(profileImg)
    }

    @SuppressLint("MissingInflatedId")
    private fun showChangeDataDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_data, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
        val currentPasswordEditText =
            dialogView.findViewById<EditText>(R.id.currentPasswordEditText)
        val confirmButton = dialogView.findViewById<Button>(R.id.button_confirm)
        val cancelButton = dialogView.findViewById<Button>(R.id.button_cancel)
        val dialog = builder.create()
        confirmButton.setOnClickListener {
            val currentPassword = currentPasswordEditText.text.toString().trim()

            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateUserData(currentPassword)
            dialog.dismiss()
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
        val currentPasswordEditText =
            dialogView.findViewById<EditText>(R.id.currentPasswordEditText)
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.newPasswordEditText)
        val newPasswordEditText2 = dialogView.findViewById<EditText>(R.id.newPasswordEditText2)
        val confirmButton = dialogView.findViewById<Button>(R.id.button_confirm)
        val cancelButton = dialogView.findViewById<Button>(R.id.button_cancel)
        val dialog = builder.create()
        confirmButton.setOnClickListener {
            val currentPassword = currentPasswordEditText.text.toString().trim()
            val newPassword = newPasswordEditText.text.toString().trim()
            val newPassword2 = newPasswordEditText2.text.toString().trim()
            if (currentPassword.isEmpty() || newPassword.isEmpty() || newPassword2.isEmpty()) {
                Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword != newPassword2) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateUserPassword(currentPassword, newPassword)
            dialog.dismiss()
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateUserPassword(currentPassword: String, newPassword: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val credential = EmailAuthProvider.getCredential(it.email!!, currentPassword)
            it.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    it.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Toast.makeText(this, "Пароль успешно изменен", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(
                                this,
                                "Ошибка изменения пароля: ${updateTask.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Ошибка аутентификации: неверный пароль",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateUserData(currentPassword: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val credential = EmailAuthProvider.getCredential(it.email!!, currentPassword)
            it.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val database = FirebaseDatabase.getInstance()
                    val userRef = database.reference.child("users").child(userId)
                    val newUsername = findViewById<EditText>(R.id.editText3).text.toString().trim()

                    userRef.get().addOnSuccessListener { dataSnapshot ->
                        val userData = dataSnapshot.getValue(User::class.java)
                        if (userData != null) {
                            // Check for existing username
                            val updatedUserRef = database.reference.child("users").child(newUsername)
                            updatedUserRef.get().addOnSuccessListener { existingSnapshot ->
                                if (existingSnapshot.exists()) {
                                    Toast.makeText(this, "Имя пользователя уже существует", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                // Proceed with the update
                                val updatedUser = User(
                                    username = newUsername,
                                    email = user.email ?: "",
                                    movies = userData.movies,
                                    history = userData.history
                                )

                                updatedUserRef.setValue(updatedUser)
                                    .addOnCompleteListener { updateTask ->
                                        if (updateTask.isSuccessful) {
                                            userRef.removeValue().addOnCompleteListener { deleteTask ->
                                                if (deleteTask.isSuccessful) {
                                                    val intent = Intent("UPDATE_USER_DATA").apply {
                                                        putExtra("newUsername", newUsername)
                                                    }
                                                    LocalBroadcastManager.getInstance(this)
                                                        .sendBroadcast(intent)
                                                    Toast.makeText(this, "Данные пользователя обновлены", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(this, "Ошибка удаления старых данных", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(this, "Ошибка обновления данных пользователя: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }.addOnFailureListener { e ->
                                Toast.makeText(this, "Ошибка проверки существующего имени пользователя", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Ошибка получения данных пользователя", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка получения данных пользователя", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Ошибка аутентификации: неверный пароль", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, AccountActivity::class.java)
        startActivity(intent)
        finish()
    }
}