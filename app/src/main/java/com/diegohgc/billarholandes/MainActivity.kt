package com.diegohgc.billarholandes

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

private const val GAME_URL = "https://diegohgc.github.io/billar-holandes/?app=android"

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

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, bars.bottom)
            insets
        }

        adView.loadAd(AdRequest.Builder().build())

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.textZoom = 100
        // always fetch the latest game HTML from the network instead of serving a stale
        // cached copy - this app updates purely by editing the web repo, so a cached page
        // can silently hide bug fixes from the player for a long time otherwise
        settings.cacheMode = WebSettings.LOAD_NO_CACHE

        webView.webViewClient = object : WebViewClient() {
            // Google bloquea el login OAuth dentro de un WebView embebido (error
            // disallowed_useragent); en cuanto la navegacion vaya a accounts.google.com la
            // abrimos en el navegador del sistema, que si esta permitido.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.host?.contains("accounts.google.com") == true) {
                    // se fuerza Chrome porque es el que gestiona de forma fiable la redireccion
                    // final (via script) hacia el esquema puckslide://; otros navegadores como
                    // Firefox pueden ignorarla o mostrarla como texto en vez de reabrir la app
                    val intent = Intent(Intent.ACTION_VIEW, url).setPackage("com.android.chrome")
                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                    }
                    return true
                }
                return false
            }
        }
        webView.setInitialScale(1)
        webView.loadUrl(GAME_URL)

        handleAuthCallbackIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthCallbackIntent(intent)
    }

    // tras completar el login en el navegador externo, Supabase redirige a
    // puckslide://auth-callback#access_token=...; el sistema reabre esta Activity con ese intent,
    // y aqui recargamos el juego con ese mismo trozo (#...) para que supabase-js recoja la sesion
    // exactamente igual que hace en el navegador normal.
    private fun handleAuthCallbackIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "puckslide") return
        val fragment = data.encodedFragment
        val url = if (fragment != null) "$GAME_URL#$fragment" else GAME_URL
        webView.loadUrl(url)
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
