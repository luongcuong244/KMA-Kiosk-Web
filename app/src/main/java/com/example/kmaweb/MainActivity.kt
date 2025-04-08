package com.example.kmaweb

import android.annotation.SuppressLint
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var btnReload: Button
    private lateinit var txtError: TextView
    private var mUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemUI()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        btnReload = findViewById(R.id.btnReload)
        txtError = findViewById(R.id.txtError)

        initWebView()
        loadUrl()

        btnReload.setOnClickListener {
            loadUrl()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called")
        loadUrl()
    }

    private fun initWebView() {
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e("WebViewError", "Error: $description")
                showReloadUI("Lỗi khi tải trang: $description")
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: android.webkit.WebResourceRequest,
                errorResponse: android.webkit.WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                Log.e("WebViewHttpError", "HTTP error: ${errorResponse.statusCode}")
                showReloadUI("Lỗi HTTP: ${errorResponse.statusCode}")
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
    }

    private fun loadUrl() {
        if (!isNetworkConnected()) {
            showReloadUI("Không có kết nối mạng. Vui lòng kiểm tra Wi-Fi hoặc dữ liệu di động.")
            return
        }

        val uri = Uri.parse("content://com.example.kmamdm.appsettingprovider/web")
        val cursor = contentResolver.query(uri, null, null, null, null)

        if (cursor != null) {
            try {
                var foundUrl = false
                while (cursor.moveToNext()) {
                    val attribute = cursor.getString(0)
                    val value = cursor.getString(1)
                    val comment = cursor.getString(2)
                    Log.d("MainActivity", "Attribute: $attribute - Value: $value - Comment: $comment")

                    if (attribute == "webview_url") {
                        mUrl = value
                        foundUrl = true
                        Log.d("MainActivity", "WebView URL: $mUrl")
                        showWebView()
                        webView.loadUrl(mUrl ?: "https://www.google.com")
                        break
                    }
                }
                if (!foundUrl) {
                    showReloadUI("Không tìm thấy URL hợp lệ trong dữ liệu.")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Lỗi khi đọc dữ liệu từ ContentProvider", e)
                showReloadUI("Đã xảy ra lỗi khi đọc dữ liệu.")
            } finally {
                cursor.close()
            }
        } else {
            showReloadUI("Không lấy được dữ liệu. Cursor trả về null.")
        }
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showReloadUI(message: String) {
        Log.d("MainActivity", "showReloadUI: $message")
        webView.visibility = View.GONE
        btnReload.visibility = View.VISIBLE
        txtError.visibility = View.VISIBLE
        txtError.text = message
    }

    private fun showWebView() {
        webView.visibility = View.VISIBLE
        btnReload.visibility = View.GONE
        txtError.visibility = View.GONE
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    private fun hideSystemUI() {
        val flags: Int = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.decorView.systemUiVisibility = flags
        val decorView: View = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                decorView.systemUiVisibility = flags
            }
        }
    }
}