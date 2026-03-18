package com.ethiostat.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class UssdAccessibilityService : AccessibilityService() {
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.d("UssdAccessibility", "Service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Detect USSD dialog from phone/telecom packages
            if (isUssdDialog(packageName)) {
                Log.d("UssdAccessibility", "USSD dialog detected: $packageName")
                
                // Wait 2 seconds then auto-dismiss
                handler.postDelayed({
                    dismissUssdDialog()
                }, 2000)
            }
        }
    }
    
    private fun isUssdDialog(packageName: String): Boolean {
        return packageName.contains("phone", ignoreCase = true) ||
               packageName.contains("telecom", ignoreCase = true) ||
               packageName.contains("telephony", ignoreCase = true) ||
               packageName == "com.android.phone"
    }
    
    private fun dismissUssdDialog() {
        val rootNode = rootInActiveWindow ?: run {
            Log.w("UssdAccessibility", "No root node available")
            return
        }
        
        try {
            // Try to find and click "Cancel" or "OK" button
            val dismissed = findAndClickButton(rootNode, "Cancel") ||
                           findAndClickButton(rootNode, "OK") ||
                           findAndClickButton(rootNode, "Dismiss")
            
            if (dismissed) {
                Log.d("UssdAccessibility", "USSD dialog dismissed successfully")
            } else {
                // Fallback: try to press back button
                performGlobalAction(GLOBAL_ACTION_BACK)
                Log.d("UssdAccessibility", "Pressed back button as fallback")
            }
        } catch (e: Exception) {
            Log.e("UssdAccessibility", "Error dismissing dialog: ${e.message}")
        } finally {
            rootNode.recycle()
        }
    }
    
    private fun findAndClickButton(node: AccessibilityNodeInfo, text: String): Boolean {
        // Check if this node is the button we're looking for
        if (node.isClickable && 
            (node.text?.toString()?.contains(text, ignoreCase = true) == true ||
             node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true)) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        
        // Recursively search child nodes
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickButton(child, text)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        
        return false
    }
    
    override fun onInterrupt() {
        Log.d("UssdAccessibility", "Service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        Log.d("UssdAccessibility", "Service destroyed")
    }
}
