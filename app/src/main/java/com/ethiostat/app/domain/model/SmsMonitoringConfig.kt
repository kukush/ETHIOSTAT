package com.ethiostat.app.domain.model

data class SmsMonitoringConfig(
    val id: Long = 1,
    val isEnabled: Boolean = true,
    val autoRefreshEnabled: Boolean = true,
    val refreshIntervalMinutes: Int = 5,
    val showNetBalance: Boolean = true,
    val enableRealTimeMonitoring: Boolean = true,
    val lastMonitoringTimestamp: Long = 0L
)

data class RefreshConfig(
    val ussdCode: String = "*804#",
    val autoCallOnRefresh: Boolean = true,
    val refreshTimeoutSeconds: Int = 30
)
