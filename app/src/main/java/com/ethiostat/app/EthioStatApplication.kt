package com.ethiostat.app

import android.app.Application
import com.ethiostat.app.BuildConfig
import com.ethiostat.app.data.local.EthioStatDatabase
import com.ethiostat.app.data.local.entity.AppConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EthioStatApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        initializeDatabase()
    }
    
    private fun initializeDatabase() {
        applicationScope.launch {
            val database = EthioStatDatabase.getDatabase(applicationContext)
            val configDao = database.configDao()
            
            val existingConfig = configDao.getConfigOnce()
            
            if (existingConfig == null) {
                val defaultConfig = AppConfigEntity(
                    id = 1,
                    telecomSenders = BuildConfig.DEFAULT_TELECOM_SENDER,
                    telebirrSenders = BuildConfig.DEFAULT_TELEBIRR_SENDER,
                    bankSenders = BuildConfig.DEFAULT_BANK_SENDERS,
                    ussdBalanceCode = BuildConfig.DEFAULT_USSD_BALANCE,
                    ussdPackagesCode = BuildConfig.DEFAULT_USSD_PACKAGES,
                    ussdDataCheckCode = BuildConfig.DEFAULT_USSD_DATA_CHECK,
                    appLanguage = BuildConfig.DEFAULT_LANGUAGE
                )
                
                configDao.insertConfig(defaultConfig)
            }
        }
    }
}
