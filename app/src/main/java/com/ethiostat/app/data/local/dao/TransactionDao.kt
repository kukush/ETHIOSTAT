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
    
    @Query("DELETE FROM transactions WHERE timestamp < :timestamp")
    suspend fun deleteOldTransactions(timestamp: Long)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
