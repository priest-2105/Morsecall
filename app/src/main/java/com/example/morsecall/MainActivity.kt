package com.example.morsecall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.morsecall.ui.theme.MorsecallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MorsecallTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    TapDetector()
                }
            }
        }
    }
}

@Composable
fun TapDetector() {
    var tapped by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                tapped = true
            },
        contentAlignment = Alignment.Center
    ) {
        if (tapped) {
            Text(
                text = "Tapped!",
                color = Color.Red,
                fontSize = 30.sp
            )
        }
    }
}
