package com.ethiostat.app.domain.usecase

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Handles USSD-based telecom data refresh (e.g., *804# for package/balance info).
 *
 * **Text capture – Path 1**: [TelephonyManager.sendUssdRequest] triggers
 * [TelephonyManager.UssdResponseCallback.onReceiveUssdResponse] with the carrier popup text.
 * That text is broadcast locally via [LocalBroadcastManager] with action
 * `"com.ethiostat.app.USSD_RESPONSE_RECEIVED"` and extra `"ussd_text"`.
 * [DashboardViewModel] receives this broadcast and updates [DashboardState.ussdResponseText].
 *
 * **Text capture – Path 2**: [UssdAccessibilityService] (fallback) reads the node tree
 * of the popup dialog window and sends the same broadcast when Path 1 returns empty text.
 *
 * NOTE: SMS balance (*999*3*5#) has been removed — SMS count is now parsed directly
 * from the *804# response SMS message received from sender 251994.
 */
class SyncBalanceUseCase(
    private val context: Context
) {
    companion object {
        private const val TELECOM_USSD_CODE = "*804#"
        private const val ACTION_USSD_RESPONSE = "com.ethiostat.app.USSD_RESPONSE_RECEIVED"
    }

    /** Trigger *804# — typically causes Ethio Telecom to send an SMS with balance info. */
    suspend fun refreshTelecomData(): Result<String> {
        return sendDirectUssdRequest(TELECOM_USSD_CODE)
    }

    @RequiresPermission(Manifest.permission.CALL_PHONE)
    suspend fun sendDirectUssdRequest(ussdCode: String): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
                continuation.resume(Result.failure(SecurityException("CALL_PHONE permission not granted")))
                return@suspendCancellableCoroutine
            }

            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val handler = Handler(Looper.getMainLooper())

            val callback = object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(
                    telephonyManager: TelephonyManager?,
                    request: String?,
                    returnMessage: CharSequence?
                ) {
                    super.onReceiveUssdResponse(telephonyManager, request, returnMessage)
                    val text = returnMessage?.toString() ?: ""

                    // Path 1: broadcast popup text for the dashboard to display
                    if (text.isNotBlank()) {
                        android.util.Log.d("SyncBalanceUseCase", "USSD response text: $text")
                        val broadcastIntent = Intent(ACTION_USSD_RESPONSE).apply {
                            putExtra("ussd_text", text)
                        }
                        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent)
                    } else {
                        android.util.Log.w("SyncBalanceUseCase",
                            "USSD callback returned empty text — Path 2 (AccessibilityService) may capture it")
                    }

                    if (continuation.isActive) {
                        continuation.resume(Result.success(text.ifBlank { "Success (no popup text)" }))
                    }
                }

                override fun onReceiveUssdResponseFailed(
                    telephonyManager: TelephonyManager?,
                    request: String?,
                    failureCode: Int
                ) {
                    super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode)
                    android.util.Log.e("SyncBalanceUseCase",
                        "USSD request failed for $request with code: $failureCode")
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("USSD request failed with code: $failureCode")))
                    }
                }
            }

            manager.sendUssdRequest(ussdCode, callback, handler)
        } catch (e: Exception) {
            android.util.Log.e("SyncBalanceUseCase", "sendUssdRequest exception: ${e.message}", e)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }
}

