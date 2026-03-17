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
        
        for (smsMessage in messages) {
            processSmsMessage(context, smsMessage)
        }
    }
    
    private fun processSmsMessage(context: Context, smsMessage: SmsMessage) {
        val sender = smsMessage.originatingAddress ?: return
        val body = smsMessage.messageBody ?: return
        val timestamp = smsMessage.timestampMillis
        
        scope.launch {
            try {
                val database = EthioStatDatabase.getDatabase(context)
                val repository = EthioStatRepositoryImpl(
                    balanceDao = database.balanceDao(),
                    transactionDao = database.transactionDao(),
                    configDao = database.configDao(),
                    smsLogDao = database.smsLogDao(),
                    smsParser = MultilingualSmsParser(
                        languageDetector = SmsLanguageDetector(),
                        englishParser = EnglishSmsParser(),
                        amharicParser = AmharicSmsParser()
                    )
                )
                
                repository.processSms(sender, body, timestamp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
