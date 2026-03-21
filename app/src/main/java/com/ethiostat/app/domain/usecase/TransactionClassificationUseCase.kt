package com.ethiostat.app.domain.usecase

import com.ethiostat.app.domain.model.*
import com.ethiostat.app.domain.repository.IEthioStatRepository

class TransactionClassificationUseCase(
    private val repository: IEthioStatRepository
) {
    
    suspend fun classifyTransaction(
        transaction: Transaction,
        accountSources: List<AccountSource>
    ): Transaction {
        val sourcePhoneNumber = transaction.sourcePhoneNumber ?: return transaction
        
        // Find matching account source
        val matchingSource = accountSources.find { source ->
            source.phoneNumber.contains(sourcePhoneNumber, ignoreCase = true) ||
            sourcePhoneNumber.contains(source.phoneNumber, ignoreCase = true)
        }
        
        return if (matchingSource != null) {
            transaction.copy(
                accountSource = matchingSource.type,
                isClassified = true
            )
        } else {
            // Auto-classify based on phone number patterns
            val autoClassifiedType = autoClassifyByPhoneNumber(sourcePhoneNumber)
            transaction.copy(
                accountSource = autoClassifiedType,
                isClassified = autoClassifiedType != null
            )
        }
    }
    
    suspend fun classifyTransactionsBatch(
        transactions: List<Transaction>,
        accountSources: List<AccountSource>
    ): List<Transaction> {
        return transactions.map { transaction ->
            classifyTransaction(transaction, accountSources)
        }
    }
    
    fun getTransactionsBySource(
        transactions: List<Transaction>,
        sourceType: AccountSourceType
    ): List<Transaction> {
        return transactions.filter { it.accountSource == sourceType }
    }
    
    fun getTransactionSummaryBySource(
        transactions: List<Transaction>
    ): Map<AccountSourceType, TransactionSummary> {
        return transactions
            .filter { it.isClassified && it.accountSource != null }
            .groupBy { it.accountSource!! }
            .mapValues { (_, txns) ->
                TransactionSummary(
                    totalIncome = txns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    totalExpense = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                    transactionCount = txns.size,
                    lastTransactionTime = txns.maxOfOrNull { it.timestamp } ?: 0L
                )
            }
    }
    
    private fun autoClassifyByPhoneNumber(phoneNumber: String): AccountSourceType? {
        return when {
            phoneNumber.contains("830") || phoneNumber.contains("telebirr", ignoreCase = true) -> 
                AccountSourceType.TELEBIRR
            phoneNumber.contains("251994") || phoneNumber.contains("ethio", ignoreCase = true) -> 
                AccountSourceType.TELECOM
            phoneNumber.contains("cbe", ignoreCase = true) || phoneNumber.contains("commercial", ignoreCase = true) -> 
                AccountSourceType.CBE
            phoneNumber.contains("awash", ignoreCase = true) -> 
                AccountSourceType.BANK_AWASH
            else -> null
        }
    }
}

data class TransactionSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val transactionCount: Int,
    val lastTransactionTime: Long
) {
    val netBalance: Double get() = totalIncome - totalExpense
}
