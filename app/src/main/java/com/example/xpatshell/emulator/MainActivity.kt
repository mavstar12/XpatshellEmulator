package com.example.xpatshell.emulator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var webSocket: WebSocket? = null
    private lateinit var dispatcher: NativeDispatcher

    // Change this to your dev server IP/port or make it configurable
    private var httpUrl = "http://192.168.0.105:3000"
    private var wsUrl = "ws://192.168.0.105:3000"

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    // Permissions that may be needed at runtime
    private val requiredPermissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.SEND_SMS
    )

    // ActivityResult launcher for runtime permission request (optional)
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            // No-op here; each call checks permission and returns error if missing
            Log.d("XPAT", "Permissions result: $perms")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple layout programmatic (safe if layout resource isn't present)
        webView = WebView(this)
        setContentView(webView)

        // Setup WebView
        setupWebView()

        // Create dispatcher that implements native functions
        dispatcher = NativeDispatcher(this) { sendResponse(it) }

        // Load URL - allow custom scheme 'xps://' by converting to http/https
        val startUrl = intent?.dataString ?: httpUrl
        val resolved = resolveXpsScheme(startUrl)
        webView.loadUrl(resolved)

        connectWebSocket(resolveWsScheme(wsUrl))

        // Ask user for common permissions proactively (optional)
        requestPermissionsIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "bye")
    }

    private fun resolveXpsScheme(url: String): String {
        return when {
            url.startsWith("xps://") -> url.replaceFirst("xps://", "http://")
            url.startsWith("xpss://") -> url.replaceFirst("xpss://", "https://")
            else -> url
        }
    }

    private fun resolveWsScheme(w: String): String {
        return w.replaceFirst("xps://", "http://").replaceFirst("xpss://", "https://")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.loadsImagesAutomatically = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        // Inject a small JS bridge if needed (optional)
    }

    private fun requestPermissionsIfNeeded() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun connectWebSocket(wsUrl: String) {
        try {
            val request = Request.Builder().url(wsUrl).build()
            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d("XPAT", "WebSocket connected")
                    // hello message
                    val hello = JSONObject()
                    hello.put("type", "hello")
                    hello.put("platform", "android")
                    hello.put("package", packageName)
                    ws.send(hello.toString())
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    Log.d("XPAT", "WS message: $text")
                    // Expect JSON messages with { id, module, action, params }
                    try {
                        val msg = JSONObject(text)
                        val id = msg.optString("id", "")
                        val module = msg.optString("module", "")
                        val action = msg.optString("action", "")
                        val params = msg.optJSONObject("params") ?: JSONObject()
                        // Dispatch to native dispatcher on UI thread if needed
                        runOnUiThread {
                            dispatcher.dispatch(id, module, action, params)
                        }
                    } catch (e: Exception) {
                        Log.e("XPAT", "Invalid WS message: ${e.message}")
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e("XPAT", "WS failure: ${t.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("XPAT", "Failed to connect WS: ${e.message}")
        }
    }

    // Compose & send responses back via websocket
    private fun sendResponse(responseJson: JSONObject) {
        try {
            webSocket?.send(responseJson.toString())
        } catch (e: Exception) {
            Log.e("XPAT", "Failed to send response: ${e.message}")
        }
    }
}