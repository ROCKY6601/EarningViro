package com.earning.viro

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsLoadError
import com.unity3d.ads.UnityAdsShowError
import com.unity3d.ads.UnityAdsShowOptions

class MainActivity : AppCompatActivity() {

    // ============================================================
    // 🔴 YAHAN APNE 3 SERVER LINKS DAALEIN
    // ============================================================
    private val server1Url = "https://example1.com"
    private val server2Url = "https://example2.com"
    private val server3Url = "https://example3.com"
    // ============================================================

    // ============================================================
    // 🔴 UNITY ADS CONFIG — AAPKI IDs
    // ============================================================
    private val unityGameId = "800377542"
    private val rewardedPlacementId = "BP_Rewarded_Android"
    private val testMode = true
    // ============================================================

    private lateinit var homeLayout: FrameLayout
    private lateinit var webContainer: LinearLayout
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var backBtn: TextView
    private lateinit var titleTxt: TextView
    private lateinit var adLoadingTxt: TextView

    private var pendingUrl: String = ""
    private var pendingName: String = ""
    private var adIsReady = false
    private var adIsLoading = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        homeLayout = findViewById(R.id.homeLayout)
        webContainer = findViewById(R.id.webContainer)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        backBtn = findViewById(R.id.backBtn)
        titleTxt = findViewById(R.id.titleTxt)
        adLoadingTxt = findViewById(R.id.adLoadingTxt)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
            }
        }
        webView.webChromeClient = WebChromeClient()

        findViewById<LinearLayout>(R.id.cardServer1).setOnClickListener {
            requestServer(server1Url, "SERVER 1")
        }
        findViewById<LinearLayout>(R.id.cardServer2).setOnClickListener {
            requestServer(server2Url, "SERVER 2")
        }
        findViewById<LinearLayout>(R.id.cardServer3).setOnClickListener {
            requestServer(server3Url, "SERVER 3")
        }

        backBtn.setOnClickListener {
            if (webView.canGoBack()) webView.goBack() else closeWeb()
        }

        initializeUnityAds()
    }

    private fun initializeUnityAds() {
        if (UnityAds.isInitialized) {
            loadRewardedAd()
            return
        }

        UnityAds.initialize(this, unityGameId, testMode, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ads Ready ✅", Toast.LENGTH_SHORT).show()
                    loadRewardedAd()
                }
            }

            override fun onInitializationFailed(
                error: UnityAds.UnityAdsInitializationError?,
                message: String?
            ) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ad Init Failed: $message", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun loadRewardedAd() {
        if (adIsReady || adIsLoading) return
        adIsLoading = true

        UnityAds.load(rewardedPlacementId, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                adIsReady = true
                adIsLoading = false
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAdsLoadError?,
                message: String?
            ) {
                adIsReady = false
                adIsLoading = false
            }
        })
    }

    private fun requestServer(url: String, name: String) {
        if (adIsReady) {
            showRewardedAd(url, name)
            return
        }

        adLoadingTxt.visibility = View.VISIBLE
        adLoadingTxt.text = "⏳ Ad loading... please wait"
        pendingUrl = url
        pendingName = name

        loadRewardedAd()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (adIsReady) {
                    adLoadingTxt.visibility = View.GONE
                    showRewardedAd(pendingUrl, pendingName)
                } else {
                    handler.postDelayed(this, 500)
                }
            }
        }, 500)
    }

    private fun showRewardedAd(url: String, name: String) {
        if (!adIsReady) {
            openServer(url, name)
            return
        }

        adIsReady = false

        UnityAds.show(this, rewardedPlacementId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(
                placementId: String?,
                error: UnityAdsShowError?,
                message: String?
            ) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ad failed, opening server...", Toast.LENGTH_SHORT).show()
                    openServer(url, name)
                }
            }

            override fun onUnityAdsShowStart(placementId: String?) {
                adLoadingTxt.visibility = View.GONE
            }

            override fun onUnityAdsShowClick(placementId: String?) {
            }

            override fun onUnityAdsShowComplete(
                placementId: String?,
                state: UnityAds.UnityAdsShowCompletionState?
            ) {
                runOnUiThread {
                    openServer(url, name)
                    loadRewardedAd()
                }
            }
        })
    }

    private fun openServer(url: String, name: String) {
        homeLayout.visibility = View.GONE
        webContainer.visibility = View.VISIBLE
        titleTxt.text = name
        progressBar.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    private fun closeWeb() {
        webContainer.visibility = View.GONE
        homeLayout.visibility = View.VISIBLE
        webView.loadUrl("about:blank")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webContainer.visibility == View.VISIBLE) {
            if (webView.canGoBack()) webView.goBack() else closeWeb()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
