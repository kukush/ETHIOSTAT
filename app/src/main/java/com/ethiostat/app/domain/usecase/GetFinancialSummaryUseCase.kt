package com.ethiostat.app.domain.usecase

import com.ethiostat.app.domain.model.FinancialSummary
import com.ethiostat.app.domain.model.TimePeriod
import com.ethiostat.app.domain.model.Transaction
import com.ethiostat.app.domain.model.TransactionType
import java.util.concurrent.TimeUnit

class GetFinancialSummaryUseCase {
    
    operator fun invoke(transactions: List<Transaction>, period: TimePeriod = TimePeriod.WEEKLY): FinancialSummary {
        val filteredTransactions = filterByPeriod(transactions, period)
        
        val totalIncome = filteredTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        
        val totalExpense = filteredTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        return FinancialSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = totalIncome - totalExpense,
            period = period
        )
    }
    
    private fun filterByPeriod(transactions: List<Transaction>, period: TimePeriod): List<Transaction> {
        val currentTime = System.currentTimeMillis()
        val startTime = when (period) {
            TimePeriod.DAILY -> currentTime - TimeUnit.DAYS.toMillis(1)
            TimePeriod.WEEKLY -> currentTime - TimeUnit.DAYS.toMillis(7)
            TimePeriod.MONTHLY -> currentTime - TimeUnit.DAYS.toMillis(30)
            TimePeriod.ALL_TIME -> 0L
        }
        
        return transactions.filter { it.timestamp >= startTime }
    }
}
