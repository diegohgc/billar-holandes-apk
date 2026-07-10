package com.diegohgc.billarholandes

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var adView: AdView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this)

        webView = WebView(this)
        adView = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = "ca-app-pub-5015878857432448/5769337587"
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Toast.makeText(this@MainActivity, "Anuncio cargado correctamente", Toast.LENGTH_SHORT).show()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Toast.makeText(this@MainActivity, "Fallo al cargar anuncio: ${error.message} (código ${error.code})", Toast.LENGTH_LONG).show()
                }
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(webView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(adView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER_HORIZONTAL })
        setContentView(root)

        adView.loadAd(AdRequest.Builder().build())

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.textZoom = 100

        webView.webViewClient = WebViewClient()
        webView.setInitialScale(1)
        webView.loadUrl("https://diegohgc.github.io/billar-holandes/")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        adView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onDestroy() {
        adView.destroy()
        super.onDestroy()
    }
}
