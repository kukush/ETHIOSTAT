package com.ethiostat.app.domain.repository

import com.ethiostat.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface IEthioStatRepository {
    
    fun getBalances(): Flow<List<BalancePackage>>
    
    fun getTransactions(startDate: Long = 0, endDate: Long = Long.MAX_VALUE): Flow<List<Transaction>>
    
    fun getConfig(): Flow<AppConfig?>
    
    suspend fun getConfigOnce(): AppConfig?
    
    suspend fun insertBalance(balance: BalancePackage)
    
    suspend fun insertBalances(balances: List<BalancePackage>)
    
    suspend fun insertTransaction(transaction: Transaction)
    
    suspend fun updateConfig(config: AppConfig)
    
    suspend fun updateLastReadTimestamp(timestamp: Long)
    
    suspend fun processSms(sender: String, body: String, timestamp: Long): ParsedSmsData
    
    suspend fun deleteOldTransactions(olderThan: Long)
    
    suspend fun deleteExpiredPackages()
}

data class AppConfig(
    val id: Int = 1,
    val lastReadTimestamp: Long = 0,
    val telecomSenders: String = "",
    val telebirrSenders: String = "",
    val bankSenders: String = "",
    val ussdBalanceCode: String = "",
    val ussdPackagesCode: String = "",
    val ussdDataCheckCode: String = "",
    val appLanguage: String = "en",
    val parseEnglishSms: Boolean = true,
    val parseAmharicSms: Boolean = true,
    val parseOromiffaSms: Boolean = false,
    val parseTigrinyaSms: Boolean = false,
    val showExpiredPackages: Boolean = true,
    val expiryWarningDays: Int = 3,
    val currencySymbol: String = "Birr",
    val enableSmsLogging: Boolean = false,
    val logUnparsedSms: Boolean = false
)
