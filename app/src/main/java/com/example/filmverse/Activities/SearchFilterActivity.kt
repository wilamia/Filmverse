package com.example.filmverse.Activities

import Movie
import SpaceItemDecoration
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filmverse.Adapters.FilmListAdapter
import com.example.filmverse.R
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Suppress("NAME_SHADOWING")
class SearchFilterActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var loading: View
    private lateinit var recyclerViewFilmSearch: RecyclerView
    private lateinit var adapterFilmSearch: FilmListAdapter
    private var currentPage = 1
    private var isLoading = false
    private val movies = mutableListOf<Movie>()
    private lateinit var defaultTextView: TextView
    private lateinit var noResultTextView: TextView
    private lateinit var backImg: ImageView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_filter)
        initializeViews()
        setupDrawer()
        setupRecyclerView()
        setupHeader()
        setupGetSelectedButton()
        setupScrollListener()
        backImg.setOnClickListener { navigateBack() }
    }

    private fun initializeViews() {
        defaultTextView = findViewById(R.id.textView27)
        noResultTextView = findViewById(R.id.textView28)
        loading = findViewById(R.id.progressBar8)
        recyclerViewFilmSearch = findViewById(R.id.recyclerView)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        backImg = findViewById(R.id.backImage4)
    }

    private fun setupDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        findViewById<View>(R.id.menuButton).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerViewFilmSearch.layoutManager = GridLayoutManager(this, 2)
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.recycler_view_item_spacing)
        recyclerViewFilmSearch.addItemDecoration(SpaceItemDecoration(spacingInPixels))
    }

    private fun setupHeader() {
        val headerView = layoutInflater.inflate(R.layout.checkbox_item, null)
        navigationView.addView(headerView)
        setupCheckboxHeaders(headerView)
    }

    private fun setupCheckboxHeaders(headerView: View) {
        setupHeader(headerView, R.id.header, R.id.checkbox_container) { setupYearCheckboxes(it) }
        setupHeader(headerView, R.id.header2, R.id.checkbox_container2) { setupGenreCheckboxes(it) }
        setupHeader(headerView, R.id.header3, R.id.checkbox_container3) { setupTypeCheckboxes(it) }
        setupHeader(headerView, R.id.header4, R.id.checkbox_container4) { setupSortCheckboxes(it) }
        setupHeader(
            headerView,
            R.id.header5,
            R.id.checkbox_container5
        ) { setupCountryCheckboxes(it) }
    }

    private fun setupGetSelectedButton() {
        val getSelectedButton = findViewById<Button>(R.id.button)
        getSelectedButton.setOnClickListener {
            clearMovieList()
            currentPage = 1
            sendRequestFilms(currentPage, buildUrl())
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun setupScrollListener() {
        recyclerViewFilmSearch.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!isLoading && isLastItemDisplaying(recyclerView)) {
                    currentPage++
                    sendRequestFilms(currentPage, buildUrl())
                }
            }
        })
    }

    private fun navigateBack() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun clearMovieList() {
        movies.clear()
        if (!::adapterFilmSearch.isInitialized) {
            adapterFilmSearch = FilmListAdapter(movies)
            recyclerViewFilmSearch.adapter = adapterFilmSearch
        } else {
            adapterFilmSearch.notifyDataSetChanged()
        }
        defaultTextView.visibility = View.VISIBLE
    }

    private fun isLastItemDisplaying(recyclerView: RecyclerView): Boolean {
        val layoutManager = recyclerView.layoutManager as GridLayoutManager
        val lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        return lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.adapter?.itemCount?.minus(
            1
        )
    }

    private fun sendRequestFilms(page: Int, url: String) {
        loading.visibility = View.VISIBLE
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var newUrl: String
                if (page == 1) {
                    newUrl = "$url/"
                } else {
                    newUrl = "$url/page/$page/"
                }

                val doc: Document = Jsoup.connect(newUrl).userAgent("Mozilla/5.0").get()
                val noResultsMessage = doc.select("div.box.berrors").text()
                if (noResultsMessage.contains(
                        "Новостей по данному запросу не найдено",
                        ignoreCase = true
                    )
                ) {
                    withContext(Dispatchers.Main) {
                        updateUI()
                    }
                    return@launch
                }

                val movieElements = doc.select("main > :not(.aside-block-content) > div.kino-card")
                if (movieElements.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        clearMovieList()
                        updateUI()
                    }
                    return@launch
                }

                for (element in movieElements) {
                    extractMovieData(element)
                }

                withContext(Dispatchers.Main) {
                    updateUI()
                }
            } catch (e: IOException) {
                handleError(e)
            }
        }
    }


    private fun extractMovieData(element: org.jsoup.nodes.Element) {
        val title = element.select("h2.kino-card-title").text().trim().substringBeforeLast(" (")
        if (title.isNotEmpty()) {
            val posterPath =
                element.select("img").attr("data-src").ifEmpty { element.select("img").attr("src") }
            val posterUrl = "https://kinogo.bot$posterPath"
            val id =
                element.select("a").attr("href").substringAfterLast("/").substringBefore(".html")

            movies.add(Movie(title = title, posterUrl = posterUrl, id = id))
        }
    }

    private suspend fun updateUI() {
        loading.visibility = View.GONE
        if (movies.isEmpty()) {
            defaultTextView.visibility = View.GONE
            noResultTextView.visibility = View.VISIBLE
            recyclerViewFilmSearch.visibility = View.GONE
        } else {
            noResultTextView.visibility = View.GONE
            defaultTextView.visibility = View.GONE
            recyclerViewFilmSearch.visibility = View.VISIBLE
            if (!::adapterFilmSearch.isInitialized) {
                adapterFilmSearch = FilmListAdapter(movies)
                recyclerViewFilmSearch.adapter = adapterFilmSearch
            } else {
                adapterFilmSearch.notifyDataSetChanged()
            }
        }
        isLoading = false
    }

    private suspend fun handleError(e: Exception) {
        withContext(Dispatchers.Main) {
            loading.visibility = View.GONE
            isLoading = false
        }
    }

    private fun buildUrl(): String {
        fun String.encodeURL(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

        val baseUrl = "https://kinogo.bot/f/"
        val urlComponents = mutableListOf<String>()
        val urlParams = mutableMapOf<String, String>()

        val selectedCountries =
            getSelectedCheckboxesFromContainer(R.id.checkbox_container5).joinToString(",") { it.encodeURL() }
        if (selectedCountries.isNotEmpty()) {
            urlParams["countries"] = selectedCountries
        }

        val genres =
            getSelectedCheckboxesFromContainer(R.id.checkbox_container2).map { it.encodeURL() }
        if (genres.isNotEmpty()) {
            urlComponents.add("genres=${genres.joinToString(",")}")
        }

        val years = getSelectedCheckboxesFromContainer(R.id.checkbox_container).joinToString(",")
        if (years.isNotEmpty()) {
            urlComponents.add("s.f_year=$years")
        }
        urlComponents.add("s.trailers=0")

        val type =
            getType(getSelectedCheckboxesFromContainer(R.id.checkbox_container3).joinToString())?.encodeURL()
                ?: ""
        if (type.isNotEmpty()) {
            urlComponents.add("x.type=$type")
        }

        val sortOption =
            getSelectedSortOption(getSelectedCheckboxesFromContainer(R.id.checkbox_container4).joinToString())
                ?: "date;desc"
        urlComponents.add("sort=$sortOption")

        val urlBuilder = StringBuilder(baseUrl)
        urlParams.forEach { (key, value) ->
            urlBuilder.append(if (urlBuilder.length > baseUrl.length) "/" else "")
                .append("$key=$value")
        }

        if (urlComponents.isNotEmpty()) {
            urlBuilder.append(if (urlBuilder.length > baseUrl.length) "/" else "")
                .append(urlComponents.joinToString("/"))
        }

        val completeUrl = urlBuilder.toString()
        return completeUrl
    }

    private fun getSelectedSortOption(selectedType: String): String? {
        return when (selectedType) {
            "По дате" -> "date;desc"
            "По КП" -> "d.rating_kp"
            "По IMDB" -> "d.rating_imdb"
            "По оценкам" -> "rating;desc"
            "По алфавиту" -> "title;asc"
            "По просмотрам" -> "news_read;desc"
            "По комментариям" -> "comm_num;desc"
            "Топ за 3 дня" -> "3days"
            "Топ за неделю" -> "8days"
            else -> null
        }
    }

    private fun getType(selectedType: String): String? {
        return when (selectedType) {
            "Фильм" -> "movie"
            "Сериал" -> "series"
            "Мультфильм" -> "mc"
            "Мультсериал" -> "sc"
            "Дорама (Фильм)" -> "md"
            "Дорама (Сериал)" -> "sd"
            "Аниме (Фильм)" -> "ma"
            "Аниме (Сериал)" -> "sa"
            "Новинки" -> "en"
            "Лучшие фильмы" -> "mb"
            "Лучшие мультфильмы" -> "bestcartoons"
            "Скоро в кино" -> "soon"
            "Сейчас в кино" -> "eo"
            else -> null
        }
    }

    private fun getSelectedCheckboxesFromContainer(containerId: Int): List<String> {
        val selectedCheckboxes = mutableListOf<String>()
        val checkboxContainer = findViewById<LinearLayout>(containerId)

        for (i in 0 until checkboxContainer.childCount) {
            val gridLayout = checkboxContainer.getChildAt(i) as GridLayout
            for (j in 0 until gridLayout.childCount) {
                val checkBox = gridLayout.getChildAt(j) as CheckBox
                if (checkBox.isChecked) {
                    selectedCheckboxes.add(checkBox.text.toString())
                }
            }
        }
        return selectedCheckboxes
    }

    private fun setupHeader(
        headerView: View,
        headerId: Int,
        containerId: Int,
        setupFunction: (LinearLayout) -> Unit
    ) {
        val headerTextView = headerView.findViewById<TextView>(headerId)
        val checkboxContainer = headerView.findViewById<LinearLayout>(containerId)

        var isRotated = false
        headerTextView.setOnClickListener {
            isRotated = !isRotated
            rotateDrawableEnd(headerTextView, isRotated)
            checkboxContainer.visibility = if (isRotated) View.VISIBLE else View.GONE
        }
        setupFunction(checkboxContainer)
    }

    private fun rotateDrawableEnd(textView: TextView, isRotated: Boolean) {
        val drawableRes = if (isRotated) R.drawable.arrow_rotate else R.drawable.ic_arrow
        textView.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(drawableRes), null)
    }

    private fun setupYearCheckboxes(container: LinearLayout) {
        val gridLayoutYears = createGridLayout(3)
        (1989..2025).forEach { year ->
            gridLayoutYears.addView(createCheckbox(year.toString()))
        }
        container.addView(gridLayoutYears)
    }

    private fun setupGenreCheckboxes(container: LinearLayout) {
        val genres = listOf(
            "Биография",
            "Боевик",
            "Вестерн",
            "Военный",
            "Детектив",
            "Детский",
            "Документальный",
            "Драма",
            "История",
            "Комедия",
            "Криминал",
            "Мелодрама",
            "Мистика",
            "Мюзикл",
            "Приключения",
            "Семейный",
            "Спорт",
            "Триллер",
            "Телепередачи",
            "Фантастика",
            "Фэнтези",
            "Ужасы"
        )
        val gridLayoutGenres = createGridLayout()

        genres.forEach { genre ->
            val checkBox = createCheckbox(genre)
            checkBox.setOnCheckedChangeListener { _, isChecked ->

            }
            gridLayoutGenres.addView(checkBox)
        }
        container.addView(gridLayoutGenres)
    }

    private fun setupTypeCheckboxes(container: LinearLayout) {
        val types = listOf(
            "Фильм", "Сериал", "Мультфильм", "Мультсериал", "Дорама (Фильм)",
            "Дорама (Сериал)", "Аниме (Фильм)", "Аниме (Сериал)", "Новинки",
            "Лучшие фильмы", "Лучшие мультфильмы", "Скоро в кино", "Сейчас в кино"
        )
        val gridLayoutTypes = createGridLayout()

        types.forEach { type ->
            val checkBox = createCheckbox(type).apply {
                setOnClickListener { uncheckOtherCheckboxes(gridLayoutTypes, this) }
            }
            gridLayoutTypes.addView(checkBox)
        }
        container.addView(gridLayoutTypes)
    }

    private fun setupSortCheckboxes(container: LinearLayout) {
        val sorts = listOf(
            "По дате", "По КП", "По IMDB", "По оценкам", "По алфавиту",
            "По просмотрам", "Топ за 3 дня", "Топ за неделю"
        )
        val gridLayoutSorts = createGridLayout()

        sorts.forEach { sort ->
            val checkBox = createCheckbox(sort).apply {
                setOnClickListener { uncheckOtherCheckboxes(gridLayoutSorts, this) }
            }
            gridLayoutSorts.addView(checkBox)
        }
        container.addView(gridLayoutSorts)
    }

    private fun setupCountryCheckboxes(container: LinearLayout) {
        val countries = listOf(
            "Австралия",
            "Аргентина",
            "Бельгия",
            "Бразилия",
            "Великобритания",
            "Германия",
            "Гонконг",
            "Индия",
            "Ирландия",
            "Исландия",
            "Испания",
            "Италия",
            "Казахстан",
            "Канада",
            "Китай",
            "Колумбия",
            "Корея Южная",
            "Люксембург",
            "Мексика",
            "Нидерланды",
            "Никарагуа",
            "Новая Зеландия",
            "Норвегия",
            "ОАЭ",
            "Польша",
            "Россия",
            "Румыния",
            "СССР",
            "США",
            "Таиланд",
            "Тайвань",
            "Турция",
            "Украина",
            "Франция",
            "Хорватия",
            "Чехия",
            "Чили",
            "Швейцария",
            "Швеция",
            "Эстония",
            "ЮАР",
            "Япония"
        )
        val gridLayoutCountries = createGridLayout()

        countries.forEach { country ->
            gridLayoutCountries.addView(createCheckbox(country))
        }
        container.addView(gridLayoutCountries)
    }

    private fun createCheckbox(text: String): CheckBox {
        return CheckBox(this).apply {
            this.text = text
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            setTextColor(resources.getColor(R.color.light_grey))
            textSize = 16f
            typeface = resources.getFont(R.font.nunito_medium)
        }
    }

    private fun createGridLayout(columnCount: Int = 1): GridLayout {
        return GridLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            this.columnCount = columnCount
        }
    }

    private fun uncheckOtherCheckboxes(gridLayout: GridLayout, selectedCheckbox: CheckBox) {
        for (i in 0 until gridLayout.childCount) {
            val checkBox = gridLayout.getChildAt(i) as CheckBox
            if (checkBox != selectedCheckbox) {
                checkBox.isChecked = false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) true else super.onOptionsItemSelected(item)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}