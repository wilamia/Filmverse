package com.example.filmverse.Activities

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.filmverse.R

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var videoView: WebView
    private var webViewState: Bundle? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var videoUrl: String? = null
    private var currentTime: Int = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        videoView = findViewById(R.id.fullscreenWebView)
        sharedPreferences = getSharedPreferences("video_preferences", MODE_PRIVATE)

        videoView.settings.javaScriptEnabled = true
        videoView.settings.domStorageEnabled = true
        videoView.settings.mediaPlaybackRequiresUserGesture = false

        videoView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url.toString()
                return if (isAdUrl(url)) {
                    WebResourceResponse("text/plain", "utf-8", null)
                } else {
                    super.shouldInterceptRequest(view, request)
                }
            }

            private fun isAdUrl(url: String): Boolean {
                val blockedKeywords = listOf("ad", "advertising", "betera")
                return blockedKeywords.any { url.contains(it, ignoreCase = true) }
            }
        }

        videoUrl = intent.getStringExtra("VIDEO_URL")
        currentTime = sharedPreferences.getInt(videoUrl ?: "", 0)

        if (savedInstanceState != null) {
            webViewState = savedInstanceState.getBundle("webViewState")
            webViewState?.let { videoView.restoreState(it) }
        } else {
            loadVideo(videoUrl, currentTime)
        }

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun loadVideo(url: String?, startTime: Int) {
        val htmlContent = """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body {
                margin: 0;
                padding: 0;
                overflow: hidden;
                background-color: black;
            }
            .video-container {
                width: 100%;
                height: 100%;
                object-fit: contain;
            }
            iframe {
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                border: none;
                object-fit: contain;
            }
        </style>
        <script>
            var video;
            window.onload = function() {
                video = document.querySelector('.video-container');
                if (video) {
                    video.currentTime = $startTime;
                    video.play();
                }
            };
            
            function onPiPMode() {
                if (video) {
                    video.play();
                }
            }
        </script>
    </head>
    <body>
        <div class="video-container">
            <iframe src="$url" allowfullscreen></iframe>
        </div>
    </body>
    </html>
""".trimIndent().replace("$startTime", startTime.toString())

        videoView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webViewState = Bundle()
        videoView.saveState(webViewState!!)
        outState.putBundle("webViewState", webViewState)
    }

    override fun onBackPressed() {
        if (videoView.canGoBack()) {
            videoView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPiPMode(Rational(16, 9))
    }

    private fun enterPiPMode(aspectRatio: Rational = Rational(1, 1)) {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build()
        enterPictureInPictureMode(params)
    }

    override fun onPause() {
        super.onPause()
        videoUrl?.let { url ->
            videoView.evaluateJavascript("document.querySelector('video').currentTime;", { time ->
                val currentTime = time.toIntOrNull() ?: 0
                sharedPreferences.edit().putInt(url, currentTime).apply()
            })
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            videoView.evaluateJavascript("onPiPMode();", null)
        }
    }
}