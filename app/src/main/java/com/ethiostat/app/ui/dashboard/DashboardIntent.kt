package com.ethiostat.app.ui.dashboard

import com.ethiostat.app.domain.model.AppLanguage
import com.ethiostat.app.domain.model.TimePeriod

sealed class DashboardIntent {
    object LoadData : DashboardIntent()
    object RefreshBalances : DashboardIntent()
    data class SyncUssd(val ussdCode: String) : DashboardIntent()
    data class FilterTransactions(val period: TimePeriod) : DashboardIntent()
    data class ChangeLanguage(val language: AppLanguage) : DashboardIntent()
    object ClearError : DashboardIntent()
}
