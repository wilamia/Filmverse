package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filmverse.Adapters.HistoryListAdapter
import com.example.filmverse.Domain.MovieHistory
import com.example.filmverse.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.util.Date

class HistoryActivity : AppCompatActivity() {
    private lateinit var loading: ProgressBar
    private lateinit var adapterFilms: HistoryListAdapter
    private lateinit var recyclerViewFilms: RecyclerView
    private val history = mutableListOf<MovieHistory>()
    private lateinit var backBtn: ImageView
    private lateinit var noHistoryTextView: TextView
    private var isDataLoaded = true
    private lateinit var internetCheckHandler: Handler
    private lateinit var internetCheckRunnable: Runnable
    private var hasLoadedData = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val usernameFromIntent = getUserSession().toString()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        noHistoryTextView = findViewById(R.id.noHistoryTextView)
        backBtn = findViewById(R.id.backImage2)
        backBtn.setOnClickListener {
            finish()
        }
        loading = findViewById(R.id.progressBar2)
        recyclerViewFilms = findViewById(R.id.recyclerView)
        recyclerViewFilms.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapterFilms = HistoryListAdapter(this, history)
        recyclerViewFilms.adapter = adapterFilms
        recyclerViewFilms.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.bottom = 24
            }
        })
        adapterFilms.setOnItemLongClickListener { movieHistory ->
            showDeleteConfirmationDialog(movieHistory)
        }
        internetCheckHandler = Handler(Looper.getMainLooper())
        internetCheckRunnable = object : Runnable {
            override fun run() {
                if (!isInternetAvailable()) {
                    Toast.makeText(
                        this@HistoryActivity,
                        "Нет интернет-соединения",
                        Toast.LENGTH_SHORT
                    ).show()
                    hasLoadedData = false
                } else {
                    if (!hasLoadedData) {
                        loadHistoryMovies(usernameFromIntent)
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

    private fun getUserSession(): String? {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPreferences.getString("username", null)
    }

    private fun showDeleteConfirmationDialog(movieHistory: MovieHistory) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom_delete)
        dialog.setCancelable(true)
        val buttonCancel: Button = dialog.findViewById(R.id.button_cancel)
        val buttonConfirm: Button = dialog.findViewById(R.id.button_confirm)
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        buttonConfirm.setOnClickListener {
            deleteMovieFromHistory(movieHistory)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun deleteMovieFromHistory(movieHistory: MovieHistory) {
        val database = FirebaseDatabase.getInstance()
        val userId = getUserSession().toString()
        val userRef =
            database.getReference("users").child(userId).child("history").child(movieHistory.id)
        userRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                history.remove(movieHistory)
                adapterFilms.notifyDataSetChanged()
                Toast.makeText(this, "Фильм удален из истории", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ошибка при удалении фильма", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadHistoryMovies(userId: String) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId).child("history")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val historyMovies = mutableListOf<Pair<String, String>>()
                for (movieSnapshot in dataSnapshot.children) {
                    val movieId = movieSnapshot.key
                    val uniqueId = movieSnapshot.value as? String
                    if (movieId != null && uniqueId != null) {
                        historyMovies.add(movieId.trim() to uniqueId.trim())
                    }
                }
                history.removeAll { oldMovie ->
                    val matchFound = historyMovies.any { (newMovieId, _) ->
                        oldMovie.id == newMovieId
                    }
                    if (matchFound) {
                        Log.d("HistoryActivityL", "Удаляем: ${oldMovie.id}")
                    }
                    matchFound
                }
                historyMovies.sortByDescending { it.second.toLongOrNull() }

                if (historyMovies.isNotEmpty()) {
                    for ((movieId, uniqueId) in historyMovies) {
                        loadMovieData(movieId, uniqueId)
                    }
                } else {
                    showNoHistoryMessage()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@HistoryActivity,
                    "Ошибка при загрузке истории просмотров: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun convertTimestampToDate(timestamp: String): Date? {
        return try {
            val timeInMillis = timestamp.toLong()
            Date(timeInMillis)
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun loadMovieData(movieId: String, uniqueId: String) {
        val movieUrl = "https://kinogo.bot/$movieId.html"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = Jsoup.connect(movieUrl).get()
                val title = document.select("h1.main-title").text()
                val cleanedTitle = if (title.endsWith(")")) {
                    title.substringBeforeLast(" (")
                } else {
                    title
                }
                val posterRelativeUrl = document.select("img.full-left-poster").attr("src")
                val posterUrl = "https://kinogo.bot$posterRelativeUrl"
                val year = document.select("div.right-info-element.year-spacer .datecreated").text()
                val id = movieId
                val ratings = document.select("div.services-ratings .rating-trigger")
                val cleanText = { text: String? ->
                    text?.replace(Regex("КП: |IMDb: "), "")?.trim()
                }
                val ratingKp = cleanText(ratings.getOrNull(0)?.text()?.replace("КП: ", "")) ?: "N/A"
                val ratingImdb =
                    cleanText(ratings.getOrNull(1)?.text()?.replace("IMDb: ", "")) ?: "N/A"
                val viewDate = convertTimestampToDate(uniqueId)
                val movieItem = MovieHistory(
                    title = cleanedTitle,
                    posterUrl = posterUrl,
                    id = id,
                    year = year,
                    ratingImdb = ratingImdb,
                    ratingKp = ratingKp,
                    uniqueId = viewDate
                )
                withContext(Dispatchers.Main) {
                    history.add(movieItem)
                    adapterFilms.notifyItemInserted(history.size - 1)
                    loading.visibility = View.GONE
                }
            } catch (e: IOException) {
                Log.e("HistoryActivity", "Ошибка при получении данных о фильме: ${e.message}", e)
            }
        }
    }

    private fun showNoHistoryMessage() {
        recyclerViewFilms.visibility = View.GONE
        loading.visibility = View.GONE
        noHistoryTextView.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        internetCheckHandler.post(internetCheckRunnable)
        if (!isDataLoaded) {
            val usernameFromIntent = getUserSession().toString()
            loadHistoryMovies(usernameFromIntent)
            isDataLoaded = true
        }
        refreshUI()
    }

    private fun refreshUI() {
        history.clear()
        adapterFilms.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        internetCheckHandler.post(internetCheckRunnable)
        isDataLoaded = false

    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    companion object {
        private fun loadHistoryMovies(historyActivity: HistoryActivity, userId: String) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users").child(userId).child("history")
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val historyMovies = mutableListOf<String>()
                    for (movieSnapshot in dataSnapshot.children) {
                        val movieId = movieSnapshot.key
                        if (movieId != null) {
                            historyMovies.add(movieId.trim())
                        }
                    }
                    historyActivity.history.removeAll { oldMovie ->
                        val matchFound = historyMovies.any { newMovieId ->
                            Log.d("HistoryActivityL", "Сравниваем: ${oldMovie.id} с $newMovieId")
                            oldMovie.id == newMovieId
                        }
                        if (matchFound) {
                            Log.d("HistoryActivityL", "Удаляем: ${oldMovie.id}")
                        }
                        matchFound
                    }
                    historyActivity.adapterFilms.notifyDataSetChanged()

                    if (historyMovies.isNotEmpty()) {
                        for (movieId in historyMovies) {
                            historyActivity.loadMovieData(movieId, "")
                        }
                    } else {
                        historyActivity.showNoHistoryMessage()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(
                        historyActivity,
                        "Ошибка при загрузке истории просмотров: ${databaseError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}