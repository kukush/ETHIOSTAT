package com.ethiostat.app.domain.usecase

import android.content.Context
import com.ethiostat.app.domain.model.ParsedSmsData
import com.ethiostat.app.domain.repository.IEthioStatRepository
import java.util.concurrent.TimeUnit

class ReadStoredSmsUseCase(
    private val repository: IEthioStatRepository,
    private val context: Context? = null
) {
    
    suspend operator fun invoke(): Result<List<ParsedSmsData>> {
        return try {
            val results = mutableListOf<ParsedSmsData>()
            
            // First, try to sync with ContentResolver if available
            context?.let { ctx ->
                val syncUseCase = SyncSmsContentUseCase(ctx, repository)
                syncUseCase.syncAllTelecomAndTelebirrMessages(7)
            }
            
            // Read telebirr financial messages from last 7 days (from local database)
            val oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            val telebirrMessages = repository.getStoredMessagesBySenderSince("*830*", oneWeekAgo)
            
            telebirrMessages.forEach { smsLog ->
                if (!smsLog.parsed) { // Only process if not already parsed
                    val parsed = repository.processSms(smsLog.sender, smsLog.body, smsLog.receivedAt)
                    if (parsed.isParsed) {
                        results.add(parsed)
                    }
                }
            }
            
            // Read latest telecom balance message
            val telecomMessage = repository.getLatestMessageBySender("251994")
            telecomMessage?.let { smsLog ->
                if (!smsLog.parsed) { // Only process if not already parsed
                    val parsed = repository.processSms(smsLog.sender, smsLog.body, smsLog.receivedAt)
                    if (parsed.isParsed) {
                        results.add(parsed)
                    }
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun readTelebirrMessages(daysBack: Int = 7): Result<List<ParsedSmsData>> {
        return try {
            val results = mutableListOf<ParsedSmsData>()
            val fromTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysBack.toLong())
            
            val messages = repository.getStoredMessagesBySenderSince("*830*", fromTimestamp)
            
            messages.forEach { smsLog ->
                val parsed = repository.processSms(smsLog.sender, smsLog.body, smsLog.receivedAt)
                if (parsed.isParsed) {
                    results.add(parsed)
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun readLatestTelecomMessage(): Result<ParsedSmsData?> {
        return try {
            val smsLog = repository.getLatestMessageBySender("251994")
            val result = smsLog?.let { 
                repository.processSms(it.sender, it.body, it.receivedAt)
            }
            
            Result.success(result?.takeIf { it.isParsed })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
