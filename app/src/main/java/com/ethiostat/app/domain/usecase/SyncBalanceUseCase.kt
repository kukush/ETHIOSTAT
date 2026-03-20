package com.ethiostat.app.domain.usecase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Handles USSD-based telecom data refresh (e.g., *804# for package/balance info).
 *
 * NOTE: SMS balance (*999*3*5#) has been removed — SMS count is now parsed directly
 * from the *804# response SMS message received from sender 251994.
 */
class SyncBalanceUseCase(
    private val context: Context
) {
    companion object {
        private const val TELECOM_USSD_CODE = "*804#"
    }

    // Specific method for telecom service data refresh (*804#)
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
                    if (continuation.isActive) {
                        continuation.resume(Result.success(returnMessage?.toString() ?: "Success"))
                    }
                }

                override fun onReceiveUssdResponseFailed(
                    telephonyManager: TelephonyManager?,
                    request: String?,
                    failureCode: Int
                ) {
                    super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode)
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("USSD request failed with code: $failureCode")))
                    }
                }
            }

            manager.sendUssdRequest(ussdCode, callback, handler)
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }
}
