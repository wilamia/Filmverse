package com.example.filmverse.Activities

import Movie
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.filmverse.Adapters.CategoryListAdapter
import com.example.filmverse.Adapters.FilmListAdapter
import com.example.filmverse.Adapters.SliderAdapters
import com.example.filmverse.Domian.GenreResponse
import com.example.filmverse.Domian.GenreSolo
import com.example.filmverse.Domian.SliderItems
import com.example.filmverse.R
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileNotFoundException
import okio.IOException
import org.jsoup.Jsoup


@Suppress("UNREACHABLE_CODE")
class MainActivity : AppCompatActivity() {
    private lateinit var adapterBestMovies: FilmListAdapter
    private lateinit var adapterBestSerials: FilmListAdapter
    private lateinit var adapterUpcoming: FilmListAdapter
    private lateinit var adapterCategory: CategoryListAdapter
    private lateinit var adapterPopular: FilmListAdapter
    private lateinit var loading1: ProgressBar
    private lateinit var loading2: ProgressBar
    private lateinit var loading3: ProgressBar
    private lateinit var loading4: ProgressBar
    private lateinit var loading5: ProgressBar
    private lateinit var loading6: ProgressBar
    private lateinit var recyclerViewBestMovies: RecyclerView
    private lateinit var recyclerViewBestSerials: RecyclerView
    private lateinit var recyclerViewPopular: RecyclerView
    private lateinit var recyclerViewUpcoming: RecyclerView
    private lateinit var recyclerViewCategory: RecyclerView
    private lateinit var viewPager2: ViewPager2
    private lateinit var auth: FirebaseAuth
    private lateinit var slideHandler: Handler
    private lateinit var internetCheckHandler: Handler
    private lateinit var internetCheckRunnable: Runnable
    private var hasLoadedData = false

    @SuppressLint("WrongViewCast", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.movieCountry)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val buttonProfile: ImageView = findViewById(R.id.imageView4)
        buttonProfile.setOnClickListener {
            val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)
            finish()
        }

        val favBtn: ImageView = findViewById(R.id.imageView3)
        favBtn.setOnClickListener {
            val intent2 = Intent(this, FavouriteActivity::class.java)
            startActivity(intent2)
        }

        val searchFilter: ImageView = findViewById(R.id.imageView5)
        searchFilter.setOnClickListener {
            val intent2 = Intent(this, SearchFilterActivity::class.java)
            startActivity(intent2)
            finish()
        }

        val editTextSearch = findViewById<EditText>(R.id.editTextText2)
        editTextSearch.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                if (event.rawX >= editTextSearch.right - editTextSearch.compoundDrawables[drawableEnd].bounds.width()) {

                    val query = editTextSearch.text.toString()
                    if (query.isNotEmpty()) {
                        val intent = Intent(this, SearchActivity::class.java)
                        intent.putExtra("SEARCH_QUERY", query)
                        startActivity(intent)
                        finish()
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }
        editTextSearch.setOnEditorActionListener { v, actionId, event ->
            if (event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_SEARCH)) {
                val query = editTextSearch.text.toString()
                if (query.isNotEmpty()) {
                    val intent = Intent(this, SearchActivity::class.java)
                    intent.putExtra("SEARCH_QUERY", query)
                    startActivity(intent)
                    finish()
                }
                true
            } else {
                false
            }
        }
        auth = FirebaseAuth.getInstance()
        if (!isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        internetCheckHandler = Handler(Looper.getMainLooper())
        internetCheckRunnable = object : Runnable {
            override fun run() {
                if (!isInternetAvailable()) {
                    Toast.makeText(this@MainActivity, "Нет интернет-соединения", Toast.LENGTH_SHORT)
                        .show()
                    hasLoadedData = false
                } else {
                    if (!hasLoadedData) {
                        loadData()
                        hasLoadedData = true
                    }
                }
                internetCheckHandler.postDelayed(this, 1000)
            }
        }
        initView()
        slideHandler = Handler(Looper.getMainLooper())
        loadData()
    }

    private fun loadData() {
        if (!isInternetAvailable()) {
            Toast.makeText(this, "Нет интернет-соединения", Toast.LENGTH_LONG).show()
            return
        } else {
            loadFilms()
            sendRequestPopular()
            sendRequestCategory()
            sendRequestBestMovies()
            sendRequestBestSeries()
            sendRequestUpcoming()
        }
    }

    @SuppressLint("ServiceCast")
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

    private fun sendRequestUpcoming() {
        loading3.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val movies = mutableListOf<Movie>()
                for (page in 1..10) {
                    val url = "https://kinogo.bot/skoro-v-kino/page/$page/"
                    val document = Jsoup.connect(url).get()
                    val movieElements = document.select("div.kino-card")

                    for (element in movieElements) {
                        val title = element.select("h2.kino-card-title").text().trim()
                        val cleanedTitle = if (title.endsWith(")")) {
                            title.substringBeforeLast(" (")
                        } else {
                            title
                        }
                        val posterPath =
                            element.select("img").attr("data-src").takeIf { it.isNotEmpty() }
                                ?: element.select("img").attr("src")
                        val posterUrl = "https://kinogo.bot$posterPath"
                        val id = element.select("a").attr("href").substringAfterLast("/")
                            .substringBefore(".html")

                        val movie = Movie(
                            title = cleanedTitle,
                            posterUrl = posterUrl,
                            id = id
                        )
                        movies.add(movie)
                    }
                }
                withContext(Dispatchers.Main) {
                    loading3.visibility = View.GONE
                    adapterUpcoming = FilmListAdapter(movies)
                    recyclerViewUpcoming.adapter = adapterUpcoming
                }

            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    loading3.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading3.visibility = View.GONE
                }
            }
        }
    }

    private fun sendRequestPopular() {
        loading5.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val movies = mutableListOf<Movie>()
                for (page in 1..10) {
                    val url = "https://kinogo.bot/top-3/page/$page/"
                    val document = Jsoup.connect(url).get()
                    val movieElements = document.select("div.kino-card")

                    for (element in movieElements) {
                        val title = element.select("h2.kino-card-title").text().trim()
                        val cleanedTitle = if (title.endsWith(")")) {
                            title.substringBeforeLast(" (")
                        } else {
                            title
                        }
                        val posterPath =
                            element.select("img").attr("data-src").takeIf { it.isNotEmpty() }
                                ?: element.select("img").attr("src")
                        val posterUrl = "https://kinogo.bot$posterPath"
                        val id = element.select("a").attr("href").substringAfterLast("/")
                            .substringBefore(".html")
                        val movie = Movie(
                            title = cleanedTitle,
                            posterUrl = posterUrl,
                            id = id
                        )
                        movies.add(movie)
                    }
                }

                withContext(Dispatchers.Main) {
                    loading5.visibility = View.GONE
                    adapterPopular = FilmListAdapter(movies)
                    recyclerViewPopular.adapter = adapterPopular
                }

            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    loading5.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading5.visibility = View.GONE
                }
            }
        }
    }

    private fun sendRequestBestMovies() {
        loading2.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val movies = mutableListOf<Movie>()

                for (page in 1..10) {
                    val url = "https://kinogo.bot/luchshie-filmy/page/$page/"
                    val document = Jsoup.connect(url).get()

                    val movieElements = document.select("div.kino-card")

                    for (element in movieElements) {
                        val title = element.select("h2.kino-card-title").text().trim()
                        val cleanedTitle = if (title.endsWith(")")) {
                            title.substringBeforeLast(" (")
                        } else {
                            title
                        }
                        val posterPath =
                            element.select("img").attr("data-src").takeIf { it.isNotEmpty() }
                                ?: element.select("img").attr("src")
                        val posterUrl = "https://kinogo.bot$posterPath"
                        val id = element.select("a").attr("href").substringAfterLast("/")
                            .substringBefore(".html")

                        val movie = Movie(
                            title = cleanedTitle,
                            posterUrl = posterUrl,
                            id = id
                        )
                        movies.add(movie)
                    }
                }

                withContext(Dispatchers.Main) {
                    loading2.visibility = View.GONE
                    adapterBestMovies = FilmListAdapter(movies)
                    recyclerViewBestMovies.adapter = adapterBestMovies
                }

            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    loading2.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading2.visibility = View.GONE
                }
            }
        }
    }

    private fun sendRequestBestSeries() {
        loading4.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val series = mutableListOf<Movie>()

                for (page in 1..10) {
                    val url =
                        "https://kinogo.bot/f/s.trailers=0/x.type=series/sort=rating;desc/page/$page/"
                    val document = Jsoup.connect(url).get()

                    val seriesElements = document.select("div.kino-card")

                    for (element in seriesElements) {
                        val title = element.select("h2.kino-card-title").text().trim()
                        val cleanedTitle = if (title.endsWith(")")) {
                            title.substringBeforeLast(" (")
                        } else {
                            title
                        }
                        val posterPath =
                            element.select("img").attr("data-src").takeIf { it.isNotEmpty() }
                                ?: element.select("img").attr("src")
                        val posterUrl = "https://kinogo.bot$posterPath"

                        val id = element.select("a").attr("href").substringAfterLast("/")
                            .substringBefore(".html")

                        val seriesItem = Movie(
                            title = cleanedTitle,
                            posterUrl = posterUrl,
                            id = id
                        )
                        series.add(seriesItem)
                    }
                }

                withContext(Dispatchers.Main) {
                    loading4.visibility = View.GONE
                    adapterBestSerials = FilmListAdapter(series)
                    recyclerViewBestSerials.adapter = adapterBestSerials
                }

            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    loading4.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading4.visibility = View.GONE
                }
            }
        }
    }

    private fun sendRequestCategory() {
        loading1.visibility = View.VISIBLE
        try {

            val inputStream = assets.open("responseGenre.json")
            val json = inputStream.bufferedReader().use { it.readText() }

            val gson = Gson()
            val genreResponse: GenreResponse = gson.fromJson(json, GenreResponse::class.java)
            val items: ArrayList<GenreSolo> = ArrayList(genreResponse.genres)

            loading1.visibility = View.GONE

            adapterCategory = CategoryListAdapter(items)
            recyclerViewCategory.adapter = adapterCategory

        } catch (e: FileNotFoundException) {
            loading1.visibility = View.GONE
        } catch (e: IOException) {
            loading1.visibility = View.GONE
        } catch (e: JsonSyntaxException) {
            loading1.visibility = View.GONE
        } catch (e: Exception) {
            loading1.visibility = View.GONE
        }
    }

    private fun loadFilms() {
        loading1.visibility = View.VISIBLE

        val url = "https://kinogo.bot/f/s.trailers=0/x.type=en/sort=date;desc/"

        Thread {
            try {
                val document = Jsoup.connect(url).get()

                val films = document.select("div.kino-card")
                val filmList = films.map { element ->
                    val title = element.select("h2.kino-card-title").text().trim()
                    val cleanedTitle = if (title.endsWith(")")) {
                        title.substringBeforeLast(" (")
                    } else {
                        title
                    }
                    val posterPath =
                        element.select("img").attr("data-src").takeIf { it.isNotEmpty() }
                            ?: element.select("img").attr("src")
                    val posterUrl = "https://kinogo.bot$posterPath"

                    val id = element.select("a").attr("href").substringAfterLast("/")
                        .substringBefore(".html")
                    Movie(cleanedTitle, posterUrl, id)
                }

                runOnUiThread {
                    loading1.visibility = View.GONE
                    banners(filmList)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loading1.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun banners(films: List<Movie>) {
        val sliderItems = films.map { film ->
            SliderItems(film.posterUrl)
        }

        if (sliderItems.isEmpty()) return

        viewPager2.adapter = SliderAdapters(sliderItems, films, this)

        viewPager2.clipToPadding = false
        viewPager2.clipChildren = false
        viewPager2.offscreenPageLimit = 3
        viewPager2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS

        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(40))
        compositePageTransformer.addTransformer(object : ViewPager2.PageTransformer {
            override fun transformPage(page: View, position: Float) {
                val r = 1 - Math.abs(position)
                page.scaleY = 0.85f + r * 0.15f
                page.scaleX = 0.85f + r * 0.15f
            }
        })
        viewPager2.setPageTransformer(compositePageTransformer)

        viewPager2.setCurrentItem(0, false)
        loading6.visibility = View.GONE

        startAutoScroll()
    }

    private fun startAutoScroll() {
        slideHandler = Handler(Looper.getMainLooper())
        slideHandler.postDelayed(object : Runnable {
            override fun run() {
                val nextItem = (viewPager2.currentItem + 1) % (viewPager2.adapter?.itemCount ?: 1)
                viewPager2.setCurrentItem(nextItem, true)
                slideHandler.postDelayed(this, 5000)
            }
        }, 5000)
    }


    override fun onResume() {
        super.onResume()
        startAutoScroll()
        if (!isInternetAvailable()) {
            internetCheckHandler.post(internetCheckRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        slideHandler.removeCallbacksAndMessages(null)
        if (!isInternetAvailable()) {
            internetCheckHandler.post(internetCheckRunnable)
        }
    }

    private fun initView() {
        viewPager2 = findViewById(R.id.viewpagerSlider)

        recyclerViewBestMovies = findViewById(R.id.view1)
        recyclerViewBestMovies.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewPopular = findViewById(R.id.view7)
        recyclerViewPopular.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewBestSerials = findViewById(R.id.view6)
        recyclerViewBestSerials.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewUpcoming = findViewById(R.id.view3)
        recyclerViewUpcoming.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewCategory = findViewById(R.id.view2)
        recyclerViewCategory.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        loading1 = findViewById(R.id.progressBar)
        loading2 = findViewById(R.id.progressBar1)
        loading3 = findViewById(R.id.progressBar3)
        loading4 = findViewById(R.id.progressBar6)
        loading5 = findViewById(R.id.progressBar7)
        loading6 = findViewById(R.id.progressBar9)
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        return sharedPreferences.getString("username", null) != null
    }
}