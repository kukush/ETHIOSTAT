package com.ethiostat.app.domain.usecase

import com.ethiostat.app.domain.model.AccountSource
import com.ethiostat.app.domain.model.ParsedSmsData
import com.ethiostat.app.domain.repository.IEthioStatRepository
import java.util.concurrent.TimeUnit

class ReadTransactionSourceSmsUseCase(
    private val repository: IEthioStatRepository
) {
    
    suspend operator fun invoke(accountSources: List<AccountSource>): Result<List<ParsedSmsData>> {
        return try {
            val results = mutableListOf<ParsedSmsData>()
            val oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            
            // Get only enabled sources with phone numbers
            val enabledSources = accountSources.filter { it.isEnabled && it.phoneNumber.isNotBlank() }
            
            if (enabledSources.isEmpty()) {
                android.util.Log.d("EthioStat", "No enabled transaction sources with phone numbers")
                return Result.success(emptyList())
            }
            
            // Get last read timestamp for each source
            val lastReadTimestamps = mutableMapOf<String, Long>()
            enabledSources.forEach { source ->
                val lastRead = repository.getLastReadSmsTimestamp(source.phoneNumber)
                lastReadTimestamps[source.phoneNumber] = lastRead ?: oneWeekAgo
                android.util.Log.d("EthioStat", "Source ${source.displayName}: lastRead=${lastReadTimestamps[source.phoneNumber]}")
            }
            
            // Read SMS from all enabled sources since their last read time
            enabledSources.forEach { source ->
                val fromTimestamp = lastReadTimestamps[source.phoneNumber] ?: oneWeekAgo
                val messages = repository.getStoredMessagesBySenderSince(source.phoneNumber, fromTimestamp)
                
                android.util.Log.d("EthioStat", "Reading ${messages.size} messages from ${source.displayName} since ${fromTimestamp}")
                
                messages.forEach { smsLog ->
                    val parsed = repository.processSms(smsLog.sender, smsLog.body, smsLog.receivedAt)
                    if (parsed.isParsed) {
                        results.add(parsed)
                        
                        // Update last read timestamp for this source
                        repository.updateLastReadSmsTimestamp(source.phoneNumber, smsLog.receivedAt)
                        android.util.Log.d("EthioStat", "Updated last read timestamp for ${source.displayName} to ${smsLog.receivedAt}")
                    }
                }
            }
            
            android.util.Log.d("EthioStat", "Successfully processed ${results.size} SMS from transaction sources")
            Result.success(results)
            
        } catch (e: Exception) {
            android.util.Log.e("EthioStat", "ReadTransactionSourceSmsUseCase error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun readFromNewSource(accountSource: AccountSource): Result<List<ParsedSmsData>> {
        return invoke(listOf(accountSource))
    }
}
