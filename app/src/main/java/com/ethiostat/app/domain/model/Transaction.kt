package com.ethiostat.app.domain.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val source: String,
    val description: String,
    val timestamp: Long
)

enum class TransactionType {
    INCOME,
    EXPENSE
}
