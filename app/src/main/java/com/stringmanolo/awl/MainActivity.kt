package com.stringmanolo.awl

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
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

        Toast.makeText(this, "🚀 Launcher iniciado", Toast.LENGTH_SHORT).show()

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
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Toast.makeText(this@MainActivity, "📱 Cargando aplicaciones...", Toast.LENGTH_SHORT).show()
                loadAllApps()
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "Android")
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

            webView.evaluateJavascript("window.loadAllApps($appsJson)") { 
                Toast.makeText(this, "✅ ${apps.size} apps cargadas", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error cargando apps", Toast.LENGTH_LONG).show()
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
                } else {
                    Toast.makeText(this@MainActivity, "❌ No se puede abrir la app", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "❌ Error al abrir app", Toast.LENGTH_SHORT).show()
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







/*package com.stringmanolo.awl

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Toast.makeText(this, "Launcher iniciando...", Toast.LENGTH_SHORT).show()

        try {
            webView = findViewById(R.id.webview)
            setupWebView()
            Toast.makeText(this, "WebView configurado", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error en onCreate: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        try {
            val webSettings = webView.settings
            webSettings.javaScriptEnabled = true
            webSettings.domStorageEnabled = true
            
            // WebViewClient básico
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Toast.makeText(this@MainActivity, "Página cargada: $url", Toast.LENGTH_SHORT).show()
                    loadBasicApps()
                }

                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Toast.makeText(this@MainActivity, "Error WebView: $description", Toast.LENGTH_LONG).show()
                }
            }

            // Interfaz JavaScript simple
            webView.addJavascriptInterface(WebAppInterface(), "Android")

            // Cargar HTML básico
            webView.loadUrl("file:///android_asset/simple_launcher.html")
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error configurando WebView: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadBasicApps() {
        try {
            // Solo cargar algunas apps básicas para prueba
            val basicApps = listOf(
                mapOf("name" to "Ajustes", "package" to "com.android.settings"),
                mapOf("name" to "Navegador", "package" to "com.android.chrome"),
                mapOf("name" to "Cámara", "package" to "com.android.camera")
            )

            val appsJson = basicApps.joinToString(
                ", ",
                prefix = "[",
                postfix = "]"
            ) { app ->
                """{"name":"${app["name"]}","packageName":"${app["package"]}"}"""
            }

            webView.evaluateJavascript("window.loadBasicApps($appsJson)") { 
                Toast.makeText(this, "Apps cargadas en WebView", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error cargando apps: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    inner class WebAppInterface {
        @android.webkit.JavascriptInterface
        fun launchApp(packageName: String) {
            try {
                Toast.makeText(this@MainActivity, "Intentando abrir: $packageName", Toast.LENGTH_SHORT).show()
                
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    startActivity(intent)
                    Toast.makeText(this@MainActivity, "App lanzada: $packageName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "No se pudo abrir: $packageName", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        @android.webkit.JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(this@MainActivity, "JS: $message", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            moveTaskToBack(true)
        }
    }
}*/


