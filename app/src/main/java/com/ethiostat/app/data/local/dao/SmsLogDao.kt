package com.ethiostat.app.data.local.dao

import androidx.room.*
import com.ethiostat.app.data.local.entity.SmsLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsLogDao {
    
    @Query("SELECT * FROM sms_log ORDER BY receivedAt DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<SmsLogEntity>>
    
    @Query("SELECT * FROM sms_log WHERE parsed = 0 ORDER BY receivedAt DESC")
    fun getUnparsedLogs(): Flow<List<SmsLogEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SmsLogEntity)
    
    @Query("DELETE FROM sms_log WHERE receivedAt < :timestamp")
    suspend fun deleteOldLogs(timestamp: Long)
    
    @Query("DELETE FROM sms_log")
    suspend fun deleteAllLogs()
}
