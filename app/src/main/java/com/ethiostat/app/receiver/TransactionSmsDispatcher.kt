package com.ethiostat.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ethiostat.app.data.local.EthioStatDatabase
import com.ethiostat.app.data.parser.AmharicSmsParser
import com.ethiostat.app.data.parser.EnglishSmsParser
import com.ethiostat.app.data.parser.MultilingualSmsParser
import com.ethiostat.app.data.parser.OromoSmsParser
import com.ethiostat.app.data.parser.SmsLanguageDetector
import com.ethiostat.app.data.repository.EthioStatRepositoryImpl
import com.ethiostat.app.domain.model.AccountSourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Primary SMS broadcast receiver for all incoming financial SMS messages.
 *
 * Matches the SMS originating address against the [AccountSourceType] short-code registry.
 * Only SMS from known Ethiopian financial platforms (banks / mobile-money) are processed.
 * Unmatched SMS are silently ignored — the app never reads unrelated messages.
 *
 * Registered in AndroidManifest.xml with highest priority so it receives SMS before
 * the generic system inbox.
 *
 * This is separate from [SmsReceiver] which handles only the DEBUG_SMS test broadcast.
 */
class TransactionSmsDispatcher : BroadcastReceiver() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val timestamp = messages[0].timestampMillis

        // Check if this SMS is from a known financial platform
        val sourceType = AccountSourceType.fromShortCode(sender)
        if (sourceType == AccountSourceType.UNKNOWN) {
            android.util.Log.v("TransactionSmsDispatcher",
                "Ignored SMS from unknown sender: $sender")
            return
        }

        // Concatenate multi-part SMS
        val body = messages.joinToString("") { it.messageBody ?: "" }

        android.util.Log.d("TransactionSmsDispatcher",
            "Matched sender [$sender] → ${sourceType.displayName} | body length: ${body.length}")

        dispatchSms(context, sender, body, timestamp, sourceType)
    }

    private fun dispatchSms(
        context: Context,
        sender: String,
        body: String,
        timestamp: Long,
        sourceType: AccountSourceType
    ) {
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

                // Notify UI that new SMS was processed
                val updateIntent = Intent("com.ethiostat.app.SMS_PROCESSED").apply {
                    putExtra("sender", sender)
                    putExtra("timestamp", timestamp)
                    putExtra("sourceType", sourceType.name)
                }
                context.sendBroadcast(updateIntent)

                android.util.Log.d("TransactionSmsDispatcher",
                    "SMS processed: sender=$sender type=${sourceType.displayName}")

            } catch (e: Exception) {
                android.util.Log.e("TransactionSmsDispatcher",
                    "Failed to process SMS from $sender: ${e.message}", e)
            }
        }
    }
}
