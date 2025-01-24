package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.filmverse.R

class VideoPlayerTrailerActivity : AppCompatActivity() {
    private lateinit var videoView: WebView
    private var webViewState: Bundle? = null

    @SuppressLint("MissingInflatedId", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player_trailer)

        videoView = findViewById(R.id.fullscreenWebView)

        videoView.settings.javaScriptEnabled = true
        videoView.settings.domStorageEnabled = true
        videoView.settings.mediaPlaybackRequiresUserGesture = true

        videoView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.e("WebViewError", "Ошибка загрузки: ${error?.description}")
            }
        }

        if (savedInstanceState != null) {
            webViewState = savedInstanceState.getBundle("webViewState")
            webViewState?.let { videoView.restoreState(it) }
        } else {
            val videoUrl = intent.getStringExtra("VIDEO_URL")
            videoUrl?.let { loadVideo(it) }
        }
    }

    private fun loadVideo(url: String) {
        videoView.loadUrl(url)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webViewState = Bundle()
        videoView.saveState(webViewState!!)
        outState.putBundle("webViewState", webViewState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webViewState = savedInstanceState.getBundle("webViewState")
    }

    override fun onBackPressed() {
        if (videoView.canGoBack()) {
            videoView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}