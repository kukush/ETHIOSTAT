package com.ethiostat.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethiostat.app.domain.model.AppLanguage
import com.ethiostat.app.domain.usecase.ChangeLanguageUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val changeLanguageUseCase: ChangeLanguageUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ChangeLanguage -> changeLanguage(intent.language)
            is SettingsIntent.ClearError -> clearError()
        }
    }
    
    private fun changeLanguage(language: AppLanguage) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                changeLanguageUseCase(language)
                _state.update {
                    it.copy(
                        currentLanguage = language,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to change language"
                    )
                }
            }
        }
    }
    
    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun setCurrentLanguage(language: AppLanguage) {
        _state.update { it.copy(currentLanguage = language) }
    }
}

data class SettingsState(
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class SettingsIntent {
    data class ChangeLanguage(val language: AppLanguage) : SettingsIntent()
    object ClearError : SettingsIntent()
}
