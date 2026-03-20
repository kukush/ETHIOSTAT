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
import kotlinx.coroutines.flow.combine

class EthioStatRepositoryImpl(
    private val balanceDao: BalanceDao,
    private val transactionDao: TransactionDao,
    private val configDao: ConfigDao,
    private val smsLogDao: SmsLogDao,
    private val accountSourceDao: AccountSourceDao,
    private val smsMonitoringConfigDao: SmsMonitoringConfigDao,
    private val unreadMessageDao: UnreadMessageDao,
    private val lastReadSmsDao: LastReadSmsDao,
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
            android.util.Log.d("EthioStat", "processSms: sender=$sender skipped by allowlist")
            return ParsedSmsData.empty()
        }
        
        val parsedData = try {
            val result = smsParser.parse(body, sender)
            android.util.Log.d(
                "EthioStat",
                "processSms: sender=$sender parsed=${result.isParsed} packages=${result.packages.size} txn=${result.transaction != null}"
            )
            result
        } catch (e: Exception) {
            logSms(sender, body, timestamp, false, e.message)
            return ParsedSmsData.error("Parse error: ${e.message}")
        }
        
        if (parsedData.isParsed) {
            // Delete PRECISE balances of same name AND same type before inserting fresh data
            // This ensures "Account Balance" doesn't wipe out "Recharge Bonus"
            parsedData.packages.forEach { pkg ->
                android.util.Log.d("EthioStat", "Deleting existing balance for name=${pkg.packageName} type=${pkg.packageType}")
                balanceDao.deleteByNameAndType(pkg.packageName, pkg.packageType.name)
                
                android.util.Log.d("EthioStat", "Inserting balance pkg type=${pkg.packageType} name=${pkg.packageName} rem=${pkg.remainingAmount} total=${pkg.totalAmount} exp=${pkg.expiryDate}")
                insertBalance(pkg)
            }
            parsedData.transaction?.let { txn ->
                val isDuplicate = transactionDao.countBySourceAndTimestamp(txn.source, timestamp) > 0
                if (!isDuplicate) {
                    android.util.Log.d("EthioStat", "Inserting transaction amount=${txn.amount} type=${txn.type} source=${txn.source}")
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
        val fallbackTelecomSenders = listOf(
            "ethio", "ethio telecom", "telecom", "939", "999", "127", "251994", "cbe", "boa", "awash", "awashbank"
        )
        
        val allSenders = telecomSenders + telebirrSenders + bankSenders
        val allowedConfig = allSenders.any { configSender ->
            val normalized = configSender.replace("*", "").trim()
            normalized.isNotEmpty() && (sender.contains(normalized, ignoreCase = true) || normalized.contains(sender, ignoreCase = true))
        }
        val allowedFallback = fallbackTelecomSenders.any { sender.contains(it, ignoreCase = true) }
        val allowed = allowedConfig || allowedFallback
        
        android.util.Log.d("EthioStat", "shouldProcessSms: sender=$sender allowed=$allowed (config=$allowedConfig fallback=$allowedFallback)")
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
    
    // Account Source Management Implementation
    override fun getAccountSources(): Flow<List<AccountSource>> {
        return accountSourceDao.getAllAccountSources().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getEnabledAccountSources(): Flow<List<AccountSource>> {
        return accountSourceDao.getEnabledAccountSources().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun insertAccountSource(accountSource: AccountSource): Long {
        return accountSourceDao.insertAccountSource(accountSource.toEntity())
    }
    
    override suspend fun updateAccountSource(accountSource: AccountSource) {
        accountSourceDao.updateAccountSource(accountSource.toEntity())
    }
    
    override suspend fun deleteAccountSource(accountSource: AccountSource) {
        accountSourceDao.deleteAccountSource(accountSource.toEntity())
    }
    
    override suspend fun toggleAccountSourceEnabled(id: Long, isEnabled: Boolean) {
        accountSourceDao.updateAccountSourceEnabled(id, isEnabled)
    }
    
    // Last Read SMS Tracking Implementation
    override suspend fun getLastReadSmsTimestamp(phoneNumber: String): Long? {
        return lastReadSmsDao.getLastReadSms(phoneNumber)?.lastReadTimestamp
    }
    
    override suspend fun updateLastReadSmsTimestamp(phoneNumber: String, timestamp: Long) {
        val existing = lastReadSmsDao.getLastReadSms(phoneNumber)
        if (existing != null) {
            lastReadSmsDao.update(existing.copy(
                lastReadTimestamp = timestamp,
                updatedAt = System.currentTimeMillis()
            ))
        } else {
            lastReadSmsDao.insertOrUpdate(
                LastReadSmsEntity(
                    phoneNumber = phoneNumber,
                    lastReadTimestamp = timestamp
                )
            )
        }
    }
    
    // SMS Monitoring Configuration Implementation
    override fun getSmsMonitoringConfig(): Flow<SmsMonitoringConfig?> {
        return smsMonitoringConfigDao.getSmsMonitoringConfig().map { entity ->
            entity?.toDomain()
        }
    }
    
    override suspend fun getSmsMonitoringConfigOnce(): SmsMonitoringConfig? {
        return smsMonitoringConfigDao.getSmsMonitoringConfigOnce()?.toDomain()
    }
    
    override suspend fun updateSmsMonitoringConfig(config: SmsMonitoringConfig) {
        smsMonitoringConfigDao.updateSmsMonitoringConfig(config.toEntity())
    }
    
    override suspend fun updateShowNetBalance(showNetBalance: Boolean) {
        // TODO: Implement show net balance update
    }
    
    // Unread Messages Implementation
    override fun getUnreadMessages(): Flow<List<UnreadMessage>> {
        return unreadMessageDao.getUnreadMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getUnreadMessageCount(): Flow<UnreadMessageCount> {
        return combine(
            unreadMessageDao.getUnreadCount(),
            unreadMessageDao.getHighPriorityUnreadCount(),
            unreadMessageDao.getUrgentUnreadCount()
        ) { total: Int, highPriority: Int, urgent: Int ->
            UnreadMessageCount(
                totalCount = total,
                highPriorityCount = highPriority,
                urgentCount = urgent
            )
        }
    }
    
    override suspend fun markMessageAsRead(messageId: Long) {
        unreadMessageDao.markAsRead(messageId)
    }
    
    override suspend fun markAllMessagesAsRead() {
        unreadMessageDao.markAllAsRead()
    }
    
    override suspend fun insertUnreadMessage(message: UnreadMessage): Long {
        return unreadMessageDao.insertMessage(message.toEntity())
    }

    override suspend fun deleteTransactionsBySourceSince(accountSourceType: String, fromTimestamp: Long) {
        transactionDao.deleteTransactionsByAccountSourceSince(accountSourceType, fromTimestamp)
    }

    override suspend fun deleteAllTransactionsBySource(accountSourceType: String) {
        transactionDao.deleteTransactionsByAccountSource(accountSourceType)
    }
}
