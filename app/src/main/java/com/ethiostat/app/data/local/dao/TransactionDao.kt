package com.ethiostat.app.data.local.dao

import androidx.room.*
import com.ethiostat.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE source = :source ORDER BY timestamp DESC")
    fun getTransactionsBySource(source: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTotalIncome(startDate: Long, endDate: Long): Double?
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpense(startDate: Long, endDate: Long): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
    
    @Query("SELECT COUNT(*) FROM transactions WHERE source = :source AND timestamp = :smsTimestamp")
    suspend fun countBySourceAndTimestamp(source: String, smsTimestamp: Long): Int

    @Query("DELETE FROM transactions WHERE timestamp < :timestamp")
    suspend fun deleteOldTransactions(timestamp: Long)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    /**
     * Delete transactions for a given account source type (enum name string)
     * that have a timestamp >= [fromTimestamp].
     * Used by the reset/clear functionality in Settings.
     */
    @Query("DELETE FROM transactions WHERE accountSource = :accountSource AND timestamp >= :fromTimestamp")
    suspend fun deleteTransactionsByAccountSourceSince(accountSource: String, fromTimestamp: Long)

    /**
     * Delete ALL transactions for a given account source type (full reset).
     */
    @Query("DELETE FROM transactions WHERE accountSource = :accountSource")
    suspend fun deleteTransactionsByAccountSource(accountSource: String)
}
