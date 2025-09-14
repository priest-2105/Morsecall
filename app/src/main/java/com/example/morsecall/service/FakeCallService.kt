package com.example.morsecall.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.morsecall.R
import com.example.morsecall.loadFakeCallName
import com.example.morsecall.saveFakeCallName
import kotlinx.coroutines.*

class FakeCallService : Service() {
    
    companion object {
        private const val TAG = "FakeCallService"
        private var windowManager: WindowManager? = null
        private var overlayView: View? = null
        private var isShowing = false
        
        fun showFakeCall(context: Context) {
            if (isShowing) return
            
            val service = Intent(context, FakeCallService::class.java)
            ContextCompat.startForegroundService(context, service)
        }
        
        fun hideFakeCall(context: Context) {
            val service = Intent(context, FakeCallService::class.java)
            context.stopService(service)
        }
        
        fun isOverlayPermissionGranted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FakeCallService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "FakeCallService started")
        showFakeCallOverlay()
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun showFakeCallOverlay() {
        if (isShowing) return
        
        if (!isOverlayPermissionGranted(this)) {
            Log.e(TAG, "Overlay permission not granted")
            stopSelf()
            return
        }
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.fake_call_overlay, null)
        
        // Configure the overlay view
        setupOverlayView()
        
        // Set up window parameters
        val params = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                   WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                   WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                   WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                   WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP or Gravity.LEFT
            x = 0
            y = 0
        }
        
        try {
            windowManager?.addView(overlayView, params)
            isShowing = true
            Log.d(TAG, "Fake call overlay shown")
            
            // Auto-dismiss after 15 seconds
            serviceScope.launch {
                delay(15000)
                dismissOverlay()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
            stopSelf()
        }
    }
    
    private fun setupOverlayView() {
        overlayView?.let { view ->
            val callerNameText = view.findViewById<TextView>(R.id.caller_name)
            val acceptButton = view.findViewById<Button>(R.id.accept_button)
            val declineButton = view.findViewById<Button>(R.id.decline_button)
            
            // Set caller name from settings
            val callerName = loadFakeCallName(this)
            callerNameText.text = callerName
            
            // Set up button click listeners
            acceptButton.setOnClickListener {
                Log.d(TAG, "Call accepted")
                dismissOverlay()
            }
            
            declineButton.setOnClickListener {
                Log.d(TAG, "Call declined")
                dismissOverlay()
            }
            
            // Handle outside touch
            view.setOnTouchListener { _, _ ->
                Log.d(TAG, "Outside touch detected")
                dismissOverlay()
                true
            }
        }
    }
    
    private fun dismissOverlay() {
        if (!isShowing) return
        
        try {
            overlayView?.let { view ->
                windowManager?.removeView(view)
            }
            overlayView = null
            windowManager = null
            isShowing = false
            Log.d(TAG, "Fake call overlay dismissed")
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing overlay", e)
        } finally {
            stopSelf()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dismissOverlay()
        serviceScope.cancel()
        Log.d(TAG, "FakeCallService destroyed")
    }
}
