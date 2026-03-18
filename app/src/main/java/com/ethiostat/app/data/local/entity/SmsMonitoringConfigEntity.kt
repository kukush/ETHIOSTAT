package com.ethiostat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ethiostat.app.domain.model.SmsMonitoringConfig

@Entity(tableName = "sms_monitoring_config")
data class SmsMonitoringConfigEntity(
    @PrimaryKey val id: Long = 1,
    val isEnabled: Boolean = true,
    val autoRefreshEnabled: Boolean = true,
    val refreshIntervalMinutes: Int = 5,
    val showNetBalance: Boolean = true,
    val enableRealTimeMonitoring: Boolean = true,
    val lastMonitoringTimestamp: Long = 0L
)

fun SmsMonitoringConfigEntity.toDomain(): SmsMonitoringConfig {
    return SmsMonitoringConfig(
        id = id,
        isEnabled = isEnabled,
        autoRefreshEnabled = autoRefreshEnabled,
        refreshIntervalMinutes = refreshIntervalMinutes,
        showNetBalance = showNetBalance,
        enableRealTimeMonitoring = enableRealTimeMonitoring,
        lastMonitoringTimestamp = lastMonitoringTimestamp
    )
}

fun SmsMonitoringConfig.toEntity(): SmsMonitoringConfigEntity {
    return SmsMonitoringConfigEntity(
        id = id,
        isEnabled = isEnabled,
        autoRefreshEnabled = autoRefreshEnabled,
        refreshIntervalMinutes = refreshIntervalMinutes,
        showNetBalance = showNetBalance,
        enableRealTimeMonitoring = enableRealTimeMonitoring,
        lastMonitoringTimestamp = lastMonitoringTimestamp
    )
}
