import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    private var webSocket: WebSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp { url ->
                val wsUrl = url.replace("http://", "ws://").replace("https://", "wss://")
                connectWebSocket(wsUrl)
            }
        }
    }  

    private fun connectWebSocket(wsUrl: String) {
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("XPAT", "WebSocket connected")
                val hello = JSONObject().apply {
                    put("type", "hello")
                    put("platform", "android")
                    put("package", packageName)
                }
                ws.send(hello.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("XPAT", "WS message: $text")
                // Handle incoming messages here
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("XPAT", "WS failure: ${t.message}")
            }
        })
    }
}

@Composable
fun MyApp(onConnect: (String) -> Unit) {
    var showWebView by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showWebView) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Xpatshell Emulator", color = Color.White)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Enter server URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    if (url.isNotEmpty()) {
                        showWebView = true
                        onConnect(url)
                    }
                }) {
                    Text("Connect")
                }
            }
        }
    }
}