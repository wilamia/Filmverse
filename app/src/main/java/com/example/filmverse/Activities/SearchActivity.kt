package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filmverse.Adapters.SearchListAdapter
import com.example.filmverse.Domain.MovieFavourite
import com.example.filmverse.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

@SuppressLint("NotifyDataSetChanged")
class SearchActivity : AppCompatActivity() {
    private lateinit var adapterFilms: SearchListAdapter
    private lateinit var recyclerViewFilms: RecyclerView
    private lateinit var loading: View
    private lateinit var noResultsText: TextView
    private lateinit var backBtn: ImageView
    private val series = mutableListOf<MovieFavourite>()
    private var currentPage = 1
    private var isLoading = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        overridePendingTransition(0, 0)
        setContentView(R.layout.activity_search)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val movieName = intent.getStringExtra("SEARCH_QUERY") ?: ""
        val search = findViewById<EditText>(R.id.editTextText3)
        backBtn = findViewById(R.id.backImage3)
        noResultsText = findViewById(R.id.noResultsText)
        loading = findViewById(R.id.progressBar5)
        recyclerViewFilms = findViewById(R.id.recyclerView2)
        recyclerViewFilms.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapterFilms = SearchListAdapter(this, series)
        recyclerViewFilms.adapter = adapterFilms
        recyclerViewFilms.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.bottom = 1
            }
        })

        if (movieName != "") {
            search.setText(movieName)
            searchFilm(movieName)
        }

        backBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        search.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                if (event.rawX >= search.right - search.compoundDrawables[drawableEnd].bounds.width()) {
                    performSearch()
                    return@setOnTouchListener true
                }
            }
            false
        }

        search.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        noResultsText = findViewById(R.id.noResultsText)
        val search = findViewById<EditText>(R.id.editTextText3)
        val query = search.text.toString()
        if (query.isNotEmpty()) {
            noResultsText.visibility = View.GONE
            series.clear()
            adapterFilms.notifyDataSetChanged()
            searchFilm(query)
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(search.windowToken, 0)
        }
    }

    private fun searchFilm(query: String, page: Int = 1) {
        val movieUrl = "https://kinogo.bot/search/$query/page/$page/"
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = Jsoup.connect(movieUrl).get()
                val noResultsMessage = document.select("div.box.berrors").text()
                if (noResultsMessage.contains(
                        "Новостей по данному запросу не найдено",
                        ignoreCase = true
                    )
                ) {
                    withContext(Dispatchers.Main) {
                        noResultsText.visibility = View.VISIBLE
                        loading.visibility = View.GONE
                    }
                    return@launch
                }
                val movieElements = document.select("div.kino-card")
                for (element in movieElements) {
                    val yearElement = element.select("span.card-title-year").first()
                    val ratingKpElement = element.select("span.kino-poster-kp-rting").first()
                    val ratingImdbElement = element.select("span.kino-poster-imdb-rting").first()
                    val descriptionElement = element.select("div.kino-info-text").first()
                    val title = element.select("h2.kino-card-title").text().trim()
                    val cleanedTitle = if (title.endsWith(")")) {
                        title.substringBeforeLast(" (")
                    } else {
                        title
                    }
                    val year =
                        yearElement?.text()?.trim()?.removeSurrounding("(", ")") ?: "Unknown Year"
                    val posterPath =
                        element.select("img").attr("data-src").takeIf { it.isNotEmpty() }
                            ?: element.select("img").attr("src")
                    val posterUrl = "https://kinogo.bot$posterPath"
                    val ratingKp = ratingKpElement?.attr("data-title")?.replace("KP: ", "") ?: "N/A"
                    val ratingImdb =
                        ratingImdbElement?.attr("data-title")?.replace("IMDb: ", "") ?: "N/A"
                    val description =
                        descriptionElement?.text()?.trim() ?: "No Description Available"
                    val id = element.select("a").attr("href").substringAfterLast("/")
                        .substringBefore(".html")
                    val seriesItem = MovieFavourite(
                        title = cleanedTitle,
                        posterUrl = posterUrl,
                        id = id,
                        year = year,
                        ratingImdb = ratingImdb,
                        ratingKp = ratingKp,
                        description = description
                    )

                    series.add(seriesItem)
                }

                val nextPageElement = document.select("a.next-page").first()
                if (nextPageElement != null) {
                    val nextPageUrl = nextPageElement.attr("href")
                    val nextPageNumber =
                        nextPageUrl.substringAfterLast("/").toIntOrNull() ?: (page + 1)
                    searchFilm(query, nextPageNumber)
                }

                withContext(Dispatchers.Main) {
                    adapterFilms.notifyDataSetChanged()
                    loading.visibility = View.GONE
                    isLoading = false
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                    isLoading = false
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}