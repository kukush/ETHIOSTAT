package com.ethiostat.app.service

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.provider.Telephony
import com.ethiostat.app.receiver.SmsReceiver

/**
 * Keeps the [SmsReceiver] broadcast registration alive as a foreground-eligible service.
 *
 * Battery strategy: **event-driven only** — no polling loops.
 * The service registers [SmsReceiver] on start and unregisters on stop.
 * Between SMS events the process sleeps completely; no CPU wake-locks are held.
 *
 * The OS will restart this service after a kill because of [START_STICKY].
 */
class SmsMonitoringService : Service() {

    private var smsReceiver: SmsReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startSmsMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startSmsMonitoring()
            ACTION_STOP_MONITORING  -> stopSmsMonitoring()
            ACTION_REFRESH_DATA     -> refreshTransactionData()
        }
        return START_STICKY
    }

    private fun startSmsMonitoring() {
        if (smsReceiver != null) return   // already registered

        smsReceiver = SmsReceiver()
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            priority = 1000
        }
        registerReceiver(smsReceiver, filter)
        android.util.Log.d("SmsMonitoringService", "SMS receiver registered — listening for incoming messages")
    }

    private fun stopSmsMonitoring() {
        smsReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                android.util.Log.w("SmsMonitoringService", "Error unregistering receiver: ${e.message}")
            }
            smsReceiver = null
        }
        android.util.Log.d("SmsMonitoringService", "SMS receiver unregistered")
    }

    private fun refreshTransactionData() {
        val updateIntent = Intent(ACTION_TRANSACTION_UPDATED)
        sendBroadcast(updateIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSmsMonitoring()
    }

    companion object {
        const val ACTION_START_MONITORING    = "com.ethiostat.app.START_SMS_MONITORING"
        const val ACTION_STOP_MONITORING     = "com.ethiostat.app.STOP_SMS_MONITORING"
        const val ACTION_REFRESH_DATA        = "com.ethiostat.app.REFRESH_TRANSACTION_DATA"
        const val ACTION_TRANSACTION_UPDATED = "com.ethiostat.app.TRANSACTION_UPDATED"
    }
}
