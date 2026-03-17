package com.ethiostat.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethiostat.app.domain.model.AppLanguage
import com.ethiostat.app.domain.model.TimePeriod
import com.ethiostat.app.domain.repository.IEthioStatRepository
import com.ethiostat.app.domain.usecase.ChangeLanguageUseCase
import com.ethiostat.app.domain.usecase.GetFinancialSummaryUseCase
import com.ethiostat.app.domain.usecase.SyncBalanceUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: IEthioStatRepository,
    private val context: Context,
    private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
    private val syncBalanceUseCase: SyncBalanceUseCase,
    private val changeLanguageUseCase: ChangeLanguageUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    
    init {
        loadData()
    }
    
    fun processIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadData -> loadData()
            is DashboardIntent.RefreshBalances -> refreshBalances()
            is DashboardIntent.SyncUssd -> syncViaUssd(intent.ussdCode)
            is DashboardIntent.FilterTransactions -> filterTransactions(intent.period)
            is DashboardIntent.ChangeLanguage -> changeLanguage(intent.language)
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
                    repository.getConfig()
                ) { balances, transactions, config ->
                    Triple(balances, transactions, config)
                }.collectLatest { (balances, transactions, config) ->
                    val summary = getFinancialSummaryUseCase(
                        transactions,
                        _state.value.selectedPeriod
                    )
                    
                    val language = AppLanguage.fromCode(config?.appLanguage ?: "en")
                    
                    _state.update {
                        it.copy(
                            balances = balances,
                            transactions = transactions,
                            financialSummary = summary,
                            currentLanguage = language,
                            isLoading = false,
                            error = null
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
    
    private fun clearError() {
        _state.update { it.copy(error = null, syncSuccess = false) }
    }
}
