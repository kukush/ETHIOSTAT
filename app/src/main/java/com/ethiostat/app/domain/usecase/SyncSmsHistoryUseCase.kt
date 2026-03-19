package com.ethiostat.app.domain.usecase

import android.content.Context
import com.ethiostat.app.data.local.entity.SmsLogEntity
import com.ethiostat.app.data.sms.SmsContentReaderService
import com.ethiostat.app.domain.model.PackageType
import com.ethiostat.app.domain.repository.IEthioStatRepository
import java.util.concurrent.TimeUnit

class SyncSmsHistoryUseCase(
    private val context: Context,
    private val repository: IEthioStatRepository
) {
    private val contentReader = SmsContentReaderService(context)

    suspend operator fun invoke(daysBack: Int = 30): Result<Int> {
        return try {
            if (!contentReader.hasReadSmsPermission()) {
                return Result.failure(SecurityException("READ_SMS permission not granted"))
            }

            val fromTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysBack.toLong())
            
            // We scan for common telecom/telebirr senders
            val targetSenders = listOf("251994", "127", "804", "ethiotel", "telebirr")
            
            var processedCount = 0
            
            // Collect all relevant messages from the system inbox
            val allMessages = mutableListOf<SmsLogEntity>()
            for (sender in targetSenders) {
                val msgs = contentReader.readMessagesBySenderSince(sender, fromTimestamp)
                allMessages.addAll(msgs)
            }
            
            // Sort by most recent first
            val sortedMessages = allMessages.sortedByDescending { it.timestamp }
            
            // Scan through messages until we find the latest account balance for Birr
            var foundMainBalance = false
            
            for (msg in sortedMessages) {
                val parsed = repository.processSms(msg.sender, msg.body, msg.timestamp)
                
                if (parsed.isParsed) {
                    processedCount++
                    
                    // If we found a MAIN_BALANCE, we can stop scanning for it (optional optimization)
                    if (parsed.packages.any { it.packageType == PackageType.MAIN_BALANCE }) {
                        foundMainBalance = true
                        // We continue a bit more to find other packages (Internet, Voice) 
                        // but specifically for the user's "Account Balance" fix, 
                        // the most recent one is now saved.
                    }
                }
                
                // Limit scanning to avoid heavy processing if they have thousands of messages
                if (processedCount > 50) break 
            }

            Result.success(processedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
