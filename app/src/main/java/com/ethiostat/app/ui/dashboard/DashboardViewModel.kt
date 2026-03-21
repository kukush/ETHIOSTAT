package com.ethiostat.app.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ethiostat.app.domain.model.AppLanguage
import com.ethiostat.app.domain.model.BalancePackage
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
import com.ethiostat.app.domain.usecase.ReadTransactionSourceSmsUseCase
import com.ethiostat.app.domain.usecase.SyncBalanceUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: IEthioStatRepository,
    private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
    private val syncBalanceUseCase: SyncBalanceUseCase,
    private val changeLanguageUseCase: ChangeLanguageUseCase,
    private val readTransactionSourceSmsUseCase: ReadTransactionSourceSmsUseCase,
    private val syncSmsHistoryUseCase: com.ethiostat.app.domain.usecase.SyncSmsHistoryUseCase,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    /**
     * Receives USSD popup text captured either by [SyncBalanceUseCase] (Path 1)
     * or [UssdAccessibilityService] (Path 2 fallback).  Updates [DashboardState.ussdResponseText].
     */
    private val ussdTextReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val text = intent?.getStringExtra("ussd_text") ?: return
            android.util.Log.d("DashboardViewModel", "USSD popup text received: $text")
            _state.update { it.copy(ussdResponseText = text) }
        }
    }

    init {
        LocalBroadcastManager.getInstance(context).registerReceiver(
            ussdTextReceiver,
            IntentFilter("com.ethiostat.app.USSD_RESPONSE_RECEIVED")
        )
        loadData()
        // Scan existing SMS history only — no USSD call on startup
        refreshSmsDataOnStart()

        // Ensure default sources and deduplicate ONCE on startup
        viewModelScope.launch {
            val currentSources = repository.getAccountSources().first()
            ensureDefaultSources(currentSources)
        }
    }

    override fun onCleared() {
        super.onCleared()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(ussdTextReceiver)
    }
    
    fun processIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadData -> loadData()
            is DashboardIntent.RefreshBalances -> refreshBalances()
            is DashboardIntent.RefreshUssd804 -> refreshUssd804()
            is DashboardIntent.ScanSmsHistory -> scanSmsHistory()
            is DashboardIntent.SyncUssd -> syncViaUssd(intent.ussdCode)
            is DashboardIntent.FilterTransactions -> filterTransactions(intent.period)
            is DashboardIntent.FilterBySource -> filterBySource(intent.sourceType)
            is DashboardIntent.ChangeLanguage -> changeLanguage(intent.language)
            is DashboardIntent.ToggleNetBalanceVisibility -> toggleNetBalanceVisibility()
            is DashboardIntent.SelectAccountSource -> selectAccountSource(intent.source)
            is DashboardIntent.ShowAccountSourcesScreen -> showAccountSourcesScreen()
            is DashboardIntent.HideAccountSourcesScreen -> hideAccountSourcesScreen()
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
            is DashboardIntent.ResetTransactionsForSource -> resetTransactionsForSource(intent.accountSourceType, intent.fromTimestamp)
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                combine(
                    repository.getBalances(),
                    repository.getTransactions(),
                    repository.getConfig()
                ) { balances, transactions, config ->
                    Triple(balances, transactions, config)
                }.combine(
                    combine(
                        repository.getUnreadMessages(),
                        repository.getUnreadMessageCount(),
                        repository.getAccountSources()
                    ) { unreadMessages, unreadCount, accountSources ->
                        Triple(unreadMessages, unreadCount, accountSources)
                    }
                ) { (balances, transactions, config), (unreadMessages, unreadCount, accountSources) ->
                    Pair(
                        Triple(balances, transactions, config),
                        Triple(unreadMessages, unreadCount, accountSources)
                    )
                }.collectLatest { (firstTriple, secondTriple) ->
                    val (balances, transactions, config) = firstTriple
                    val (unreadMessages, unreadCount, accountSources) = secondTriple
                    val summary = calculateSummaryWithFilters(
                        transactions,
                        _state.value.selectedPeriod,
                        _state.value.selectedAccountSource,
                        _state.value.selectedSourceFilter,
                        accountSources
                    )
                    
                    val language = AppLanguage.fromCode(config?.appLanguage ?: "en")
                    
                    android.util.Log.d("EthioStat", "loadData: accountSources.size=${accountSources.size}")
                    
                    _state.update {
                        it.copy(
                            balances = if (balances.isNotEmpty()) balances else BalancePackageFactory.createDefaultZeroBalances(),
                            transactions = transactions,
                            financialSummary = summary,
                            currentLanguage = language,
                            unreadMessageCount = unreadCount,
                            unreadMessages = unreadMessages,
                            accountSources = accountSources,
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
                // Execute *804# — triggers an incoming SMS from Ethio Telecom that is
                // processed by SmsReceiver and parsed by EnglishSmsParser (including SMS count)
                val result804 = syncBalanceUseCase.refreshTelecomData()

                // Fallback: if USSD response failed, scan SMS history
                if (result804.isFailure) {
                    android.util.Log.d("EthioStat", "USSD refresh failed, falling back to history scan")
                    syncSmsHistoryUseCase(forceFullScan = false)
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        syncSuccess = result804.isSuccess,
                        error = result804.exceptionOrNull()?.message
                    )
                }

                if (result804.isSuccess) {
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
            val newState = _state.updateAndGet { it.copy(selectedSourceFilter = sourceType) }
            val summary = calculateSummaryWithFilters(
                newState.transactions,
                newState.selectedPeriod,
                newState.selectedAccountSource,
                newState.selectedSourceFilter,
                newState.accountSources
            )
            _state.update { it.copy(financialSummary = summary) }
        }
    }
    
    /**
     * On app start: scan existing SMS history only — no USSD call.
     * The USSD call (*804#) is only triggered by the user pressing the refresh button,
     * which maps to [DashboardIntent.RefreshUssd804].
     */
    private fun refreshSmsDataOnStart() {
        viewModelScope.launch {
            try {
                // Scan the last 14 days of SMS history for balances and transactions
                syncSmsHistoryUseCase(forceFullScan = false)

                // Scan each registered transaction source SMS inbox
                val sources = _state.value.accountSources
                if (sources.isNotEmpty()) {
                    readTransactionSourceSmsUseCase.invoke(sources)
                }

                // Reload UI with the freshly scanned data
                loadData()
            } catch (e: Exception) {
                android.util.Log.w("DashboardViewModel", "Failed startup SMS scan: ${e.message}")
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
            val result = syncBalanceUseCase.sendDirectUssdRequest(ussdCode)
            
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
            val newState = _state.updateAndGet { it.copy(selectedPeriod = period) }
            val summary = calculateSummaryWithFilters(
                newState.transactions,
                newState.selectedPeriod,
                newState.selectedAccountSource,
                newState.selectedSourceFilter,
                newState.accountSources
            )
            
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
                
                // Read SMS from the new source if it has a phone number
                if (source.phoneNumber.isNotBlank()) {
                    android.util.Log.d("EthioStat", "Reading SMS from new source: ${source.phoneNumber}")
                    val smsResult = readTransactionSourceSmsUseCase.readFromNewSource(source)
                    smsResult.onSuccess { smsList ->
                        android.util.Log.d("EthioStat", "Successfully read ${smsList.size} SMS from ${source.displayName}")
                    }.onFailure { error ->
                        android.util.Log.e("EthioStat", "Failed to read SMS from ${source.displayName}: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EthioStat", "Failed to add source", e)
                _state.update { it.copy(error = "Failed to add source: ${e.message}") }
            }
        }
    }
    
    private fun editAccountSource(source: com.ethiostat.app.domain.model.AccountSource) {
        viewModelScope.launch {
            try {
                android.util.Log.d("EthioStat", "Updating AccountSource: ${source.displayName}")
                repository.updateAccountSource(source)
                android.util.Log.d("EthioStat", "Successfully updated AccountSource")
            } catch (e: Exception) {
                android.util.Log.e("EthioStat", "Failed to update source", e)
                _state.update { it.copy(error = "Failed to update source: ${e.message}") }
            }
        }
    }
    
    private fun deleteAccountSource(source: com.ethiostat.app.domain.model.AccountSource) {
        viewModelScope.launch {
            try {
                android.util.Log.d("EthioStat", "Deleting AccountSource: ${source.displayName}")
                repository.deleteAccountSource(source)
                android.util.Log.d("EthioStat", "Successfully deleted AccountSource")
            } catch (e: Exception) {
                android.util.Log.e("EthioStat", "Failed to delete source", e)
                _state.update { it.copy(error = "Failed to delete source: ${e.message}") }
            }
        }
    }
    
    private fun toggleAccountSource(source: com.ethiostat.app.domain.model.AccountSource) {
        viewModelScope.launch {
            try {
                val newEnabledState = !source.isEnabled
                android.util.Log.d("EthioStat", "Toggling AccountSource: ${source.displayName} -> $newEnabledState")
                repository.updateAccountSource(source.copy(isEnabled = newEnabledState))
                android.util.Log.d("EthioStat", "Successfully toggled AccountSource")
                
                // If the toggled source was selected and is now disabled, deselect it
                if (!newEnabledState && _state.value.selectedAccountSource?.id == source.id) {
                    android.util.Log.d("EthioStat", "Deselecting disabled source: ${source.displayName}")
                    _state.update { it.copy(selectedAccountSource = null) }
                }
            } catch (e: Exception) {
                android.util.Log.e("EthioStat", "Failed to toggle source", e)
                _state.update { it.copy(error = "Failed to toggle source: ${e.message}") }
            }
        }
    }
    
    private fun selectAccountSource(source: com.ethiostat.app.domain.model.AccountSource?) {
        android.util.Log.d("EthioStat", "Selecting AccountSource: ${source?.displayName ?: "All Sources"}")
        
        val newState = _state.updateAndGet { 
            it.copy(
                selectedAccountSource = source,
                selectedSourceFilter = source?.type
            ) 
        }
        
        val summary = calculateSummaryWithFilters(
            newState.transactions,
            newState.selectedPeriod,
            newState.selectedAccountSource,
            newState.selectedSourceFilter,
            newState.accountSources
        )
        
        _state.update { it.copy(financialSummary = summary) }
    }
    
    private fun showAccountSourcesScreen() {
        android.util.Log.d("EthioStat", "ShowAccountSourcesScreen called")
        _state.update { it.copy(showAccountSourcesScreen = true) }
    }
    
    private fun hideAccountSourcesScreen() {
        android.util.Log.d("EthioStat", "HideAccountSourcesScreen called")
        _state.update { it.copy(showAccountSourcesScreen = false) }
    }
    
    /**
     * Seeds the 4 [AccountSourceType.DEFAULT_SOURCES] on a fresh install.
     *
     * Guard: checks by [AccountSourceType] — if the user deletes a source, it will NOT
     * be re-created automatically. Each type can only be seeded once.
     */
    private suspend fun ensureDefaultSources(currentSources: List<com.ethiostat.app.domain.model.AccountSource>) {
        // 1. Deduplicate by type first to clean up existing mess (14 sources issue)
        val typeGroups = currentSources.groupBy { it.type }
        typeGroups.forEach { (type, sources) ->
            if (sources.size > 1 && type != com.ethiostat.app.domain.model.AccountSourceType.UNKNOWN) {
                // Keep the one with the lowest ID (usually the first one created)
                val toKeep = sources.minBy { it.id }
                sources.filter { it.id != toKeep.id }.forEach { toDelete ->
                    android.util.Log.d("EthioStat", "Deleting duplicate source: ${toDelete.displayName} (Type: ${toDelete.type})")
                    repository.deleteAccountSource(toDelete)
                }
            }
        }

        // 2. Refresh list after deduplication
        val updatedSources = repository.getAccountSources().first()
        
        // 3. Seed missing defaults
        com.ethiostat.app.domain.model.AccountSourceType.DEFAULT_SOURCES.forEach { sourceType ->
            val alreadyExists = updatedSources.any { it.type == sourceType }
            if (!alreadyExists) {
                val newSource = com.ethiostat.app.domain.model.AccountSource(
                    name = sourceType.name,
                    displayName = sourceType.displayName,
                    type = sourceType,
                    phoneNumber = sourceType.shortCode
                )
                android.util.Log.d("EthioStat", "Seeding default source: ${sourceType.displayName} (${sourceType.shortCode})")
                repository.insertAccountSource(newSource)
            }
        }

        // 4. Remove unsupported/unknown sources (legacy cleanup)
        val finalSources = repository.getAccountSources().first()
        finalSources.forEach { source ->
            if (source.type == com.ethiostat.app.domain.model.AccountSourceType.UNKNOWN) {
                android.util.Log.d("EthioStat", "Removing unsupported source: ${source.displayName}")
                repository.deleteAccountSource(source)
            }
        }
    }
    
    private fun calculateSummaryWithFilters(
        transactions: List<com.ethiostat.app.domain.model.Transaction>,
        period: TimePeriod,
        selectedAccountSource: com.ethiostat.app.domain.model.AccountSource?,
        selectedSourceFilter: AccountSourceType?,
        accountSources: List<com.ethiostat.app.domain.model.AccountSource>
    ): com.ethiostat.app.domain.model.FinancialSummary {
        val filteredTransactions = when {
            selectedAccountSource != null -> {
                transactions.filter { it.accountSource == selectedAccountSource.type }
            }
            selectedSourceFilter != null -> {
                transactions.filter { it.accountSource == selectedSourceFilter }
            }
            else -> {
                // All resources selected. Only include transactions from enabled sources.
                val activeSourceTypes = accountSources.filter { it.isEnabled }.map { it.type }
                transactions.filter { it.accountSource in activeSourceTypes }
            }
        }
        return getFinancialSummaryUseCase(filteredTransactions, period)
    }

    private fun scanSmsHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val result = syncSmsHistoryUseCase(forceFullScan = true) // Force full 14-day scan
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
                        error = "Scanner failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Resets (deletes) transactions for a specific account source starting from [fromTimestamp].
     * If [fromTimestamp] is 0L, deletes ALL transactions for that source.
     * After deletion, recalculates the financial summary and updates the UI.
     */
    private fun resetTransactionsForSource(accountSourceType: String, fromTimestamp: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                if (fromTimestamp == 0L) {
                    repository.deleteAllTransactionsBySource(accountSourceType)
                    android.util.Log.d("EthioStat", "Reset ALL transactions for $accountSourceType")
                } else {
                    repository.deleteTransactionsBySourceSince(accountSourceType, fromTimestamp)
                    android.util.Log.d("EthioStat", "Reset transactions for $accountSourceType since $fromTimestamp")
                }
                _state.update { it.copy(isLoading = false, syncSuccess = true) }
                loadData() // Recalculate summary and update UI
            } catch (e: Exception) {
                android.util.Log.e("EthioStat", "resetTransactionsForSource error: ${e.message}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Reset failed: ${e.message}"
                    )
                }
            }
        }
    }
}
