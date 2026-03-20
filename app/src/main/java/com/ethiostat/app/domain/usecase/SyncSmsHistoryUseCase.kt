package com.ethiostat.app.domain.usecase

import android.content.Context
import com.ethiostat.app.data.local.entity.LastReadSmsEntity
import com.ethiostat.app.data.local.entity.SmsLogEntity
import com.ethiostat.app.data.sms.SmsContentReaderService
import com.ethiostat.app.domain.model.PackageType
import com.ethiostat.app.domain.repository.IEthioStatRepository
import java.util.concurrent.TimeUnit

/**
 * Scans the device SMS inbox for balance-related messages from Ethio Telecom (251994).
 *
 * FIRST-RUN BEHAVIOUR:
 *  - If no LastReadSmsEntity exists for "251994" in the DB → treat as first run.
 *    Scan the last 14 days (2 weeks). Save the latest read timestamp so the next
 *    call only scans new messages.
 *  - If an entry exists → scan from last read timestamp only (incremental scan).
 *
 * If no balance message is found, the balance defaults to 0 via BalancePackageFactory.
 */
class SyncSmsHistoryUseCase(
    private val context: Context,
    private val repository: IEthioStatRepository
) {
    companion object {
        private const val TELECOM_SENDER = "251994"
        private const val FIRST_RUN_DAYS_BACK = 14L   // 2 weeks
        private const val FALLBACK_DAYS_BACK = 1L     // incremental
        private const val MAX_MESSAGES_TO_PROCESS = 100
    }

    private val contentReader = SmsContentReaderService(context)

    /**
     * Invoke the history scan.
     *
     * @param forceFullScan  If true, ignore last-read timestamp and always scan
     *                       [FIRST_RUN_DAYS_BACK] days regardless (useful for manual refresh).
     */
    suspend operator fun invoke(forceFullScan: Boolean = false): Result<Int> {
        return try {
            if (!contentReader.hasReadSmsPermission()) {
                return Result.failure(SecurityException("READ_SMS permission not granted"))
            }

            // Determine scan window
            val lastReadEntry = repository.getLastReadSmsTimestamp(TELECOM_SENDER)
            val isFirstRun = lastReadEntry == null || lastReadEntry == 0L

            val fromTimestamp = when {
                forceFullScan || isFirstRun ->
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(FIRST_RUN_DAYS_BACK)
                else -> lastReadEntry!! // incremental — scan only since last read
            }

            android.util.Log.d(
                "EthioStat",
                "SyncSmsHistoryUseCase: isFirstRun=$isFirstRun forceFullScan=$forceFullScan scanning from=$fromTimestamp"
            )

            // Target senders for telecom balance/package info
            val targetSenders = listOf(TELECOM_SENDER, "804", "ethiotel", "telebirr")

            val allMessages = mutableListOf<SmsLogEntity>()
            for (sender in targetSenders) {
                val msgs = contentReader.readMessagesBySenderSince(sender, fromTimestamp)
                allMessages.addAll(msgs)
            }

            // Sort: most recent first
            val sortedMessages = allMessages.sortedByDescending { it.receivedAt }

            android.util.Log.d(
                "EthioStat",
                "SyncSmsHistoryUseCase: found ${sortedMessages.size} messages to process"
            )

            var processedCount = 0
            var newestTimestamp = fromTimestamp

            for (msg in sortedMessages) {
                val parsed = repository.processSms(msg.sender, msg.body, msg.receivedAt)

                if (parsed.isParsed) {
                    processedCount++
                }
                if (msg.receivedAt > newestTimestamp) {
                    newestTimestamp = msg.receivedAt
                }

                if (processedCount >= MAX_MESSAGES_TO_PROCESS) {
                    android.util.Log.d("EthioStat", "SyncSmsHistoryUseCase: hit max messages limit ($MAX_MESSAGES_TO_PROCESS)")
                    break
                }
            }

            // Save the newest processed timestamp so next call is incremental
            repository.updateLastReadSmsTimestamp(TELECOM_SENDER, newestTimestamp)

            android.util.Log.d(
                "EthioStat",
                "SyncSmsHistoryUseCase: done. processed=$processedCount, newestTimestamp=$newestTimestamp"
            )

            Result.success(processedCount)
        } catch (e: Exception) {
            android.util.Log.e("EthioStat", "SyncSmsHistoryUseCase error: ${e.message}")
            Result.failure(e)
        }
    }
}
