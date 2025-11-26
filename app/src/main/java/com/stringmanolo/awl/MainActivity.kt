package com.stringmanolo.awl

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            webView = findViewById(R.id.webview)
            setupWebView()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        
        // Configuraciones para mejorar rendimiento
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
        
        // Mejorar rendimiento en versiones modernas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        
        // Configurar para mejor rendimiento de scroll
        webSettings.allowContentAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadAllApps()
            }
        }

        // Configurar Chrome Client
        webView.webChromeClient = WebChromeClient()
        
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        
        // Cargar la página web
        webView.loadUrl("file:///android_asset/launcher.html")
    }

    private fun loadAllApps() {
        try {
            val apps = getInstalledApps()
            val appsJson = JSONArray()
            
            apps.forEach { app ->
                val appJson = JSONObject().apply {
                    put("packageName", app.packageName)
                    put("name", app.name)
                    put("icon", drawableToBase64(app.icon))
                }
                appsJson.put(appJson)
            }

            webView.evaluateJavascript("window.loadAllApps($appsJson)", null)

        } catch (e: Exception) {
            Toast.makeText(this, "Error cargando apps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val packageManager = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val packages = packageManager.queryIntentActivities(mainIntent, 0)
        val apps = mutableListOf<AppInfo>()

        packages.forEach { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            val app = AppInfo(
                packageName = activityInfo.packageName,
                name = resolveInfo.loadLabel(packageManager).toString(),
                icon = resolveInfo.loadIcon(packageManager)
            )
            apps.add(app)
        }

        return apps.sortedBy { it.name }
    }

    private fun drawableToBase64(drawable: android.graphics.drawable.Drawable): String {
        return try {
            val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: run {
                val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
            
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            ""
        }
    }

    inner class WebAppInterface {
        @android.webkit.JavascriptInterface
        fun launchApp(packageName: String) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "No se puede abrir la app", Toast.LENGTH_SHORT).show()
            }
        }

        @android.webkit.JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            moveTaskToBack(true)
        }
    }
}

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: android.graphics.drawable.Drawable
)
