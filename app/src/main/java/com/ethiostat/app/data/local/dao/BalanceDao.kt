package com.ethiostat.app.data.local.dao

import androidx.room.*
import com.ethiostat.app.data.local.entity.BalancePackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceDao {
    
    @Query("SELECT * FROM balance_packages ORDER BY expiryTimestamp ASC")
    fun getAllBalances(): Flow<List<BalancePackageEntity>>
    
    @Query("SELECT * FROM balance_packages WHERE expiryTimestamp > :currentTime ORDER BY expiryTimestamp ASC")
    fun getActiveBalances(currentTime: Long = System.currentTimeMillis()): Flow<List<BalancePackageEntity>>
    
    @Query("SELECT * FROM balance_packages WHERE packageType = :type ORDER BY expiryTimestamp ASC")
    fun getBalancesByType(type: String): Flow<List<BalancePackageEntity>>
    
    @Query("SELECT * FROM balance_packages WHERE id = :id")
    suspend fun getBalanceById(id: Long): BalancePackageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: BalancePackageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalances(balances: List<BalancePackageEntity>)
    
    @Update
    suspend fun updateBalance(balance: BalancePackageEntity)
    
    @Delete
    suspend fun deleteBalance(balance: BalancePackageEntity)
    
    @Query("DELETE FROM balance_packages WHERE expiryTimestamp < :timestamp")
    suspend fun deleteExpiredPackages(timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM balance_packages WHERE packageType = :packageType AND source = :source")
    suspend fun deleteByTypeAndSource(packageType: String, source: String)

    @Query("DELETE FROM balance_packages WHERE packageType = :packageType")
    suspend fun deleteByType(packageType: String)

    @Query("DELETE FROM balance_packages")
    suspend fun deleteAllBalances()
}
