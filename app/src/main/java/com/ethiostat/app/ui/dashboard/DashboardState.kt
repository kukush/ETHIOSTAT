package com.ethiostat.app.ui.dashboard

import com.ethiostat.app.domain.model.*
import com.ethiostat.app.domain.usecase.TransactionSummary

data class DashboardState(
    val balances: List<BalancePackage> = BalancePackageFactory.createDefaultZeroBalances(),
    val transactions: List<Transaction> = emptyList(),
    val financialSummary: FinancialSummary = FinancialSummary(),
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val selectedPeriod: TimePeriod = TimePeriod.WEEKLY,
    val selectedSourceFilter: AccountSourceType? = null,
    val selectedAccountSource: AccountSource? = null,
    val showNetBalance: Boolean = true,
    val isSmsMonitoringActive: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val syncSuccess: Boolean = false,
    val hasRealData: Boolean = false,
    val transactionSummaryBySource: Map<AccountSourceType, TransactionSummary> = emptyMap(),
    val accountSources: List<AccountSource> = emptyList(),
    val showAccountSourcesScreen: Boolean = false,
    val unreadMessageCount: UnreadMessageCount = UnreadMessageCount(0, 0, 0),
    val unreadMessages: List<UnreadMessage> = emptyList()
) {
    val internetPackages: List<BalancePackage>
        get() = balances.filter { it.packageType == PackageType.INTERNET }
    
    val voicePackages: List<BalancePackage>
        get() = balances.filter { it.packageType == PackageType.VOICE }
    
    val bonusFunds: List<BalancePackage>
        get() = balances.filter { it.packageType == PackageType.BONUS_FUND }
    
    val smsPackages: List<BalancePackage>
        get() = balances.filter { it.packageType == PackageType.SMS }
    
    val totalInternetMB: Double
        get() = internetPackages.sumOf { it.remainingAmount }
    
    val totalVoiceMinutes: Double
        get() = voicePackages.sumOf { it.remainingAmount }
    
    val totalBonusBirr: Double
        get() = bonusFunds.sumOf { it.remainingAmount }
}
