package com.example.morsecall

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.media.Ringtone
import android.media.RingtoneManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.morsecall.ui.theme.MorsecallTheme

// Define navigation routes
object AppDestinations {
    const val MAIN_SCREEN = "main"
    const val SETTINGS_SCREEN = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MorsecallTheme {
                // Set up the NavController
                val navController = rememberNavController()

                // NavHost defines the navigation graph
                NavHost(
                    navController = navController,
                    startDestination = AppDestinations.MAIN_SCREEN
                ) {
                    composable(AppDestinations.MAIN_SCREEN) {
                        MainScreen(navController = navController)
                    }
                    composable(AppDestinations.SETTINGS_SCREEN) {
                        SettingsScreen(navController = navController)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    var lastTapTime by remember { mutableStateOf(0L) }
    var tapCount by remember { mutableStateOf(0) }
    var consecutiveTapCount by remember { mutableStateOf(0) }
    val tapLog = remember { mutableStateListOf<String>() }
    
    // Load tap configuration settings
    val dotDuration = remember { loadDotDuration(context) }
    val dashDuration = remember { loadDashDuration(context) }
    val pauseDuration = remember { loadPauseDuration(context) }
    
    // Load ringtone
    val selectedRingtoneUri = remember { loadRingtoneUri(context) }
    var ringtone by remember { mutableStateOf<Ringtone?>(null) }
    
    // Initialize ringtone
    LaunchedEffect(selectedRingtoneUri) {
        ringtone = selectedRingtoneUri?.let { uri ->
            try {
                RingtoneManager.getRingtone(context, uri)
            } catch (e: Exception) {
                null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MorseCall") },
                actions = {
                    IconButton(onClick = {
                        // Navigate to Settings Screen
                        navController.navigate(AppDestinations.SETTINGS_SCREEN)
                        Log.d("MainScreen", "Settings button clicked, navigating to settings")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = "Tap the button below in your Morse pattern.",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            
            Text(
                text = "Sensitivity: ${
                    when {
                        dotDuration <= 150 -> "Very Fast"
                        dotDuration <= 250 -> "Fast"
                        dotDuration <= 350 -> "Normal"
                        dotDuration <= 450 -> "Slow"
                        else -> "Very Slow"
                    }
                }",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Button(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    tapCount++
                    
                    // Check for consecutive taps (within 3 seconds)
                    val timeSinceLastTap = if (lastTapTime != 0L) currentTime - lastTapTime else 0L
                    if (timeSinceLastTap < 3000 && timeSinceLastTap > 0) {
                        consecutiveTapCount++
                        if (consecutiveTapCount >= 2) {
                            // Play ringtone after 2 consecutive taps
                            ringtone?.play()
                            consecutiveTapCount = 0
                            tapLog.add(0, "🎵 RINGTONE PLAYING!")
                            Log.d("MORSE_TAP", "Ringtone triggered after 2 taps!")
                        }
                    } else {
                        // Reset consecutive count if too much time passed or first tap
                        consecutiveTapCount = 1
                    }
                    lastTapTime = currentTime
                    
                    tapLog.add(0, "Tap #$tapCount (Consecutive: $consecutiveTapCount)")
                    Log.d("MORSE_TAP", "Tap detected: #$tapCount, Consecutive: $consecutiveTapCount")
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "TAP HERE",
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Total Taps: $tapCount",
                fontSize = 16.sp
            )
            Text(
                text = "Consecutive Taps: $consecutiveTapCount/2",
                fontSize = 14.sp,
                color = if (consecutiveTapCount >= 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            if (tapLog.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Recent Activity:", fontSize = 14.sp, style = MaterialTheme.typography.titleSmall)
                Column(modifier = Modifier.heightIn(max = 200.dp)) {
                    tapLog.take(5).forEach { logEntry ->
                        Text(logEntry, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
