package com.ethiostat.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethiostat.app.domain.model.AppLanguage
import com.ethiostat.app.domain.model.BalancePackageFactory
import com.ethiostat.app.domain.model.TimePeriod
import com.ethiostat.app.domain.model.AccountSourceType
import com.ethiostat.app.domain.model.Tuple5
import com.ethiostat.app.domain.model.UnreadMessage
import com.ethiostat.app.domain.model.MessageType
import com.ethiostat.app.domain.model.MessagePriority
import com.ethiostat.app.domain.repository.IEthioStatRepository
import com.ethiostat.app.domain.usecase.ChangeLanguageUseCase
import com.ethiostat.app.domain.usecase.GetFinancialSummaryUseCase
import com.ethiostat.app.domain.usecase.SyncBalanceUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: IEthioStatRepository,
    private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
    private val syncBalanceUseCase: SyncBalanceUseCase,
    private val changeLanguageUseCase: ChangeLanguageUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    
    init {
        loadData()
        // Automatically refresh telecom data on app start
        refreshTelecomDataOnStart()
    }
    
    fun processIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadData -> loadData()
            is DashboardIntent.RefreshBalances -> refreshBalances()
            is DashboardIntent.RefreshUssd804 -> refreshUssd804()
            is DashboardIntent.SyncUssd -> syncViaUssd(intent.ussdCode)
            is DashboardIntent.FilterTransactions -> filterTransactions(intent.period)
            is DashboardIntent.FilterBySource -> filterBySource(intent.sourceType)
            is DashboardIntent.ChangeLanguage -> changeLanguage(intent.language)
            is DashboardIntent.ToggleNetBalanceVisibility -> toggleNetBalanceVisibility()
            is DashboardIntent.AddAccountSource -> addAccountSource(intent.source)
            is DashboardIntent.EditAccountSource -> editAccountSource(intent.source)
            is DashboardIntent.DeleteAccountSource -> deleteAccountSource(intent.source)
            is DashboardIntent.ToggleAccountSource -> toggleAccountSource(intent.source)
            is DashboardIntent.StartSmsMonitoring -> startSmsMonitoring()
            is DashboardIntent.StopSmsMonitoring -> stopSmsMonitoring()
            is DashboardIntent.ShowUnreadMessages -> showUnreadMessages()
            is DashboardIntent.MarkMessageAsRead -> markMessageAsRead(intent.messageId)
            is DashboardIntent.MarkAllMessagesAsRead -> markAllMessagesAsRead()
            is DashboardIntent.ClearError -> clearError()
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                combine(
                    repository.getBalances(),
                    repository.getTransactions(),
                    repository.getConfig(),
                    repository.getUnreadMessages(),
                    repository.getUnreadMessageCount()
                ) { balances, transactions, config, unreadMessages, unreadCount ->
                    Tuple5(balances, transactions, config, unreadMessages, unreadCount)
                }.collectLatest { (balances, transactions, config, unreadMessages, unreadCount) ->
                    val summary = getFinancialSummaryUseCase(
                        transactions,
                        _state.value.selectedPeriod
                    )
                    
                    val language = AppLanguage.fromCode(config?.appLanguage ?: "en")
                    
                    _state.update {
                        it.copy(
                            balances = if (balances.isNotEmpty()) balances else BalancePackageFactory.createDefaultZeroBalances(),
                            transactions = transactions,
                            financialSummary = summary,
                            currentLanguage = language,
                            unreadMessageCount = unreadCount,
                            unreadMessages = unreadMessages,
                            isLoading = false,
                            error = null,
                            hasRealData = balances.isNotEmpty()
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
    
    private fun refreshBalances() {
        viewModelScope.launch {
            repository.deleteExpiredPackages()
            loadData()
        }
    }
    
    private fun refreshUssd804() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val result = syncBalanceUseCase.refreshTelecomData()
                _state.update {
                    it.copy(
                        isLoading = false,
                        syncSuccess = result.isSuccess,
                        error = result.exceptionOrNull()?.message
                    )
                }
                if (result.isSuccess) {
                    loadData()
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to refresh: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun filterBySource(sourceType: AccountSourceType?) {
        viewModelScope.launch {
            _state.update { it.copy(selectedSourceFilter = sourceType) }
            
            val allTransactions = _state.value.transactions
            val filteredTransactions = if (sourceType != null) {
                allTransactions.filter { it.accountSource == sourceType }
            } else {
                allTransactions
            }
            
            val summary = getFinancialSummaryUseCase(filteredTransactions, _state.value.selectedPeriod)
            _state.update { it.copy(financialSummary = summary) }
        }
    }
    
    private fun refreshTelecomDataOnStart() {
        viewModelScope.launch {
            try {
                // Small delay to ensure app is fully loaded
                kotlinx.coroutines.delay(2000)
                syncBalanceUseCase.refreshTelecomData()
            } catch (e: Exception) {
                // Silently handle errors on startup to avoid disrupting user experience
                android.util.Log.w("DashboardViewModel", "Failed to refresh telecom data on start: ${e.message}")
            }
        }
    }
    
    private fun toggleNetBalanceVisibility() {
        _state.update { it.copy(showNetBalance = !it.showNetBalance) }
    }
    
    private fun startSmsMonitoring() {
        _state.update { it.copy(isSmsMonitoringActive = true) }
    }
    
    private fun stopSmsMonitoring() {
        _state.update { it.copy(isSmsMonitoringActive = false) }
    }
    
    private fun syncViaUssd(ussdCode: String) {
        viewModelScope.launch {
            val result = syncBalanceUseCase(ussdCode)
            
            _state.update {
                it.copy(
                    syncSuccess = result.isSuccess,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
    
    private fun filterTransactions(period: TimePeriod) {
        viewModelScope.launch {
            _state.update { it.copy(selectedPeriod = period) }
            
            val transactions = _state.value.transactions
            val summary = getFinancialSummaryUseCase(transactions, period)
            
            _state.update {
                it.copy(financialSummary = summary)
            }
        }
    }
    
    private fun changeLanguage(language: AppLanguage) {
        viewModelScope.launch {
            try {
                changeLanguageUseCase(language)
                _state.update {
                    it.copy(currentLanguage = language)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message)
                }
            }
        }
    }
    
    private fun showUnreadMessages() {
        // Toggle showing unread messages dialog
        _state.update { 
            it.copy(error = null) // Clear any existing errors when showing messages
        }
    }
    
    private fun markMessageAsRead(messageId: Long) {
        viewModelScope.launch {
            try {
                // TODO: Implement mark message as read functionality
                // repository.markMessageAsRead(messageId)
                loadData() // Refresh data to update unread count
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Failed to mark message as read: ${e.message}")
                }
            }
        }
    }
    
    private fun markAllMessagesAsRead() {
        viewModelScope.launch {
            try {
                repository.markAllMessagesAsRead()
                loadData() // Refresh data to update unread count
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Failed to mark all messages as read: ${e.message}")
                }
            }
        }
    }
    
    // Add test unread messages for debugging
    private fun addTestUnreadMessages() {
        viewModelScope.launch {
            try {
                val testMessage1 = UnreadMessage(
                    title = "Transaction Alert",
                    content = "New transaction detected: 500 Birr received",
                    messageType = MessageType.TRANSACTION_ALERT,
                    priority = MessagePriority.HIGH
                )
                val testMessage2 = UnreadMessage(
                    title = "System Update",
                    content = "App updated successfully",
                    messageType = MessageType.SUCCESS,
                    priority = MessagePriority.NORMAL
                )
                repository.insertUnreadMessage(testMessage1)
                repository.insertUnreadMessage(testMessage2)
                loadData() // Refresh to show new messages
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Failed to add test messages: ${e.message}")
                }
            }
        }
    }
    
    private fun clearError() {
        _state.update { it.copy(error = null, syncSuccess = false) }
    }
    
    private fun addAccountSource(source: com.ethiostat.app.domain.model.AccountSource) {
        viewModelScope.launch {
            try {
                android.util.Log.d("EthioStat", "Adding AccountSource: ${source.displayName}")
                val id = repository.insertAccountSource(source)
                android.util.Log.d("EthioStat", "Successfully added AccountSource with ID: $id")
            } catch (e: Exception) {
                android.util.Log.e("EthioStat", "Failed to add source", e)
                _state.update { it.copy(error = "Failed to add source: ${e.message}") }
            }
        }
    }
    
    private fun editAccountSource(source: com.ethiostat.app.domain.model.AccountSource) {
        viewModelScope.launch {
            try {
                repository.updateAccountSource(source)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to update source: ${e.message}") }
            }
        }
    }
    
    private fun deleteAccountSource(source: com.ethiostat.app.domain.model.AccountSource) {
        viewModelScope.launch {
            try {
                repository.deleteAccountSource(source)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to delete source: ${e.message}") }
            }
        }
    }
    
    private fun toggleAccountSource(source: com.ethiostat.app.domain.model.AccountSource) {
        viewModelScope.launch {
            try {
                repository.updateAccountSource(source.copy(isEnabled = !source.isEnabled))
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to toggle source: ${e.message}") }
            }
        }
    }
}
