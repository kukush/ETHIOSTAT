package com.ethiostat.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Accessibility service that captures USSD popup text from the carrier dialer.
 *
 * This is **Path 2** — a fallback for devices/carriers where
 * [TelephonyManager.sendUssdRequest] does not return the popup text via its callback.
 *
 * When a USSD dialog appears from the `com.android.phone` package:
 *  1. All visible text is harvested from the accessibility node tree.
 *  2. The text is broadcast locally as [ACTION_USSD_RESPONSE] with extra `"ussd_text"`.
 *  3. The dialog is then auto-dismissed after a short delay.
 */
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
        Log.d(TAG, "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        if (isUssdDialog(packageName)) {
            Log.d(TAG, "USSD dialog detected from package: $packageName")

            // Capture the text immediately — before any dismissal
            captureAndBroadcastUssdText()

            // Auto-dismiss after a brief pause to allow text extraction
            handler.postDelayed({ dismissUssdDialog() }, 2000)
        }
    }

    // ── Text Capture ──────────────────────────────────────────────────────────

    /**
     * Traverses all nodes in the active window and collects non-empty text values.
     * Broadcasts the concatenated text as [ACTION_USSD_RESPONSE].
     */
    private fun captureAndBroadcastUssdText() {
        val rootNode = rootInActiveWindow ?: run {
            Log.w(TAG, "No root node available for text capture")
            return
        }

        try {
            val builder = StringBuilder()
            collectNodeText(rootNode, builder)
            val capturedText = builder.toString().trim()

            if (capturedText.isNotEmpty()) {
                Log.d(TAG, "USSD text captured: $capturedText")
                val broadcastIntent = Intent(ACTION_USSD_RESPONSE).apply {
                    putExtra("ussd_text", capturedText)
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
            } else {
                Log.w(TAG, "USSD dialog contained no readable text")
            }
        } finally {
            rootNode.recycle()
        }
    }

    /** Recursively collects non-empty text from all child nodes. */
    private fun collectNodeText(node: AccessibilityNodeInfo, builder: StringBuilder) {
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty()) {
            if (builder.isNotEmpty()) builder.append(" ")
            builder.append(text)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectNodeText(child, builder)
            child.recycle()
        }
    }

    // ── Dialog Dismissal ──────────────────────────────────────────────────────

    private fun dismissUssdDialog() {
        val rootNode = rootInActiveWindow ?: run {
            Log.w(TAG, "No root node available for dismissal")
            return
        }

        try {
            val dismissed = findAndClickButton(rootNode, "Cancel") ||
                            findAndClickButton(rootNode, "OK")     ||
                            findAndClickButton(rootNode, "Dismiss")

            if (dismissed) {
                Log.d(TAG, "USSD dialog dismissed via button click")
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
                Log.d(TAG, "USSD dialog dismissed via back action")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing USSD dialog: ${e.message}")
        } finally {
            rootNode.recycle()
        }
    }

    private fun findAndClickButton(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.isClickable &&
            (node.text?.toString()?.contains(text, ignoreCase = true) == true ||
             node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true)) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
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

    // ── Package Detection ─────────────────────────────────────────────────────

    private fun isUssdDialog(packageName: String): Boolean =
        packageName.contains("phone", ignoreCase = true) ||
        packageName.contains("telecom", ignoreCase = true) ||
        packageName.contains("telephony", ignoreCase = true) ||
        packageName == "com.android.phone"

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Service destroyed")
    }

    companion object {
        private const val TAG = "UssdAccessibility"
        const val ACTION_USSD_RESPONSE = "com.ethiostat.app.USSD_RESPONSE_RECEIVED"
    }
}

