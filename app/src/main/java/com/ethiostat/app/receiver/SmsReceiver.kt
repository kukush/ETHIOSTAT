package com.ethiostat.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.ethiostat.app.data.local.EthioStatDatabase
import com.ethiostat.app.data.parser.AmharicSmsParser
import com.ethiostat.app.data.parser.EnglishSmsParser
import com.ethiostat.app.data.parser.MultilingualSmsParser
import com.ethiostat.app.data.parser.OromoSmsParser
import com.ethiostat.app.data.parser.SmsLanguageDetector
import com.ethiostat.app.data.repository.EthioStatRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        
        // Concatenate multi-part SMS messages
        if (messages.isNotEmpty()) {
            val sender = messages[0].originatingAddress ?: return
            val timestamp = messages[0].timestampMillis
            
            android.util.Log.d("EthioStat", "SmsReceiver: Received ${messages.size} message parts from $sender")
            
            val fullBody = messages.joinToString("") { it.messageBody ?: "" }
            
            android.util.Log.d("EthioStat", "SmsReceiver: Concatenated body length: ${fullBody.length}")
            android.util.Log.d("EthioStat", "SmsReceiver: Full body: ${fullBody.take(500)}")
            
            processSmsMessage(context, sender, fullBody, timestamp)
        }
    }
    
    private fun processSmsMessage(context: Context, sender: String, body: String, timestamp: Long) {
        scope.launch {
            try {
                val database = EthioStatDatabase.getDatabase(context)
                val repository = EthioStatRepositoryImpl(
                    balanceDao = database.balanceDao(),
                    transactionDao = database.transactionDao(),
                    configDao = database.configDao(),
                    smsLogDao = database.smsLogDao(),
                    accountSourceDao = database.accountSourceDao(),
                    smsMonitoringConfigDao = database.smsMonitoringConfigDao(),
                    unreadMessageDao = database.unreadMessageDao(),
                    lastReadSmsDao = database.lastReadSmsDao(),
                    smsParser = MultilingualSmsParser(
                        languageDetector = SmsLanguageDetector(),
                        englishParser = EnglishSmsParser(),
                        amharicParser = AmharicSmsParser(),
                        oromoParser = OromoSmsParser()
                    )
                )
                
                repository.processSms(sender, body, timestamp)
                
                // Broadcast update to refresh UI for both telecom and transaction data
                val updateIntent = Intent("com.ethiostat.app.SMS_PROCESSED").apply {
                    putExtra("sender", sender)
                    putExtra("timestamp", timestamp)
                }
                context.sendBroadcast(updateIntent)
                android.util.Log.d("EthioStat", "Broadcast SMS_PROCESSED for sender: $sender")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
