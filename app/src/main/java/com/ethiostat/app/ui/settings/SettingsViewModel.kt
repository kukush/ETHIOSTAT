package com.ethiostat.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethiostat.app.domain.model.AppLanguage
import com.ethiostat.app.domain.model.ThemeMode
import com.ethiostat.app.domain.usecase.ChangeLanguageUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val changeLanguageUseCase: ChangeLanguageUseCase,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("ethiostat_prefs", Context.MODE_PRIVATE)
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ChangeLanguage -> changeLanguage(intent.language)
            is SettingsIntent.ChangeThemeMode -> changeThemeMode(intent.mode)
            is SettingsIntent.ClearError -> clearError()
        }
    }

    private fun changeThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.key).apply()
        _state.update { it.copy(currentThemeMode = mode) }
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

    fun loadThemeMode(): ThemeMode {
        val key = prefs.getString("theme_mode", ThemeMode.SYSTEM.key) ?: ThemeMode.SYSTEM.key
        val mode = ThemeMode.fromKey(key)
        _state.update { it.copy(currentThemeMode = mode) }
        return mode
    }
}

data class SettingsState(
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class SettingsIntent {
    data class ChangeLanguage(val language: AppLanguage) : SettingsIntent()
    data class ChangeThemeMode(val mode: ThemeMode) : SettingsIntent()
    object ClearError : SettingsIntent()
}
