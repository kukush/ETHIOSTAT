package com.ethiostat.app.domain.model

data class FinancialSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = totalIncome - totalExpense,
    val period: TimePeriod = TimePeriod.WEEKLY
)

enum class TimePeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    ALL_TIME
}
