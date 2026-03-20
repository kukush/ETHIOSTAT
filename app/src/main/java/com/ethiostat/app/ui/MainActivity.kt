package com.ethiostat.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.ethiostat.app.data.local.EthioStatDatabase
import com.ethiostat.app.data.parser.AmharicSmsParser
import com.ethiostat.app.data.parser.EnglishSmsParser
import com.ethiostat.app.data.parser.MultilingualSmsParser
import com.ethiostat.app.data.parser.OromoSmsParser
import com.ethiostat.app.data.parser.SmsLanguageDetector
import com.ethiostat.app.data.repository.EthioStatRepositoryImpl
import com.ethiostat.app.domain.repository.IEthioStatRepository
import com.ethiostat.app.domain.usecase.ChangeLanguageUseCase
import com.ethiostat.app.domain.usecase.GetFinancialSummaryUseCase
import com.ethiostat.app.domain.usecase.ReadStoredSmsUseCase
import com.ethiostat.app.domain.usecase.ReadTransactionSourceSmsUseCase
import com.ethiostat.app.domain.usecase.SyncBalanceUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.content.Context
import android.content.res.Configuration
import com.ethiostat.app.domain.model.AppLanguage
import com.ethiostat.app.domain.model.SmsMonitoringConfig
import com.ethiostat.app.domain.model.ThemeMode
import java.util.Locale
import com.ethiostat.app.ui.dashboard.DashboardIntent
import com.ethiostat.app.ui.dashboard.DashboardScreen
import com.ethiostat.app.ui.dashboard.DashboardViewModel
import com.ethiostat.app.ui.settings.AccountSourcesScreen
import com.ethiostat.app.ui.settings.SettingsIntent
import com.ethiostat.app.ui.settings.SettingsScreen
import com.ethiostat.app.ui.settings.SettingsViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ethiostat.app.ui.theme.EthioStatTheme

class MainActivity : ComponentActivity() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        val readSmsGranted = permissions[Manifest.permission.READ_SMS] == true
        val callGranted = permissions[Manifest.permission.CALL_PHONE] == true
        
        if (smsGranted && readSmsGranted) {
            // Permissions granted, start auto-sync with repository
            val database = EthioStatDatabase.getDatabase(applicationContext)
            val repository = createRepository()
            startAutoSync(repository)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        val database = EthioStatDatabase.getDatabase(applicationContext)
        val repository = EthioStatRepositoryImpl(
            balanceDao = database.balanceDao(),
            transactionDao = database.transactionDao(),
            configDao = database.configDao(),
            smsLogDao = database.smsLogDao(),
            accountSourceDao = database.accountSourceDao(),
            smsMonitoringConfigDao = database.smsMonitoringConfigDao(),
            unreadMessageDao = database.unreadMessageDao(),
            lastReadSmsDao = database.lastReadSmsDao(),
            smsParser = MultilingualSmsParser(
                languageDetector = SmsLanguageDetector(),
                englishParser = EnglishSmsParser(),
                amharicParser = AmharicSmsParser(),
                oromoParser = OromoSmsParser()
            )
        )
        
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                        DashboardViewModel(
                            repository = repository,
                            getFinancialSummaryUseCase = GetFinancialSummaryUseCase(),
                            syncBalanceUseCase = SyncBalanceUseCase(applicationContext),
                            changeLanguageUseCase = ChangeLanguageUseCase(repository),
                            readTransactionSourceSmsUseCase = ReadTransactionSourceSmsUseCase(
                                repository = repository,
                                context = applicationContext
                            ),
                            syncSmsHistoryUseCase = com.ethiostat.app.domain.usecase.SyncSmsHistoryUseCase(
                                context = applicationContext,
                                repository = repository
                            )
                        ) as T
                    }
                    modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                        SettingsViewModel(
                            changeLanguageUseCase = ChangeLanguageUseCase(repository),
                            context = applicationContext
                        ) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }

        val viewModel = ViewModelProvider(this, factory).get(DashboardViewModel::class.java)
        val settingsViewModel = ViewModelProvider(this, factory).get(SettingsViewModel::class.java)
        
        // Start auto-sync if permissions are already granted
        if (hasAllPermissions()) {
            startAutoSync(repository)
            // Start SMS monitoring service to listen for new messages
            startSmsMonitoringService()
        }
        
        settingsViewModel.loadThemeMode()

        setContent {
            val settingsState by settingsViewModel.state.collectAsState()
            val themeMode = settingsState.currentThemeMode
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            EthioStatTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val dashboardState by viewModel.state.collectAsState()
                    var showSettings by remember { mutableStateOf(false) }
                    var showAccountSources by remember { mutableStateOf(false) }

                    // Apply locale and force full UI recomposition when language changes
                    LaunchedEffect(dashboardState.currentLanguage) {
                        applyLocaleToContext(dashboardState.currentLanguage)
                    }

                    key(dashboardState.currentLanguage) {
                        when {
                            showAccountSources -> {
                        AccountSourcesScreen(
                                    accountSources = dashboardState.accountSources,
                                    onAddSource = { source ->
                                        viewModel.processIntent(DashboardIntent.AddAccountSource(source))
                                    },
                                    onEditSource = { source ->
                                        viewModel.processIntent(DashboardIntent.EditAccountSource(source))
                                    },
                                    onDeleteSource = { source ->
                                        viewModel.processIntent(DashboardIntent.DeleteAccountSource(source))
                                    },
                                    onToggleSource = { source ->
                                        viewModel.processIntent(DashboardIntent.ToggleAccountSource(source))
                                    },
                                    onResetTransactions = { source, fromTimestamp ->
                                        viewModel.processIntent(
                                            DashboardIntent.ResetTransactionsForSource(
                                                accountSourceType = source.type.name,
                                                fromTimestamp = fromTimestamp
                                            )
                                        )
                                    },
                                    onNavigateBack = {
                                        showAccountSources = false
                                        showSettings = true
                                    }
                                )
                            }
                            showSettings -> {
                                SettingsScreen(
                                    currentLanguage = dashboardState.currentLanguage,
                                    currentThemeMode = settingsState.currentThemeMode,
                                    showNetBalance = dashboardState.showNetBalance,
                                    onLanguageChange = { language ->
                                        viewModel.processIntent(DashboardIntent.ChangeLanguage(language))
                                        settingsViewModel.setCurrentLanguage(language)
                                    },
                                    onThemeModeChange = { mode ->
                                        settingsViewModel.processIntent(SettingsIntent.ChangeThemeMode(mode))
                                    },
                                    onToggleNetBalanceVisibility = { 
                                        viewModel.processIntent(DashboardIntent.ToggleNetBalanceVisibility)
                                    },
                                    onNavigateToAccountSources = { 
                                        showSettings = false
                                        showAccountSources = true
                                    },
                                    onNavigateBack = { showSettings = false }
                                )
                            }
                            else -> {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { showSettings = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_SMS)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun hasAllPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun startAutoSync(repository: IEthioStatRepository? = null) {
        android.util.Log.d("EthioStat", "Starting auto-sync...")
        scope.launch {
            try {
                val repo = repository ?: createRepository()
                val readStoredSmsUseCase = ReadStoredSmsUseCase(repo, applicationContext)
                
                android.util.Log.d("EthioStat", "Executing ReadStoredSmsUseCase...")
                // Read historical SMS messages using ContentResolver + local database
                val result = readStoredSmsUseCase()
                
                if (result.isFailure) {
                    android.util.Log.e("EthioStat", "Auto-sync failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("EthioStat", "Auto-sync exception: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun applyLocaleToContext(language: AppLanguage) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun createRepository(): IEthioStatRepository {
        val database = EthioStatDatabase.getDatabase(applicationContext)
        return EthioStatRepositoryImpl(
            balanceDao = database.balanceDao(),
            transactionDao = database.transactionDao(),
            configDao = database.configDao(),
            smsLogDao = database.smsLogDao(),
            accountSourceDao = database.accountSourceDao(),
            smsMonitoringConfigDao = database.smsMonitoringConfigDao(),
            unreadMessageDao = database.unreadMessageDao(),
            lastReadSmsDao = database.lastReadSmsDao(),
            smsParser = MultilingualSmsParser(
                languageDetector = SmsLanguageDetector(),
                englishParser = EnglishSmsParser(),
                amharicParser = AmharicSmsParser(),
                oromoParser = OromoSmsParser()
            )
        )
    }
    
    private fun startSmsMonitoringService() {
        try {
            val intent = Intent(this, com.ethiostat.app.service.SmsMonitoringService::class.java).apply {
                action = com.ethiostat.app.service.SmsMonitoringService.ACTION_START_MONITORING
            }
            startService(intent)
            android.util.Log.d("EthioStat", "SMS Monitoring Service started")
        } catch (e: Exception) {
            android.util.Log.e("EthioStat", "Failed to start SMS Monitoring Service: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        // Stop SMS monitoring service
        try {
            val intent = Intent(this, com.ethiostat.app.service.SmsMonitoringService::class.java)
            stopService(intent)
        } catch (e: Exception) {
            android.util.Log.e("EthioStat", "Failed to stop SMS Monitoring Service: ${e.message}")
        }
    }
}
