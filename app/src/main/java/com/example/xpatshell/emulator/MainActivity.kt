package com.example.xpatshell

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import okhttp3.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen / edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            XpatshellScreen()
        }
    }
}

@Composable
fun XpatshellScreen() {
    var inputUrl by remember { mutableStateOf("") }
    var previewUrl by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (previewUrl != null) {
            FullScreenWebViewWithWS(
                url = previewUrl!!,
                onExit = { previewUrl = null }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    placeholder = { Text("xpss://192.168.0.100:3000") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (inputUrl.startsWith("xpss://")) {
                            previewUrl = inputUrl.replace("xpss://", "http://")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Run App")
                }
            }
        }
    }
}

@Composable
fun FullScreenWebViewWithWS(
    url: String,
    onExit: () -> Unit
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    // WebSocket lifecycle (SAFE)
    DisposableEffect(url) {
        val wsUrl = url.replace("http://", "ws://")
        val client = OkHttpClient()

        val request = Request.Builder().url(wsUrl).build()
        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text == "reload") {
                    webViewRef.value?.post {
                        webViewRef.value?.reload()
                    }
                }
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {
                Log.e("XpatshellWS", "WebSocket error", t)
            }
        })

        onDispose {
            ws.close(1000, "Disposed")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    loadUrl(url)
                    webViewRef.value = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = onExit,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Text("Back")
        }
    }
}