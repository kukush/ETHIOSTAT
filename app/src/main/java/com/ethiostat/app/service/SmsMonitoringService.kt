package com.ethiostat.app.service

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.provider.Telephony
import com.ethiostat.app.receiver.SmsReceiver
import com.ethiostat.app.domain.repository.IEthioStatRepository
import kotlinx.coroutines.*

class SmsMonitoringService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var smsReceiver: SmsReceiver? = null
    private var monitoringJob: Job? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        startSmsMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startSmsMonitoring()
            ACTION_STOP_MONITORING -> stopSmsMonitoring()
            ACTION_REFRESH_DATA -> refreshTransactionData()
        }
        return START_STICKY
    }
    
    private fun startSmsMonitoring() {
        if (smsReceiver == null) {
            smsReceiver = SmsReceiver()
            val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
                priority = 1000
            }
            registerReceiver(smsReceiver, filter)
        }
        
        // Start periodic monitoring job
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Check for new SMS messages every 30 seconds
                    delay(30_000)
                    // Additional monitoring logic can be added here
                } catch (e: Exception) {
                    android.util.Log.e("SmsMonitoringService", "Monitoring error: ${e.message}")
                }
            }
        }
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
        monitoringJob?.cancel()
    }
    
    private fun refreshTransactionData() {
        serviceScope.launch {
            try {
                // Trigger manual refresh of transaction data
                val intent = Intent(ACTION_TRANSACTION_UPDATED)
                sendBroadcast(intent)
            } catch (e: Exception) {
                android.util.Log.e("SmsMonitoringService", "Refresh error: ${e.message}")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopSmsMonitoring()
        serviceScope.cancel()
    }
    
    companion object {
        const val ACTION_START_MONITORING = "com.ethiostat.app.START_SMS_MONITORING"
        const val ACTION_STOP_MONITORING = "com.ethiostat.app.STOP_SMS_MONITORING"
        const val ACTION_REFRESH_DATA = "com.ethiostat.app.REFRESH_TRANSACTION_DATA"
        const val ACTION_TRANSACTION_UPDATED = "com.ethiostat.app.TRANSACTION_UPDATED"
    }
}
