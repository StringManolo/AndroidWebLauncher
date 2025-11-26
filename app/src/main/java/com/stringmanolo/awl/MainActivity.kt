package com.stringmanolo.awl

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
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

    webView = findViewById(R.id.webview)
    setupWebView()

    webView.loadUrl("file:///android_asset/launcher.html")
  }

  @SuppressLint("SetJavaScriptEnabled")
  private fun setupWebView() {
    val webSettings = webView.settings
    webSettings.javaScriptEnabled = true
    webSettings.domStorageEnabled = true
    webSettings.allowFileAccess = true
    webSettings.allowContentAccess = true

    webView.addJavascriptInterface(WebAppInterface(), "Android")

    webView.webViewClient = object : WebViewClient() {
      override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        loadAppsToWebView()
      }
    }
  }

  private fun loadAppsToWebView() {
    val apps = getInstalledApps()
    val appsJson = JSONArray()

    apps.forEach { app ->
      val appJson = JSONObject().apply {
        put("packageName", app.packageName)
        put("name", app.name)
        put("className", app.className)
        put("icon", drawableToBase64(app.icon))
      }
      appsJson.put(appJson)
    }

    webView.evaluateJavascript("window.loadApps($appsJson)", null)
  }

  private fun getInstalledApps(): List<AppInfo> {
    val packageManager = packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null)
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

    val packages = packageManager.queryIntentActivities(mainIntent, 0)
    val apps = mutableListOf<AppInfo>()

    packages.forEach { resolveInfo ->
      val activityInfo = resolveInfo.activityInfo
      val applicationInfo = activityInfo.applicationInfo

      val app = AppInfo(
        packageName = activityInfo.packageName,
        name = resolveInfo.loadLabel(packageManager).toString(),
        icon = resolveInfo.loadIcon(packageManager),
        className = activityInfo.name
      )
      apps.add(app)
    }

    return apps.sortedBy { it.name }
  }

  private fun drawableToBase64(drawable: android.graphics.drawable.Drawable): String {
    val bitmap = (drawable as BitmapDrawable).bitmap
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val byteArray = stream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
  }

  inner class WebAppInterface {
    @JavascriptInterface
    fun launchApp(packageName: String) {
      try {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
          startActivity(intent)
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    @JavascriptInterface
    fun getCurrentTime(): String {
      return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
      .format(java.util.Date())
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
  val icon: android.graphics.drawable.Drawable,
  val className: String
)
