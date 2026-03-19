package com.ethiostat.app.ui.dashboard

import com.ethiostat.app.domain.model.AppLanguage
import com.ethiostat.app.domain.model.TimePeriod
import com.ethiostat.app.domain.model.AccountSourceType
import com.ethiostat.app.domain.model.AccountSource

sealed class DashboardIntent {
    object LoadData : DashboardIntent()
    object RefreshBalances : DashboardIntent()
    object RefreshUssd804 : DashboardIntent()
    data class SyncUssd(val ussdCode: String) : DashboardIntent()
    data class FilterTransactions(val period: TimePeriod) : DashboardIntent()
    data class FilterBySource(val sourceType: AccountSourceType?) : DashboardIntent()
    data class ChangeLanguage(val language: AppLanguage) : DashboardIntent()
    object ToggleNetBalanceVisibility : DashboardIntent()
    data class SelectAccountSource(val source: AccountSource?) : DashboardIntent()
    object ShowAccountSourcesScreen : DashboardIntent()
    object HideAccountSourcesScreen : DashboardIntent()
    data class AddAccountSource(val source: AccountSource) : DashboardIntent()
    data class EditAccountSource(val source: AccountSource) : DashboardIntent()
    data class DeleteAccountSource(val source: AccountSource) : DashboardIntent()
    data class ToggleAccountSource(val source: AccountSource) : DashboardIntent()
    object StartSmsMonitoring : DashboardIntent()
    object StopSmsMonitoring : DashboardIntent()
    object ShowUnreadMessages : DashboardIntent()
    data class MarkMessageAsRead(val messageId: Long) : DashboardIntent()
    object MarkAllMessagesAsRead : DashboardIntent()
    object ScanSmsHistory : DashboardIntent()
    object ClearError : DashboardIntent()
}
