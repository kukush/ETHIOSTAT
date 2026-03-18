package com.ethiostat.app.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class SyncBalanceUseCase(
    private val context: Context
) {
    companion object {
        private const val TELECOM_USSD_CODE = "*804#"
    }
    
    operator fun invoke(ussdCode: String = TELECOM_USSD_CODE): Result<Unit> {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
                return Result.failure(SecurityException("CALL_PHONE permission not granted"))
            }
            
            val encodedCode = Uri.encode(ussdCode)
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$encodedCode")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Specific method for telecom service data refresh
    fun refreshTelecomData(): Result<Unit> {
        return invoke(TELECOM_USSD_CODE)
    }
}
