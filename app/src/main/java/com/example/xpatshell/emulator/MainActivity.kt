package com.example.xpatshell.emulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XpatshellScreen()
        }
    }
}

@Composable
fun XpatshellScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "XpatshellEmulator",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "come join xpatshell",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_launcher), // replace with your image
            contentDescription = "Xpatshell Logo",
            modifier = Modifier.size(120.dp)
        )
    }
}