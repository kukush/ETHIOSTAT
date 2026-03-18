package com.ethiostat.app.data.local.dao

import androidx.room.*
import com.ethiostat.app.data.local.entity.SmsMonitoringConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsMonitoringConfigDao {
    
    @Query("SELECT * FROM sms_monitoring_config WHERE id = 1")
    fun getSmsMonitoringConfig(): Flow<SmsMonitoringConfigEntity?>
    
    @Query("SELECT * FROM sms_monitoring_config WHERE id = 1")
    suspend fun getSmsMonitoringConfigOnce(): SmsMonitoringConfigEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsMonitoringConfig(config: SmsMonitoringConfigEntity)
    
    @Update
    suspend fun updateSmsMonitoringConfig(config: SmsMonitoringConfigEntity)
    
    @Query("UPDATE sms_monitoring_config SET isEnabled = :isEnabled WHERE id = 1")
    suspend fun updateSmsMonitoringEnabled(isEnabled: Boolean)
    
    @Query("UPDATE sms_monitoring_config SET autoRefreshEnabled = :autoRefreshEnabled WHERE id = 1")
    suspend fun updateAutoRefreshEnabled(autoRefreshEnabled: Boolean)
    
    @Query("UPDATE sms_monitoring_config SET showNetBalance = :showNetBalance WHERE id = 1")
    suspend fun updateShowNetBalance(showNetBalance: Boolean)
    
    @Query("UPDATE sms_monitoring_config SET enableRealTimeMonitoring = :enableRealTimeMonitoring WHERE id = 1")
    suspend fun updateRealTimeMonitoringEnabled(enableRealTimeMonitoring: Boolean)
    
    @Query("UPDATE sms_monitoring_config SET lastMonitoringTimestamp = :timestamp WHERE id = 1")
    suspend fun updateLastMonitoringTimestamp(timestamp: Long)
}
