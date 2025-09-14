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
import com.example.morsecall.service.FakeCallService
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.Settings
import android.content.Intent

// Define navigation routes
object AppDestinations {
    const val MAIN_SCREEN = "main"
    const val SETTINGS_SCREEN = "settings"
}

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
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
        
        // Request overlay permission for fake call
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
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
    
    // Check if accessibility service is enabled and update UI accordingly
    LaunchedEffect(Unit) {
        while (true) {
            val currentServiceEnabled = MorsecallServiceManager.isAccessibilityServiceEnabled(context)
            if (currentServiceEnabled != isServiceEnabled) {
                isServiceEnabled = currentServiceEnabled
                Log.d("MainScreen", "Service enabled state changed: $isServiceEnabled")
                
                // If service is disabled, deactivate the app
                if (!isServiceEnabled && isActive) {
                    isActive = false
                    val service = MorsecallServiceManager.getServiceInstance()
                    service?.setActive(false)
                    tapLog.add(0, "⚠️ Service disabled - deactivating")
                }
            }
            
            // Update tap counts from service
            val service = MorsecallServiceManager.getServiceInstance()
            if (service != null) {
                // Sync the active state from service
                val serviceActive = service.isServiceActive()
                if (serviceActive != isActive) {
                    isActive = serviceActive
                    Log.d("MainScreen", "Service active state synced: $isActive")
                }
                
                if (isActive) {
                    tapCount = service.getTapCount()
                    consecutiveTapCount = service.getConsecutiveTapCount()
                }
            }
            
            kotlinx.coroutines.delay(1000) // Update every second
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Welcome Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Morsecall",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "System-wide tap detection for emergency calls",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Service Status Card
            if (!isServiceEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Accessibility Service Required",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enable Morsecall in Accessibility Settings to detect taps system-wide",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                MorsecallServiceManager.openAccessibilitySettings(context)
                                tapLog.add(0, "🔧 Opening Accessibility Settings")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text("Open Settings", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        }
                    }
                }
            }

            // Activation Control Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        !isServiceEnabled -> MaterialTheme.colorScheme.surfaceVariant
                        isActive -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Power Button
                    FilledTonalIconButton(
                        onClick = { 
                            if (!isServiceEnabled) {
                                tapLog.add(0, "⚠️ Enable accessibility service first")
                                return@FilledTonalIconButton
                            }
                            
                            val service = MorsecallServiceManager.getServiceInstance()
                            if (service == null) {
                                tapLog.add(0, "⚠️ Accessibility service not connected - please restart app")
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
                        modifier = Modifier.size(100.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = when {
                                !isServiceEnabled -> MaterialTheme.colorScheme.surfaceVariant
                                isActive -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            contentColor = when {
                                !isServiceEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                isActive -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                        enabled = isServiceEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Power,
                            contentDescription = when {
                                !isServiceEnabled -> "Enable accessibility service first"
                                isActive -> "Deactivate"
                                else -> "Activate"
                            },
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Status Text
                    Text(
                        text = when {
                            !isServiceEnabled -> "Service Not Enabled"
                            isActive -> "Service Active"
                            else -> "Service Ready"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            !isServiceEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                            isActive -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    
                    Text(
                        text = when {
                            !isServiceEnabled -> "Enable accessibility service to start"
                            isActive -> "Tap detection is running system-wide"
                            else -> "Ready to activate tap detection"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            !isServiceEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            isActive -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Tap Counter Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Tap Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Total Taps
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$tapCount",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = "Total Taps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Consecutive Taps
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (consecutiveTapCount >= 1) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Speed,
                                    contentDescription = null,
                                    tint = if (consecutiveTapCount >= 1) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$consecutiveTapCount/$tapTriggerCount",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (consecutiveTapCount >= 1) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = "Consecutive",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (consecutiveTapCount >= 1) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Activity Log Card
            if (tapLog.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recent Activity",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(tapLog.take(8)) { logEntry ->
                                Text(
                                    text = logEntry,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
