package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.filmverse.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class AccountActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var profileImg: ImageView
    private lateinit var favourite: Button
    private lateinit var history: Button
    private lateinit var loading: View
    private lateinit var layout: LinearLayout
    private lateinit var internetCheckHandler: Handler
    private lateinit var internetCheckRunnable: Runnable
    private var hasLoadedData = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        firebaseAuth = FirebaseAuth.getInstance()
        setupWindowInsets()
        profileImg = findViewById(R.id.profileImg)
        loading = findViewById(R.id.progressBar)
        loadProfileImage()
        history = findViewById(R.id.button3)
        layout = findViewById(R.id.linearLayout3)
        val currentUser = firebaseAuth.currentUser
        val username = getUserSession()
        val email = currentUser?.email
        favourite = findViewById(R.id.button2)
        favourite.setOnClickListener {
            val intent = Intent(this, FavouriteActivity::class.java)
            startActivity(intent)
        }
        history.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
        if (username != null) {
            loadUserData()
        } else {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<ImageView>(R.id.backImage).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        findViewById<Button>(R.id.settingsBtn).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                putExtra("EXTRA_USERNAME", username)
                putExtra("EXTRA_EMAIL", email)
            }
            startActivity(intent)
        }
        findViewById<Button>(R.id.logOutBtn).setOnClickListener { logout() }
        internetCheckHandler = Handler(Looper.getMainLooper())
        internetCheckRunnable = object : Runnable {
            override fun run() {
                if (!isInternetAvailable()) {
                    Toast.makeText(
                        this@AccountActivity,
                        "Нет интернет-соединения",
                        Toast.LENGTH_SHORT
                    ).show()
                    hasLoadedData = false
                } else {
                    if (!hasLoadedData) {
                        loadUserData()
                        hasLoadedData = true
                    }
                }
                internetCheckHandler.postDelayed(this, 1000)
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork?.let { network ->
                connectivityManager.getNetworkCapabilities(network)
            }
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            activeNetworkInfo?.isConnected == true
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadProfileImage() {
        Glide.with(this)
            .load(R.drawable.ic_profile)
            .circleCrop()
            .into(profileImg)
    }

    private fun loadUserData() {
        val username = getUserSession()
        if (username != null) {
            loading.visibility = View.VISIBLE
            val database = FirebaseDatabase.getInstance()
            val userRef = database.reference.child("users").child(username)

            userRef.get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val email = dataSnapshot.child("email").getValue(String::class.java)
                    val usernameLoaded = dataSnapshot.child("username").getValue(String::class.java)

                    findViewById<TextView>(R.id.textView21).text =
                        usernameLoaded ?: "Имя не найдено"
                    findViewById<TextView>(R.id.textViewEmail).text = email ?: "Email не найден"
                    loading.visibility = View.GONE
                    layout.visibility = View.VISIBLE
                } else {
                    Toast.makeText(
                        this,
                        "Данные пользователя не найдены в базе данных",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener { error ->
                Toast.makeText(this, "Ошибка загрузки данных: ${error.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(this, "Имя пользователя не найдено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logout() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        dialogView.findViewById<Button>(R.id.button_confirm).setOnClickListener {
            firebaseAuth.signOut()
            clearUserSession()
            Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, IntroActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        dialogView.findViewById<Button>(R.id.button_cancel).setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private val updateUserDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "UPDATE_USER_DATA") {
                val newUsername = intent.getStringExtra("newUsername")
                if (newUsername != null) {
                    findViewById<TextView>(R.id.textView21).text = newUsername

                    val sharedPreferences =
                        context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putString("username", newUsername).apply()

                    loadUserData()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("UPDATE_USER_DATA")
        LocalBroadcastManager.getInstance(this).registerReceiver(updateUserDataReceiver, filter)
        if (!isInternetAvailable()) {
            internetCheckHandler.post(internetCheckRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        val filter = IntentFilter("UPDATE_USER_DATA")
        LocalBroadcastManager.getInstance(this).registerReceiver(updateUserDataReceiver, filter)
        if (!isInternetAvailable()) {
            internetCheckHandler.post(internetCheckRunnable)
        }
    }

    private fun getUserSession(): String? {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPreferences.getString("username", null)
    }

    private fun clearUserSession() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}