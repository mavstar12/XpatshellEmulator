package com.example.xpatshell.emulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).hide(
            WindowInsetsCompat.Type.statusBars() or
            WindowInsetsCompat.Type.navigationBars()
        )
        
        setContent {
            XpatshellScreen()
        }
    }
}

@Composable
fun XpatshellScreen() {
    var url by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .consumeWindowInsets(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Xpatshell Logo",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Xpatshell Emulator",
                    fontSize = 28.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
    
            Column(
                modifier = Modifier.padding(16.dp).background(color.grey)
            ) {
                Text(
                    text = "Enter your app preview url starting with 'xpss://'",
                    fontSize = 17.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    placeholder = { Text("xpss://123.45.6.78:3000") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(30.dp))
                OutlinedButton(
                    onClick = {
                        // Load URL logic here
                    },
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)!/
                        .background(Color.Black)
                ) {
                    Text("Load Url", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
