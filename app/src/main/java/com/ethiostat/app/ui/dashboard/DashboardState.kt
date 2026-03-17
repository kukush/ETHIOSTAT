package com.ethiostat.app.ui.dashboard

import com.ethiostat.app.domain.model.*

data class DashboardState(
    val balances: List<BalancePackage> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val financialSummary: FinancialSummary = FinancialSummary(),
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val selectedPeriod: TimePeriod = TimePeriod.WEEKLY,
    val isLoading: Boolean = false,
    val error: String? = null,
    val syncSuccess: Boolean = false
) {
    val internetPackages: List<BalancePackage>
        get() = balances.filter { it.packageType == PackageType.INTERNET }
    
    val voicePackages: List<BalancePackage>
        get() = balances.filter { it.packageType == PackageType.VOICE }
    
    val bonusFunds: List<BalancePackage>
        get() = balances.filter { it.packageType == PackageType.BONUS_FUND }
    
    val totalInternetMB: Double
        get() = internetPackages.sumOf { it.remainingAmount }
    
    val totalVoiceMinutes: Double
        get() = voicePackages.sumOf { it.remainingAmount }
    
    val totalBonusBirr: Double
        get() = bonusFunds.sumOf { it.remainingAmount }
}
