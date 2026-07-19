package com.diegohgc.billarholandes

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
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

        WebView.setWebContentsDebuggingEnabled(true)
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
            // disallowed_useragent). Un Intent normal a Chrome tampoco vale: Chrome detecta que la
            // app que lo abre sigue teniendo su propio WebView activo y se cierra solo sin mostrar
            // nada (mismo bloqueo, aplicado a nivel de Android). La forma que Google si acepta es
            // Chrome Custom Tabs (libreria androidx.browser), pensada justamente para este caso.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.host?.contains("accounts.google.com") == true) {
                    // se fuerza Chrome porque es el unico navegador confirmado que Google acepta
                    // para este flujo; otros navegadores (Firefox) pueden bloquear igualmente el
                    // redirect final hacia el esquema puckslide:// o el propio login de Google
                    val customTabsIntent = CustomTabsIntent.Builder().build()
                    customTabsIntent.intent.setPackage("com.android.chrome")
                    try {
                        customTabsIntent.launchUrl(this@MainActivity, url)
                    } catch (e: ActivityNotFoundException) {
                        customTabsIntent.intent.setPackage(null)
                        customTabsIntent.launchUrl(this@MainActivity, url)
                    }
                    return true
                }
                return false
            }
        }
        // sin esto, alert()/confirm() y console.log() de la pagina no llegan a ningun sitio: se
        // pierden en silencio porque un WebView plano no gestiona esos dialogos/mensajes de JS.
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d("WebConsole", "${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                return true
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

    // tras completar el login en el navegador externo, Supabase redirige a auth-bridge.html, que
    // relanza esta app con un intent:// explicito (package=com.diegohgc.billarholandes) llevando
    // el token como query param "authFragment" (un intent:// no admite '#' antes de "#Intent").
    // Aqui lo recolocamos como fragmento real de la URL del juego para que supabase-js lo procese
    // exactamente igual que hace en el navegador normal.
    private fun handleAuthCallbackIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.host != "diegohgc.github.io") return
        val authFragment = data.getQueryParameter("authFragment") ?: return
        // si la URL solo cambia en el "#..." respecto a la ya cargada, el WebView lo trata como
        // una navegacion dentro del mismo documento (como un ancla) y NO vuelve a ejecutar el
        // script de la pagina - por eso supabase-js nunca llegaba a leer el token nuevo. Anadir
        // un parametro que cambie cada vez fuerza una recarga completa de verdad.
        val url = "$GAME_URL&_t=${System.currentTimeMillis()}#$authFragment"
        // si se navega justo cuando la Activity todavia esta volviendo a primer plano (recien
        // cerrado el Custom Tab), el WebView a veces no repinta la pantalla aunque la pagina si
        // cargue el token correctamente; posponerlo al siguiente frame de la UI lo evita
        webView.post { webView.loadUrl(url) }
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
