package com.example.morsecall

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// SharedPreferences keys
private const val KEY_RINGTONE_URI = "ringtone_uri"
private const val KEY_DOT_DURATION = "dot_duration"
private const val KEY_DASH_DURATION = "dash_duration"
private const val KEY_PAUSE_DURATION = "pause_duration"
private const val KEY_TAP_TRIGGER_COUNT = "tap_trigger_count"

// Helper functions for SharedPreferences
fun saveRingtoneUri(context: Context, uri: Uri?) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_RINGTONE_URI, uri?.toString()).apply()
    Log.d("SettingsScreen", "Saved ringtone URI: $uri")
}

fun loadRingtoneUri(context: Context): Uri? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val uriString = prefs.getString(KEY_RINGTONE_URI, null)
    return uriString?.let { Uri.parse(it) }
}

fun getRingtoneTitle(context: Context, uri: Uri?): String {
    if (uri == null) return "Default Ringtone"
    return try {
        val ringtone: Ringtone? = RingtoneManager.getRingtone(context, uri)
        ringtone?.getTitle(context) ?: "Unknown Ringtone"
    } catch (e: Exception) {
        Log.e("SettingsScreen", "Error getting ringtone title", e)
        "Error loading title"
    }
}

fun saveDotDuration(context: Context, duration: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_DOT_DURATION, duration).apply()
    Log.d("SettingsScreen", "Saved dot duration: $duration ms")
}

fun loadDotDuration(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_DOT_DURATION, 250) // Default 250ms
}

fun saveDashDuration(context: Context, duration: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_DASH_DURATION, duration).apply()
    Log.d("SettingsScreen", "Saved dash duration: $duration ms")
}

fun loadDashDuration(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_DASH_DURATION, 750) // Default 750ms
}

fun savePauseDuration(context: Context, duration: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_PAUSE_DURATION, duration).apply()
    Log.d("SettingsScreen", "Saved pause duration: $duration ms")
}

fun loadPauseDuration(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_PAUSE_DURATION, 500) // Default 500ms
}

fun saveTapTriggerCount(context: Context, count: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_TAP_TRIGGER_COUNT, count).apply()
    Log.d("SettingsScreen", "Saved tap trigger count: $count")
}

fun loadTapTriggerCount(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_TAP_TRIGGER_COUNT, 2) // Default 2 taps
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    
    var selectedRingtoneUri by remember { mutableStateOf(loadRingtoneUri(context)) }
    var selectedRingtoneTitle by remember { mutableStateOf(getRingtoneTitle(context, selectedRingtoneUri)) }
    var dotDuration by remember { mutableStateOf(loadDotDuration(context)) }
    var dashDuration by remember { mutableStateOf(loadDashDuration(context)) }
    var pauseDuration by remember { mutableStateOf(loadPauseDuration(context)) }
    var tapTriggerCount by remember { mutableStateOf(loadTapTriggerCount(context)) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            selectedRingtoneUri = uri
            saveRingtoneUri(context, uri)
            selectedRingtoneTitle = getRingtoneTitle(context, uri)
            Log.d("SettingsScreen", "Selected ringtone URI: $uri")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configure your preferences",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Ringtone Setting Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Ringtone")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedRingtoneUri)
                    }
                    ringtonePickerLauncher.launch(intent)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ringtone", style = MaterialTheme.typography.titleMedium)
                        Text(
                            selectedRingtoneTitle, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight, 
                        contentDescription = "Select Ringtone"
                    )
                }
            }

            // Tap Sensitivity Section
            Text(
                text = "Tap Sensitivity",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Simple Sensitivity Slider
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = when {
                            dotDuration <= 150 -> "Very Fast"
                            dotDuration <= 250 -> "Fast"
                            dotDuration <= 350 -> "Normal"
                            dotDuration <= 450 -> "Slow"
                            else -> "Very Slow"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = dotDuration.toFloat(),
                        onValueChange = { 
                            val newDotDuration = it.toInt()
                            dotDuration = newDotDuration
                            dashDuration = newDotDuration * 3 // Auto-calculate dash
                            saveDotDuration(context, dotDuration)
                            saveDashDuration(context, dashDuration)
                        },
                        valueRange = 100f..500f,
                        steps = 39
                    )
                    Text(
                        text = "Adjust how sensitive the app is to your tapping speed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Tap Trigger Count Section
            Text(
                text = "Ringtone Trigger",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Tap Trigger Count Slider
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Taps to trigger ringtone: $tapTriggerCount",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = tapTriggerCount.toFloat(),
                        onValueChange = { 
                            tapTriggerCount = it.toInt()
                            saveTapTriggerCount(context, tapTriggerCount)
                        },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    Text(
                        text = "Number of consecutive taps needed to play ringtone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
