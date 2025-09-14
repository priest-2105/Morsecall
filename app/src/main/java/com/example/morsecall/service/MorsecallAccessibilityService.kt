package com.example.morsecall.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.morsecall.R
import com.example.morsecall.loadRingtoneUri
import com.example.morsecall.loadTapTriggerCount
import kotlinx.coroutines.*

class MorsecallAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "MorsecallAccessibility"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "morsecall_service_channel"
        
        var instance: MorsecallAccessibilityService? = null
            private set
        
        fun isServiceRunning(): Boolean = instance != null
        
        fun stopService(context: Context) {
            instance?.stopSelf()
        }
    }
    
    private var lastTapTime = 0L
    private var tapCount = 0
    private var consecutiveTapCount = 0
    private var isActive = false
    private var isPlaying = false
    private var ringtone: Ringtone? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        Log.d(TAG, "MorsecallAccessibilityService connected")
        
        // Configure the service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or 
                        AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                        AccessibilityEvent.TYPE_TOUCH_INTERACTION_START or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                   AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 0
            packageNames = null // Monitor all packages
        }
        serviceInfo = info
        
        // Initialize ringtone
        initializeRingtone()
        
        // Start foreground service
        startForegroundService()
        
        Log.d(TAG, "Service configured and started")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        ringtone?.stop()
        Log.d(TAG, "MorsecallAccessibilityService destroyed")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isActive) return
        
        event?.let { accessibilityEvent ->
            Log.d(TAG, "Accessibility event: ${accessibilityEvent.eventType}, Package: ${accessibilityEvent.packageName}")
            
            when (accessibilityEvent.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                AccessibilityEvent.TYPE_TOUCH_INTERACTION_START,
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                    handleTapEvent()
                }
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "MorsecallAccessibilityService interrupted")
    }
    
    private fun handleTapEvent() {
        val currentTime = System.currentTimeMillis()
        tapCount++
        
        // Check for consecutive taps (within 3 seconds)
        val timeSinceLastTap = if (lastTapTime != 0L) currentTime - lastTapTime else 0L
        if (timeSinceLastTap < 3000 && timeSinceLastTap > 0) {
            consecutiveTapCount++
            val tapTriggerCount = loadTapTriggerCount(this)
            
            if (consecutiveTapCount >= tapTriggerCount) {
                playRingtone()
                consecutiveTapCount = 0
                Log.d(TAG, "Ringtone triggered after $tapTriggerCount taps!")
            }
        } else {
            consecutiveTapCount = 1
        }
        
        lastTapTime = currentTime
        
        // Update notification with current tap count
        updateNotification()
        
        Log.d(TAG, "Tap detected: #$tapCount, Consecutive: $consecutiveTapCount")
    }
    
    private fun initializeRingtone() {
        val ringtoneUri = loadRingtoneUri(this)
        ringtone = ringtoneUri?.let { uri ->
            try {
                RingtoneManager.getRingtone(this, uri)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ringtone", e)
                null
            }
        }
    }
    
    private fun playRingtone() {
        if (!isPlaying) {
            ringtone?.play()
            isPlaying = true
            
            // Stop ringtone after 5 seconds
            serviceScope.launch {
                delay(5000)
                stopRingtone()
            }
            
            Log.d(TAG, "Ringtone started")
        }
    }
    
    private fun stopRingtone() {
        ringtone?.stop()
        isPlaying = false
        Log.d(TAG, "Ringtone stopped")
    }
    
    fun setActive(active: Boolean) {
        isActive = active
        if (!active) {
            stopRingtone()
            tapCount = 0
            consecutiveTapCount = 0
            lastTapTime = 0L
        }
        updateNotification()
        Log.d(TAG, "Service active state: $active")
    }
    
    fun isServiceActive(): Boolean = isActive
    
    fun getTapCount(): Int = tapCount
    
    fun getConsecutiveTapCount(): Int = consecutiveTapCount
    
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Morsecall Active")
            .setContentText("Tap detection is running")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Foreground service started with notification")
    }
    
    private fun updateNotification() {
        val tapTriggerCount = loadTapTriggerCount(this)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Morsecall Active")
            .setContentText("Taps: $tapCount | Consecutive: $consecutiveTapCount/$tapTriggerCount")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
