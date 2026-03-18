package com.ethiostat.app.data.local.dao

import androidx.room.*
import com.ethiostat.app.data.local.entity.AccountSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountSourceDao {
    
    @Query("SELECT * FROM account_sources ORDER BY displayName ASC")
    fun getAllAccountSources(): Flow<List<AccountSourceEntity>>
    
    @Query("SELECT * FROM account_sources WHERE isEnabled = 1 ORDER BY displayName ASC")
    fun getEnabledAccountSources(): Flow<List<AccountSourceEntity>>
    
    @Query("SELECT * FROM account_sources WHERE id = :id")
    suspend fun getAccountSourceById(id: Long): AccountSourceEntity?
    
    @Query("SELECT * FROM account_sources WHERE type = :type AND isEnabled = 1")
    suspend fun getAccountSourcesByType(type: String): List<AccountSourceEntity>
    
    @Query("SELECT * FROM account_sources WHERE phoneNumber LIKE '%' || :phoneNumber || '%' AND isEnabled = 1")
    suspend fun getAccountSourcesByPhoneNumber(phoneNumber: String): List<AccountSourceEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountSource(accountSource: AccountSourceEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountSources(accountSources: List<AccountSourceEntity>)
    
    @Update
    suspend fun updateAccountSource(accountSource: AccountSourceEntity)
    
    @Delete
    suspend fun deleteAccountSource(accountSource: AccountSourceEntity)
    
    @Query("DELETE FROM account_sources WHERE id = :id")
    suspend fun deleteAccountSourceById(id: Long)
    
    @Query("UPDATE account_sources SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateAccountSourceEnabled(id: Long, isEnabled: Boolean)
}
