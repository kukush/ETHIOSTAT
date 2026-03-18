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
            if (context != null) {
                val syncUseCase = SyncSmsContentUseCase(context, repository)
                val syncResult = syncUseCase.syncAllTelecomAndTelebirrMessages(30)
                syncResult.getOrNull()?.let {
                    val total = it.telecomProcessed + it.telebirrProcessed
                    android.util.Log.d("EthioStat", "Auto-sync completed: telecom=${it.telecomProcessed} telebirr=${it.telebirrProcessed} total=$total")
                } ?: android.util.Log.e("EthioStat", "Auto-sync failed: ${syncResult.exceptionOrNull()?.message}")
            }
            Result.success(emptyList())
        } catch (e: Exception) {
            android.util.Log.e("EthioStat", "ReadStoredSmsUseCase error: ${e.message}")
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
