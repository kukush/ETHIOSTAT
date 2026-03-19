package com.ethiostat.app.domain.usecase

import android.content.Context
import com.ethiostat.app.data.sms.SmsContentReaderService
import com.ethiostat.app.domain.model.AccountSource
import com.ethiostat.app.domain.model.ParsedSmsData
import com.ethiostat.app.domain.repository.IEthioStatRepository
import java.util.concurrent.TimeUnit

/**
 * Reads SMS messages for configured transaction sources (CBE, BOA, TeleBirr, etc.)
 * directly from the Android device SMS inbox (ContentProvider) — NOT from the local SmsLog DB.
 *
 * This fixes the first-run problem: on first install the SmsLog DB is empty, so reading
 * from it yielded no history. Now we read directly from the device inbox.
 */
class ReadTransactionSourceSmsUseCase(
    private val repository: IEthioStatRepository,
    private val context: Context
) {

    private val contentReader by lazy { SmsContentReaderService(context) }

    suspend operator fun invoke(accountSources: List<AccountSource>): Result<List<ParsedSmsData>> {
        return try {
            val results = mutableListOf<ParsedSmsData>()
            val oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)

            // Only process enabled sources that have a configured phone number / sender
            val enabledSources = accountSources.filter { it.isEnabled && it.phoneNumber.isNotBlank() }

            if (enabledSources.isEmpty()) {
                android.util.Log.d("EthioStat", "No enabled transaction sources with phone numbers")
                return Result.success(emptyList())
            }

            enabledSources.forEach { source ->
                // Determine from-timestamp: use lastRead if available, else 1 week ago
                val lastRead = repository.getLastReadSmsTimestamp(source.phoneNumber) ?: oneWeekAgo
                android.util.Log.d(
                    "EthioStat",
                    "Reading device SMS inbox for ${source.displayName} (${source.phoneNumber}) since $lastRead"
                )

                // FIX: Read from device SMS inbox via ContentProvider (not local SmsLog DB)
                val messages = contentReader.readMessagesByAddressPatternSince(
                    listOf(source.phoneNumber),
                    lastRead
                )

                android.util.Log.d(
                    "EthioStat",
                    "Found ${messages.size} device SMS messages for ${source.displayName}"
                )

                var newestTimestamp = lastRead

                messages.forEach { smsLog ->
                    val parsed = repository.processSms(smsLog.sender, smsLog.body, smsLog.receivedAt)
                    if (parsed.isParsed) {
                        results.add(parsed)
                        android.util.Log.d(
                            "EthioStat",
                            "Parsed SMS from ${source.displayName}: txn=${parsed.transaction?.amount}"
                        )
                    }
                    // Track the newest timestamp we've processed
                    if (smsLog.receivedAt > newestTimestamp) {
                        newestTimestamp = smsLog.receivedAt
                    }
                }

                // Update last-read marker so next sync only reads new messages
                if (newestTimestamp > lastRead) {
                    repository.updateLastReadSmsTimestamp(source.phoneNumber, newestTimestamp)
                    android.util.Log.d(
                        "EthioStat",
                        "Updated lastRead for ${source.displayName} to $newestTimestamp"
                    )
                }
            }

            android.util.Log.d("EthioStat", "Transaction source sync: ${results.size} parsed")
            Result.success(results)

        } catch (e: Exception) {
            android.util.Log.e("EthioStat", "ReadTransactionSourceSmsUseCase error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Called when the user adds a new transaction source in Settings.
     * Always reads the last 7 days from device SMS inbox for the new source.
     */
    suspend fun readFromNewSource(accountSource: AccountSource): Result<List<ParsedSmsData>> {
        return try {
            val oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)

            if (accountSource.phoneNumber.isBlank()) {
                android.util.Log.d("EthioStat", "New source ${accountSource.displayName} has no phone number")
                return Result.success(emptyList())
            }

            android.util.Log.d(
                "EthioStat",
                "Scanning last 7 days from device inbox for new source: ${accountSource.displayName} (${accountSource.phoneNumber})"
            )

            // FIX: Read from device SMS inbox, not local DB
            val messages = contentReader.readMessagesByAddressPatternSince(
                listOf(accountSource.phoneNumber),
                oneWeekAgo
            )

            android.util.Log.d("EthioStat", "Found ${messages.size} messages for new source ${accountSource.displayName}")

            val results = mutableListOf<ParsedSmsData>()
            var newestTimestamp = oneWeekAgo

            messages.forEach { smsLog ->
                val parsed = repository.processSms(smsLog.sender, smsLog.body, smsLog.receivedAt)
                if (parsed.isParsed) {
                    results.add(parsed)
                }
                if (smsLog.receivedAt > newestTimestamp) {
                    newestTimestamp = smsLog.receivedAt
                }
            }

            // Save last-read marker for this new source
            repository.updateLastReadSmsTimestamp(accountSource.phoneNumber, newestTimestamp)

            android.util.Log.d("EthioStat", "New source scan complete: ${results.size} parsed")
            Result.success(results)

        } catch (e: Exception) {
            android.util.Log.e("EthioStat", "readFromNewSource error: ${e.message}")
            Result.failure(e)
        }
    }
}
