package com.ethiostat.app.data.local.dao

import androidx.room.*
import com.ethiostat.app.data.local.entity.AppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    
    @Query("SELECT * FROM app_config WHERE id = 1")
    fun getConfig(): Flow<AppConfigEntity?>
    
    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun getConfigOnce(): AppConfigEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfigEntity)
    
    @Update
    suspend fun updateConfig(config: AppConfigEntity)
    
    @Query("UPDATE app_config SET lastReadTimestamp = :timestamp WHERE id = 1")
    suspend fun updateLastReadTimestamp(timestamp: Long)
    
    @Query("UPDATE app_config SET appLanguage = :language WHERE id = 1")
    suspend fun updateLanguage(language: String)
    
    @Query("UPDATE app_config SET telecomSenders = :senders WHERE id = 1")
    suspend fun updateTelecomSenders(senders: String)
    
    @Query("UPDATE app_config SET bankSenders = :senders WHERE id = 1")
    suspend fun updateBankSenders(senders: String)
    
    @Query("UPDATE app_config SET ussdBalanceCode = :code WHERE id = 1")
    suspend fun updateUssdBalanceCode(code: String)
    
    @Query("UPDATE app_config SET ussdPackagesCode = :code WHERE id = 1")
    suspend fun updateUssdPackagesCode(code: String)
}
