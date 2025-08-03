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
    val tapLog = remember { mutableStateListOf<String>() }
    
    // Load tap configuration settings
    val dotDuration = remember { loadDotDuration(context) }
    val dashDuration = remember { loadDashDuration(context) }
    val pauseDuration = remember { loadPauseDuration(context) }

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
                text = "Dot: <${dotDuration}ms | Dash: >${dashDuration}ms | Pause: >${pauseDuration}ms",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Button(
                onClick = { /* Primary action handled by pointerInput */ },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                val currentTime = System.currentTimeMillis()
                                val pressTime = currentTime
                                if (lastTapTime != 0L) {
                                    val actualPauseDuration = currentTime - lastTapTime
                                    if (actualPauseDuration > pauseDuration) {
                                        Log.d("MORSE_TAP", "Long pause: $actualPauseDuration ms")
                                        tapLog.add(0, "Long pause: $actualPauseDuration ms")
                                    }
                                }
                                Log.d("MORSE_TAP", "Pressed at $pressTime")
                                tapLog.add(0, "Pressed")
                                try {
                                    awaitRelease()
                                    val releaseTime = System.currentTimeMillis()
                                    val tapDuration = releaseTime - pressTime
                                    val symbol = if (tapDuration < dotDuration) "." else "-"
                                    Log.d("MORSE_TAP", "Tap Duration: $tapDuration ms -> $symbol")
                                    tapLog.add(0, "$symbol ($tapDuration ms)")
                                    tapCount++
                                    lastTapTime = releaseTime
                                } catch (e: GestureCancellationException) {
                                    Log.d("MORSE_TAP", "Tap cancelled")
                                    tapLog.add(0, "Cancelled")
                                }
                            }
                        )
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
                text = "Taps Registered: $tapCount",
                fontSize = 16.sp
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
