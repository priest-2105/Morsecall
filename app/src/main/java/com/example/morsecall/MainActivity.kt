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
import com.example.morsecall.service.MorsecallServiceManager
import com.example.morsecall.service.NotificationChannelManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// Define navigation routes
object AppDestinations {
    const val MAIN_SCREEN = "main"
    const val SETTINGS_SCREEN = "settings"
}

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Create notification channel
        NotificationChannelManager.createNotificationChannel(this)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
        
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
    var isActive by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }
    var consecutiveTapCount by remember { mutableStateOf(0) }
    var isServiceEnabled by remember { mutableStateOf(false) }
    val tapLog = remember { mutableStateListOf<String>() }
    
    // Check if accessibility service is enabled
    LaunchedEffect(Unit) {
        isServiceEnabled = MorsecallServiceManager.isAccessibilityServiceEnabled(context)
    }
    
    // Load tap configuration settings
    val tapTriggerCount = remember { loadTapTriggerCount(context) }
    
    // Update tap counts from service and sync state
    LaunchedEffect(Unit) {
        while (true) {
            val service = MorsecallServiceManager.getServiceInstance()
            if (service != null) {
                // Sync the active state from service
                val serviceActive = service.isServiceActive()
                if (serviceActive != isActive) {
                    isActive = serviceActive
                }
                
                if (isActive) {
                    tapCount = service.getTapCount()
                    consecutiveTapCount = service.getConsecutiveTapCount()
                }
            }
            kotlinx.coroutines.delay(500) // Update every 500ms
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

            // Service Status Card
            if (!isServiceEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Accessibility Service Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enable Morsecall in Accessibility Settings to detect taps system-wide",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                MorsecallServiceManager.openAccessibilitySettings(context)
                                tapLog.add(0, "🔧 Opening Accessibility Settings")
                            }
                        ) {
                            Text("Open Settings")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Activate/Deactivate Button
            // Circular power toggle
            FilledTonalIconButton(
                onClick = { 
                    if (!isServiceEnabled) {
                        tapLog.add(0, "⚠️ Enable accessibility service first")
                        return@FilledTonalIconButton
                    }
                    
                    val service = MorsecallServiceManager.getServiceInstance()
                    if (service == null) {
                        tapLog.add(0, "⚠️ Accessibility service not connected")
                        return@FilledTonalIconButton
                    }
                    
                    isActive = !isActive
                    service.setActive(isActive)
                    
                    if (isActive) {
                        tapLog.add(0, "✅ Background Service ACTIVATED")
                        Log.d("MORSE_TAP", "Background service activated")
                    } else {
                        tapLog.add(0, "❌ Background Service DEACTIVATED")
                        Log.d("MORSE_TAP", "Background service deactivated")
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

            // Status Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                        contentDescription = "Tap Detection",
                        modifier = Modifier.size(48.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isActive) "System-wide tap detection active" else "Tap detection inactive",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap anywhere on your device to trigger ringtone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
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
