package com.ethiostat.app.domain.usecase

import com.ethiostat.app.domain.model.AccountSource
import com.ethiostat.app.domain.model.Transaction
import com.ethiostat.app.domain.repository.IEthioStatRepository
import com.ethiostat.app.data.sms.SmsContentReaderService
import android.content.Context
import java.util.concurrent.TimeUnit

class ProcessNewAccountSourceUseCase(
    private val repository: IEthioStatRepository,
    private val context: Context
) {
    
    suspend operator fun invoke(accountSource: AccountSource): Result<ProcessingResult> {
        return try {
            // Insert the new account source
            val sourceId = repository.insertAccountSource(accountSource)
            
            // Read SMS history from the past week
            val contentReader = SmsContentReaderService(context)
            val oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            
            // Search for messages from this source's phone number
            val messages = contentReader.readMessagesBySenderSince(
                accountSource.phoneNumber, 
                oneWeekAgo
            )
            
            var processedCount = 0
            val processedTransactions = mutableListOf<Transaction>()
            
            // Process each message found
            for (message in messages) {
                try {
                    val parsedData = repository.processSms(
                        message.sender,
                        message.body,
                        message.receivedAt
                    )
                    
                    if (parsedData.isParsed) {
                        parsedData.transaction?.let { transaction ->
                            // Update transaction with the new account source
                            val updatedTransaction = transaction.copy(
                                accountSource = accountSource.type,
                                sourcePhoneNumber = accountSource.phoneNumber,
                                isClassified = true
                            )
                            
                            // Check if transaction already exists to avoid duplicates
                            repository.insertTransaction(updatedTransaction)
                            processedTransactions.add(updatedTransaction)
                            processedCount++
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ProcessNewAccountSource", "Error processing message: ${e.message}")
                }
            }
            
            Result.success(
                ProcessingResult(
                    accountSourceId = sourceId,
                    messagesFound = messages.size,
                    transactionsProcessed = processedCount,
                    transactions = processedTransactions
                )
            )
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    data class ProcessingResult(
        val accountSourceId: Long,
        val messagesFound: Int,
        val transactionsProcessed: Int,
        val transactions: List<Transaction>
    )
}
