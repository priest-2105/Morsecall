package com.example.morsecall.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager

object MorsecallServiceManager {
    
    private const val TAG = "MorsecallServiceManager"
    
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        
        val serviceName = "${context.packageName}/${MorsecallAccessibilityService::class.java.name}"
        Log.d(TAG, "Looking for service: $serviceName")
        Log.d(TAG, "Enabled services count: ${enabledServices.size}")
        
        enabledServices.forEach { serviceInfo ->
            Log.d(TAG, "Enabled service: ${serviceInfo.resolveInfo.serviceInfo.name}")
        }
        
        return enabledServices.any { serviceInfo ->
            serviceInfo.resolveInfo.serviceInfo.name == MorsecallAccessibilityService::class.java.name
        }
    }
    
    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Log.d(TAG, "Opened accessibility settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
        }
    }
    
    fun isServiceRunning(): Boolean {
        return MorsecallAccessibilityService.isServiceRunning()
    }
    
    fun getServiceInstance(): MorsecallAccessibilityService? {
        return MorsecallAccessibilityService.instance
    }
    
    fun stopService(context: Context) {
        MorsecallAccessibilityService.stopService(context)
    }
}
