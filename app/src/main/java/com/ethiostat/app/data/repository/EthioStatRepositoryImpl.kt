package com.ethiostat.app.data.repository

import com.ethiostat.app.BuildConfig
import com.ethiostat.app.data.local.dao.*
import com.ethiostat.app.data.local.entity.*
import com.ethiostat.app.data.parser.MultilingualSmsParser
import com.ethiostat.app.domain.model.*
import com.ethiostat.app.domain.repository.AppConfig
import com.ethiostat.app.domain.repository.IEthioStatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EthioStatRepositoryImpl(
    private val balanceDao: BalanceDao,
    private val transactionDao: TransactionDao,
    private val configDao: ConfigDao,
    private val smsLogDao: SmsLogDao,
    private val smsParser: MultilingualSmsParser
) : IEthioStatRepository {
    
    override fun getBalances(): Flow<List<BalancePackage>> {
        return balanceDao.getAllBalances().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getTransactions(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return if (startDate == 0L && endDate == Long.MAX_VALUE) {
            transactionDao.getAllTransactions().map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            transactionDao.getTransactionsByDateRange(startDate, endDate).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }
    
    override fun getConfig(): Flow<AppConfig?> {
        return configDao.getConfig().map { it?.toDomain() }
    }
    
    override suspend fun getConfigOnce(): AppConfig? {
        return configDao.getConfigOnce()?.toDomain()
    }
    
    override suspend fun insertBalance(balance: BalancePackage) {
        balanceDao.insertBalance(balance.toEntity())
    }
    
    override suspend fun insertBalances(balances: List<BalancePackage>) {
        balanceDao.insertBalances(balances.map { it.toEntity() })
    }
    
    override suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.toEntity())
    }
    
    override suspend fun updateConfig(config: AppConfig) {
        configDao.updateConfig(config.toEntity())
    }
    
    override suspend fun updateLastReadTimestamp(timestamp: Long) {
        configDao.updateLastReadTimestamp(timestamp)
    }
    
    override suspend fun processSms(sender: String, body: String, timestamp: Long): ParsedSmsData {
        val config = getConfigOnce() ?: initializeDefaultConfig()
        
        if (!shouldProcessSms(sender, timestamp, config)) {
            return ParsedSmsData.empty()
        }
        
        val parsedData = try {
            smsParser.parse(body, sender)
        } catch (e: Exception) {
            logSms(sender, body, timestamp, false, e.message)
            return ParsedSmsData.error("Parse error: ${e.message}")
        }
        
        if (parsedData.isParsed) {
            // Delete old balances of same type+source before inserting fresh data
            parsedData.packages.forEach { pkg ->
                balanceDao.deleteByTypeAndSource(pkg.packageType.name, pkg.source)
            }
            parsedData.packages.forEach { insertBalance(it) }
            parsedData.transaction?.let { txn ->
                val isDuplicate = transactionDao.countBySourceAndTimestamp(txn.source, timestamp) > 0
                if (!isDuplicate) {
                    insertTransaction(txn.copy(timestamp = timestamp))
                }
            }
            updateLastReadTimestamp(timestamp)
            
            if (config.enableSmsLogging) {
                logSms(sender, body, timestamp, true, null)
            }
        } else {
            if (config.logUnparsedSms) {
                logSms(sender, body, timestamp, false, "Unable to parse")
            }
        }
        
        return parsedData
    }
    
    override suspend fun deleteOldTransactions(olderThan: Long) {
        transactionDao.deleteOldTransactions(olderThan)
    }
    
    override suspend fun deleteExpiredPackages() {
        balanceDao.deleteExpiredPackages()
    }
    
    private suspend fun shouldProcessSms(sender: String, timestamp: Long, config: AppConfig): Boolean {
        val telecomSenders = config.telecomSenders.split(",").map { it.trim() }
        val telebirrSenders = config.telebirrSenders.split(",").map { it.trim() }
        val bankSenders = config.bankSenders.split(",").map { it.trim() }
        
        val allSenders = telecomSenders + telebirrSenders + bankSenders
        val allowed = allSenders.any { configSender ->
            val normalized = configSender.replace("*", "").trim()
            normalized.isNotEmpty() && (sender.contains(normalized, ignoreCase = true) || normalized.contains(sender, ignoreCase = true))
        }
        
        android.util.Log.d("EthioStat", "shouldProcessSms: sender=$sender allowed=$allowed")
        return allowed
    }
    
    private suspend fun initializeDefaultConfig(): AppConfig {
        val defaultConfig = AppConfig(
            id = 1,
            telecomSenders = BuildConfig.DEFAULT_TELECOM_SENDER,
            telebirrSenders = BuildConfig.DEFAULT_TELEBIRR_SENDER,
            bankSenders = BuildConfig.DEFAULT_BANK_SENDERS,
            ussdBalanceCode = BuildConfig.DEFAULT_USSD_BALANCE,
            ussdPackagesCode = BuildConfig.DEFAULT_USSD_PACKAGES,
            ussdDataCheckCode = BuildConfig.DEFAULT_USSD_DATA_CHECK,
            appLanguage = BuildConfig.DEFAULT_LANGUAGE
        )
        configDao.insertConfig(defaultConfig.toEntity())
        return defaultConfig
    }
    
    private suspend fun logSms(sender: String, body: String, timestamp: Long, parsed: Boolean, error: String?) {
        smsLogDao.insertLog(
            SmsLogEntity(
                sender = sender,
                body = body,
                receivedAt = timestamp,
                parsed = parsed,
                errorMessage = error
            )
        )
    }
    
    override suspend fun getStoredMessagesBySenderSince(sender: String, fromTimestamp: Long): List<SmsLogEntity> {
        return smsLogDao.getMessagesBySenderSince(sender, fromTimestamp)
    }
    
    override suspend fun getLatestMessageBySender(sender: String): SmsLogEntity? {
        return smsLogDao.getLatestMessageBySender(sender)
    }
}
