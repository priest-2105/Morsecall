package com.example.morsecall

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
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
    var isActive by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    val tapLog = remember { mutableStateListOf<String>() }
    
    // Load tap configuration settings
    val dotDuration = remember { loadDotDuration(context) }
    val dashDuration = remember { loadDashDuration(context) }
    val pauseDuration = remember { loadPauseDuration(context) }
    val tapTriggerCount = remember { loadTapTriggerCount(context) }
    
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

    val appBarState = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(appBarState.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MorseCall", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(AppDestinations.SETTINGS_SCREEN)
                        Log.d("MainScreen", "Settings button clicked, navigating to settings")
                    }) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                scrollBehavior = appBarState
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Minimal, icon-forward UI

            // Activate/Deactivate Button
            // Circular power toggle
            FilledTonalIconButton(
                onClick = { 
                    isActive = !isActive
                    if (isActive) {
                        tapLog.add(0, "✅ App ACTIVATED")
                        Log.d("MORSE_TAP", "App activated")
                    } else {
                        // Stop ringtone if currently playing
                        if (isPlaying) {
                            ringtone?.stop()
                            isPlaying = false
                            tapLog.add(0, "⏹️ RINGTONE STOPPED (deactivated)")
                            Log.d("MORSE_TAP", "Ringtone stopped due to deactivation")
                        }
                        // Reset tap counters/state
                        tapCount = 0
                        consecutiveTapCount = 0
                        lastTapTime = 0L
                        tapLog.add(0, "🔄 Counters reset")
                        tapLog.add(0, "❌ App DEACTIVATED")
                        Log.d("MORSE_TAP", "App deactivated and counters reset")
                    }
                },
                modifier = Modifier.size(80.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Power,
                    contentDescription = if (isActive) "Deactivate" else "Activate",
                    modifier = Modifier.size(36.dp)
                )
            }

            // Animated pulse on the main tap area
            Button(
                onClick = {
                    if (isActive) {
                        val currentTime = System.currentTimeMillis()
                        tapCount++
                        
                        // Check for consecutive taps (within 3 seconds)
                        val timeSinceLastTap = if (lastTapTime != 0L) currentTime - lastTapTime else 0L
                        if (timeSinceLastTap < 3000 && timeSinceLastTap > 0) {
                            consecutiveTapCount++
                            if (consecutiveTapCount >= tapTriggerCount) {
                            // Play ringtone after configured number of consecutive taps
                            ringtone?.play()
                            isPlaying = true
                            consecutiveTapCount = 0
                            tapLog.add(0, "🎵 RINGTONE PLAYING!")
                            Log.d("MORSE_TAP", "Ringtone triggered after $tapTriggerCount taps!")
                        }
                        } else {
                            // Reset consecutive count if too much time passed or first tap
                            consecutiveTapCount = 1
                        }
                        lastTapTime = currentTime
                        
                        tapLog.add(0, "Tap #$tapCount (Consecutive: $consecutiveTapCount)")
                        Log.d("MORSE_TAP", "Tap detected: #$tapCount, Consecutive: $consecutiveTapCount")
                    } else {
                        tapLog.add(0, "⚠️ App is not active - tap ignored")
                        Log.d("MORSE_TAP", "Tap ignored - app not active")
                    }
                },
                shape = MaterialTheme.shapes.large,
                modifier = run {
                    val base = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                    if (isActive) {
                        val infinite = rememberInfiniteTransition(label = "pulse")
                        val scale = infinite.animateFloat(
                            initialValue = 0.98f,
                            targetValue = 1.02f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 800),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        base.graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                    } else base
                },
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                    contentDescription = "Tap",
                    tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            // Stop Button (only shows when ringtone is playing)
            if (isPlaying) {
                FilledTonalIconButton(
                    onClick = {
                        ringtone?.stop()
                        isPlaying = false
                        tapLog.add(0, "⏹️ RINGTONE STOPPED")
                        Log.d("MORSE_TAP", "Ringtone stopped manually")
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Stop, contentDescription = "Stop")
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(text = "$tapCount", style = MaterialTheme.typography.titleMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Speed, contentDescription = null, tint = if (consecutiveTapCount >= 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(text = "$consecutiveTapCount/$tapTriggerCount", style = MaterialTheme.typography.titleMedium, color = if (consecutiveTapCount >= 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (tapLog.isNotEmpty()) {
                Divider()
                Text("Recent activity", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(tapLog.take(10)) { logEntry ->
                        Text(logEntry, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                        Divider()
                    }
                }
            }
        }
    }
}
