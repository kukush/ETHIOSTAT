package com.ethiostat.app.ui

import android.Manifest
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
import com.ethiostat.app.data.parser.SmsLanguageDetector
import com.ethiostat.app.data.repository.EthioStatRepositoryImpl
import com.ethiostat.app.domain.usecase.ChangeLanguageUseCase
import com.ethiostat.app.domain.usecase.GetFinancialSummaryUseCase
import com.ethiostat.app.domain.usecase.SyncBalanceUseCase
import com.ethiostat.app.ui.dashboard.DashboardScreen
import com.ethiostat.app.ui.dashboard.DashboardViewModel
import com.ethiostat.app.ui.theme.EthioStatTheme

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        val readSmsGranted = permissions[Manifest.permission.READ_SMS] == true
        val callGranted = permissions[Manifest.permission.CALL_PHONE] == true
        
        if (!smsGranted || !readSmsGranted) {
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
            smsParser = MultilingualSmsParser(
                languageDetector = SmsLanguageDetector(),
                englishParser = EnglishSmsParser(),
                amharicParser = AmharicSmsParser()
            )
        )
        
        val viewModel = DashboardViewModel(
            repository = repository,
            getFinancialSummaryUseCase = GetFinancialSummaryUseCase(),
            syncBalanceUseCase = SyncBalanceUseCase(applicationContext),
            changeLanguageUseCase = ChangeLanguageUseCase(repository)
        )
        
        setContent {
            EthioStatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSettings by remember { mutableStateOf(false) }
                    
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { showSettings = true }
                    )
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
}
