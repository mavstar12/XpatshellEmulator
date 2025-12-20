import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import okhttp3.*
import okio.ByteString
import org.json.JSONObject

@Composable
fun XpatshellScreen() {
    val context = LocalContext.current
    var inputUrl by remember { mutableStateOf("") }
    var webViewUrl by remember { mutableStateOf<String?>(null) }
    var ws by remember { mutableStateOf<WebSocket?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        if (webViewUrl != null) {
            // Full-screen WebView
            FullScreenWebView(
                url = webViewUrl!!,
                onWebViewCreated = { webView ->

                    if (ws == null) {
                        // Open WebSocket to the same host:port as the URL
                        val wsUrl = webViewUrl!!
                            .replaceFirst("http://", "ws://") // WebSocket URL
                        val request = Request.Builder().url(wsUrl).build()
                        val client = OkHttpClient()

                        ws = client.newWebSocket(request, object : WebSocketListener() {
                            override fun onOpen(webSocket: WebSocket, response: Response) {
                                Log.d("XpatshellWS", "Connected to server")
                            }

                            override fun onMessage(webSocket: WebSocket, text: String) {
                                try {
                                    val json = JSONObject(text)
                                    when {
                                        json.has("reload") -> {
                                            webView.post { webView.reload() }
                                        }
                                        json.has("navigate") -> {
                                            val newUrl = json.getString("navigate")
                                            webView.post { webView.loadUrl(newUrl) }
                                        }
                                        json.has("toast") -> {
                                            val msg = json.getString("toast")
                                            webView.post {
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT)
                                                    .show()
                                            }
                                        }
                                        json.has("console") -> {
                                            val logMsg = json.getString("console")
                                            Log.d("XpatshellConsole", logMsg)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("XpatshellWS", "Invalid WS message: $text")
                                }
                            }

                            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {}
                            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                                t.printStackTrace()
                            }

                            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                                Log.d("XpatshellWS", "WebSocket closed: $reason")
                            }
                        })
                    }
                }
            )

            // Back button
            Button(
                onClick = {
                    webViewUrl = null
                    ws?.close(1000, "User exited")
                    ws = null
                },
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize()
            ) {
                Text("Back")
            }

        } else {
            // Input UI
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Enter your app preview URL starting with 'xpss://'")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    singleLine = true,
                    placeholder = { Text("xpss://192.168.0.100:3000") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (inputUrl.startsWith("xpss://")) {
                            webViewUrl = inputUrl.replaceFirst("xpss://", "http://")
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
fun FullScreenWebView(
    url: String,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                loadUrl(url)

                onWebViewCreated(this)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}