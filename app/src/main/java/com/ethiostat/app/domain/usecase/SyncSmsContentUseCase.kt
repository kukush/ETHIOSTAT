package com.ethiostat.app.domain.usecase

import android.content.Context
import com.ethiostat.app.data.sms.SmsContentReaderService
import com.ethiostat.app.data.sms.SmsMessageFingerprint
import com.ethiostat.app.domain.model.ParsedSmsData
import com.ethiostat.app.domain.repository.IEthioStatRepository
import java.util.concurrent.TimeUnit

class SyncSmsContentUseCase(
    private val context: Context,
    private val repository: IEthioStatRepository
) {
    
    private val contentReader = SmsContentReaderService(context)
    
    suspend operator fun invoke(): Result<SyncResult> {
        return try {
            if (!contentReader.hasReadSmsPermission()) {
                return Result.failure(SecurityException("READ_SMS permission not granted"))
            }
            
            val result = SyncResult()
            
            // Sync telebirr financial messages from last 7 days
            val telebirrResult = syncTelebirrMessages(7)
            result.telebirrProcessed = telebirrResult.getOrDefault(0)
            
            // Sync latest telecom balance message
            val telecomResult = syncLatestTelecomMessage()
            result.telecomProcessed = if (telecomResult.getOrDefault(false)) 1 else 0
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncTelebirrMessages(daysBack: Int = 7): Result<Int> {
        return try {
            val fromTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysBack.toLong())
            val contentMessages = contentReader.readMessagesBySenderSince("*830*", fromTimestamp)
            
            var processedCount = 0
            
            for (contentMessage in contentMessages) {
                val fingerprint = SmsMessageFingerprint.create(
                    contentMessage.sender,
                    contentMessage.body,
                    contentMessage.receivedAt
                )
                
                // Check if we already have this message in our local database
                val existingMessages = repository.getStoredMessagesBySenderSince(
                    contentMessage.sender,
                    contentMessage.receivedAt - 1000 // 1 second tolerance
                )
                
                val isDuplicate = existingMessages.any { existing ->
                    val existingFingerprint = SmsMessageFingerprint.create(
                        existing.sender,
                        existing.body,
                        existing.receivedAt
                    )
                    SmsMessageFingerprint.isSimilar(fingerprint, existingFingerprint)
                }
                
                if (!isDuplicate) {
                    // Process the message from ContentResolver
                    val parsed = repository.processSms(
                        contentMessage.sender,
                        contentMessage.body,
                        contentMessage.receivedAt
                    )
                    
                    if (parsed.isParsed) {
                        processedCount++
                    }
                }
            }
            
            Result.success(processedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncLatestTelecomMessage(): Result<Boolean> {
        return try {
            val contentMessage = contentReader.readLatestMessageBySender("251994")
                ?: return Result.success(false)
            
            val fingerprint = SmsMessageFingerprint.create(
                contentMessage.sender,
                contentMessage.body,
                contentMessage.receivedAt
            )
            
            // Check if we already have this message
            val existingMessage = repository.getLatestMessageBySender("251994")
            val isDuplicate = existingMessage?.let { existing ->
                val existingFingerprint = SmsMessageFingerprint.create(
                    existing.sender,
                    existing.body,
                    existing.receivedAt
                )
                SmsMessageFingerprint.isSimilar(fingerprint, existingFingerprint)
            } ?: false
            
            if (!isDuplicate) {
                val parsed = repository.processSms(
                    contentMessage.sender,
                    contentMessage.body,
                    contentMessage.receivedAt
                )
                
                return Result.success(parsed.isParsed)
            }
            
            Result.success(false) // Already processed
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncAllTelecomAndTelebirrMessages(daysBack: Int = 7): Result<SyncResult> {
        return try {
            val result = SyncResult()
            val contentMessages = contentReader.readTelecomAndTelebirrMessages(daysBack)
            
            for (contentMessage in contentMessages) {
                val fingerprint = SmsMessageFingerprint.create(
                    contentMessage.sender,
                    contentMessage.body,
                    contentMessage.receivedAt
                )
                
                // Check for duplicates in local database
                val existingMessages = repository.getStoredMessagesBySenderSince(
                    contentMessage.sender,
                    contentMessage.receivedAt - 5000 // 5 second tolerance
                )
                
                val isDuplicate = existingMessages.any { existing ->
                    val existingFingerprint = SmsMessageFingerprint.create(
                        existing.sender,
                        existing.body,
                        existing.receivedAt
                    )
                    SmsMessageFingerprint.isSimilar(fingerprint, existingFingerprint)
                }
                
                if (!isDuplicate) {
                    val parsed = repository.processSms(
                        contentMessage.sender,
                        contentMessage.body,
                        contentMessage.receivedAt
                    )
                    
                    if (parsed.isParsed) {
                        when (contentMessage.sender) {
                            "251994" -> result.telecomProcessed++
                            "*830*", "830" -> result.telebirrProcessed++
                        }
                    }
                }
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    data class SyncResult(
        var telecomProcessed: Int = 0,
        var telebirrProcessed: Int = 0
    ) {
        val totalProcessed: Int
            get() = telecomProcessed + telebirrProcessed
    }
}
