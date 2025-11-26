package com.stringmanolo.awl

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraPhotoPath: String? = null

    // Registro para el resultado de la actividad de selección de archivos
    private val filePickerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                filePathCallback?.onReceiveValue(arrayOf(uri))
            } ?: run {
                filePathCallback?.onReceiveValue(null)
            }
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

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
        webSettings.allowContentAccess = true
        
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
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadAllApps()
            }
        }

        // Configurar Chrome Client para selección de archivos
        webView.webChromeClient = object : WebChromeClient() {
            // Para Android 5.0+
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val intent = fileChooserParams?.createIntent()
                try {
                    filePickerResult.launch(intent)
                } catch (e: Exception) {
                    filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = null
                    return false
                }
                return true
            }
        }

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
            val byteArray = stream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
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

        @android.webkit.JavascriptInterface
        fun uninstallApp(packageName: String) {
            try {
                val intent = Intent(Intent.ACTION_DELETE)
                intent.data = Uri.parse("package:$packageName")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "No se puede desinstalar la app", Toast.LENGTH_SHORT).show()
            }
        }

        @android.webkit.JavascriptInterface
        fun openGallery() {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, 100)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "No se puede abrir la galería", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Pasar la URI de la imagen seleccionada al WebView
                val imageUri = uri.toString()
                webView.evaluateJavascript("window.onImageSelected('$imageUri')", null)
            }
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
