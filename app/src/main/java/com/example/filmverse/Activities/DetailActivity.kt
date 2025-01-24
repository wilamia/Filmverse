package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.filmverse.Adapters.CategoryEachFilmListAdapter
import com.example.filmverse.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.Calendar
import java.util.regex.Pattern


class DetailActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var titleTxt: TextView
    private lateinit var movieRateTxt: TextView
    private lateinit var movieYearCountry: TextView
    private lateinit var movieTimeTxt: TextView
    private lateinit var movieSummaryTxt: TextView
    private lateinit var movieActorsTxt: TextView
    private lateinit var movieDirectorTxt: TextView
    private lateinit var pic2: ImageView
    private lateinit var backImg: ImageView
    private lateinit var recycleViewCategory: RecyclerView
    private lateinit var recycleViewCountry: RecyclerView
    private lateinit var scrollView: NestedScrollView
    private var moviePageUrl: String? = null
    private var playerUrl: String? = null
    private lateinit var idFilm: String
    private lateinit var butVideo: Button
    private lateinit var favourite: ImageView
    private lateinit var videoView: WebView
    private var currentColor: Int? = null
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR) + 1
    private var trailerUrl: String? = null

    private fun setColorFilter(color: Int) {
        currentColor = color
        favourite.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        setContentView(R.layout.activity_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.movieCountry)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()

        butVideo = findViewById(R.id.watchVideo)
        idFilm = intent.getStringExtra("id") ?: ""
        moviePageUrl = "https://kinogo.bot/$idFilm"
        favourite = findViewById(R.id.imageView7)

        sendRequest(idFilm)

        val usernameFromIntent = getUserSession().toString()
        val movieTitle = titleTxt.text.toString()
        val movieId = idFilm

        checkMovieInFavorites(usernameFromIntent, movieId, favourite)

        favourite.setOnClickListener {
            if (movieTitle.isNotEmpty()) {
                checkMovie(usernameFromIntent, movieId) { isInFavorites ->
                    if (isInFavorites) {
                        removeMovieFromFavorites(usernameFromIntent, movieId)
                        setColorFilter(Color.WHITE)
                        currentColor = Color.WHITE
                    } else {
                        saveMovieToFavorites(usernameFromIntent, movieId)
                        setColorFilter(Color.RED)
                        currentColor = Color.RED
                    }
                }
            }
        }

        videoView.addJavascriptInterface(object {
            @JavascriptInterface
            fun enterFullScreen() {
                runOnUiThread {
                    openFullScreenVideo()
                }
            }
        }, "Android")

        findViewById<Button>(R.id.watchVideo).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                saveMovieToHistory(usernameFromIntent, idFilm)
                if (playerUrl != null) {
                    openVideoPlayer(playerUrl!!)
                } else {
                    Toast.makeText(this@DetailActivity, "Фильм не найден.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    }

    private fun checkMovieInFavorites(userId: String, movieId: String, heartImageView: ImageView) {

        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId).child("movies").child(movieId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    heartImageView.setColorFilter(Color.RED)
                } else {
                    heartImageView.clearColorFilter()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@DetailActivity,
                    "Ошибка при проверке фильма: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun checkMovie(userId: String, movieId: String, callback: (Boolean) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId).child("movies").child(movieId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                callback(dataSnapshot.exists())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@DetailActivity,
                    "Ошибка при проверке фильма: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
                callback(false)
            }
        })
    }

    private fun getUserSession(): String? {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPreferences.getString("username", null)
    }

    private fun removeMovieFromFavorites(userId: String, movieId: String) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId)

        userRef.child("movies").child(movieId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Фильм удален из избранного", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при удалении фильма: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun saveMovieToFavorites(userId: String, movieId: String) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId)

        userRef.child("movies").child(movieId).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(this, "Фильм сохранен в избранное", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Ошибка при сохранении фильма: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveMovieToHistory(userId: String, movieId: String) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId).child("history")

        val watchedDate = System.currentTimeMillis().toString()

        userRef.child(movieId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    userRef.child(movieId).removeValue()
                }

                userRef.child(movieId).setValue(watchedDate)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this@DetailActivity,
                            "Фильм добавлен в историю просмотра",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this@DetailActivity,
                            "Ошибка при сохранении фильма: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@DetailActivity,
                    "Ошибка при проверке истории просмотров: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    @SuppressLint("SetTextI18n")
    private fun sendRequest(idFilm: String) {
        progressBar.visibility = View.VISIBLE
        scrollView.visibility = View.GONE

        val url = "https://kinogo.bot/$idFilm.html"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = Jsoup.connect(url).get()
                val title = document.select("h1.main-title").text()
                val cleanedTitle = if (title.endsWith(")")) {
                    title.substringBeforeLast(" (")
                } else {
                    title
                }

                val genres = document.select("div.info-element-content.genre a")
                    .map { it.text() }

                val ratings = document.select("div.services-ratings .rating-trigger")
                val cleanText = { text: String? ->
                    text?.replace(Regex("КП: |IMDb: "), "")?.trim()
                }

                val ratingKp = cleanText(ratings.getOrNull(0)?.text())
                val ratingImdb = cleanText(ratings.getOrNull(1)?.text())

                val year = document.select("div.right-info-element.year-spacer .datecreated").text()
                if (year.contains(currentYear.toString())) {
                    butVideo.visibility = View.GONE
                }
                val countries =
                    document.select("div.right-info-element .info-element-content.country")
                        .map { it.text().trim(',') }

                val actorsElement = document.select("div.right-info-element.actors").firstOrNull()
                val actorsText = actorsElement?.ownText()?.trim()
                val actorsList =
                    (actorsText?.split(", ")?.joinToString(", ") { it.trim() } ?: "") + " другие"

                val directorElements =
                    document.select("div.right-info-element #director-value .zamanushka-actors")
                val directorsList = directorElements.joinToString(", ") { it.text().trim() }

                val isFilm = document.select("div.info-element-name:contains(Сериал)").isEmpty()
                val durationOrSeason = if (isFilm) {
                    val duration = document.select("div.right-info-element.duration").text()
                    duration.substringBeforeLast(" ", "").takeIf { it.isNotEmpty() }
                } else {
                    val seasonInfo =
                        document.select("div.kino-card-quality-badge.top-poster-text").attr("title")
                    seasonInfo.split(" ").take(2).joinToString(" ").takeIf { it.isNotEmpty() }
                }

                movieTimeTxt.visibility =
                    if (durationOrSeason.isNullOrEmpty()) View.GONE else View.VISIBLE

                val posterRelativeUrl = document.select("img.full-left-poster").attr("src")
                val posterUrl = "https://kinogo.bot$posterRelativeUrl"

                val description = document.select("span#data-text").text()

                extractTrailerUrlFromScript(url)

                withContext(Dispatchers.Main) {
                    titleTxt.text = cleanedTitle
                    movieSummaryTxt.text = description
                    movieRateTxt.visibility = when {
                        ratingImdb != null -> {
                            movieRateTxt.text = "IMDb: $ratingImdb"
                            View.VISIBLE
                        }

                        ratingKp != null -> {
                            movieRateTxt.text = "КП: $ratingKp"
                            View.VISIBLE
                        }

                        else -> View.GONE
                    }
                    movieYearCountry.text = year
                    movieActorsTxt.text = actorsList
                    movieDirectorTxt.text = directorsList
                    movieTimeTxt.text = durationOrSeason

                    val genreAdapter = CategoryEachFilmListAdapter(genres)
                    recycleViewCategory.adapter = genreAdapter
                    recycleViewCategory.layoutManager = LinearLayoutManager(
                        this@DetailActivity,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )

                    val countryAdapter = CategoryEachFilmListAdapter(countries)
                    recycleViewCountry.adapter = countryAdapter
                    recycleViewCountry.layoutManager = LinearLayoutManager(
                        this@DetailActivity,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )

                    Glide.with(this@DetailActivity)
                        .load(posterUrl)
                        .into(pic2)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    if (checkFilmLink(moviePageUrl!!) && !movieYearCountry.text.contains(currentYear.toString())) {
                        butVideo.visibility = View.VISIBLE
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            scrollView.visibility = View.VISIBLE
                        }
                    } else {
                        butVideo.visibility = View.GONE
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            scrollView.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    scrollView.visibility = View.VISIBLE
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun extractTrailerUrlFromScript(url: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val document = Jsoup.connect(url).get()
                val scripts = document.select("script")
                val urlPattern = Pattern.compile("https://youtu\\.be/([\\w-]+)")
                var trailerFound = false
                for (script in scripts) {
                    val scriptContent = script.data()
                    val matcher = urlPattern.matcher(scriptContent)
                    if (matcher.find()) {
                        trailerFound = true
                        val videoId = matcher.group(1)
                        trailerUrl = "https://www.youtube.com/embed/$videoId?autoplay=0"
                        withContext(Dispatchers.Main) {
                            videoView.settings.javaScriptEnabled = true
                            videoView.settings.domStorageEnabled = true
                            videoView.webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.loadUrl(
                                        "javascript:(function() { " +
                                                "var fullScreenButton = document.querySelector('.ytp-fullscreen-button');" +
                                                "if (fullScreenButton) {" +
                                                "fullScreenButton.addEventListener('click', function() {" +
                                                "    Android.enterFullScreen();" +
                                                "});" +
                                                "}" +
                                                "})()"
                                    )
                                }
                            }

                            videoView.loadUrl(trailerUrl!!)
                        }
                        break
                    }
                }
                withContext(Dispatchers.Main) {
                    if (!trailerFound) {
                        videoView.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                videoView.visibility = View.GONE
            }
        }
    }

    private fun openFullScreenVideo() {
        val intent = Intent(this, VideoPlayerTrailerActivity::class.java).apply {
            putExtra("VIDEO_URL", trailerUrl)
        }
        startActivity(intent)
    }

    override fun onBackPressed() {
        if (videoView.canGoBack()) {
            videoView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun initView() {
        titleTxt = findViewById(R.id.movieNameTxt)
        progressBar = findViewById(R.id.progressBarDetail)
        scrollView = findViewById(R.id.scrollView2)
        pic2 = findViewById(R.id.picDetail)
        movieRateTxt = findViewById(R.id.movieStar)
        movieTimeTxt = findViewById(R.id.movieTime)
        movieYearCountry = findViewById(R.id.movieYear)
        movieSummaryTxt = findViewById(R.id.movieSummary)
        movieActorsTxt = findViewById(R.id.movieActorsTxt)
        movieDirectorTxt = findViewById(R.id.movieDirectorTxt)
        backImg = findViewById(R.id.backImage)
        recycleViewCategory = findViewById(R.id.genreView)
        recycleViewCountry = findViewById(R.id.countryRecycler)

        videoView = findViewById(R.id.webView2)

        recycleViewCategory.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recycleViewCountry.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        backImg.setOnClickListener { finish() }
    }

    private fun openVideoPlayer(url: String) {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("VIDEO_URL", url)
        startActivity(intent)
    }

    private suspend fun checkFilmLink(idFilm: String): Boolean {
        val url = "$idFilm.html"

        return withContext(Dispatchers.IO) {
            try {
                val document = Jsoup.connect(url).get()

                val button = document.select("button.tab-button[data-url]").firstOrNull()

                val playerUrlExists = button != null
                if (playerUrlExists) {
                    if (button != null) {
                        playerUrl = "https:" + button.attr("data-url")
                    }
                }


                playerUrlExists
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
