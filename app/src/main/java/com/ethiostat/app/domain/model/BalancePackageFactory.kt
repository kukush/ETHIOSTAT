package com.ethiostat.app.domain.model

object BalancePackageFactory {
    
    fun createZeroInternetPackage(): BalancePackage {
        return BalancePackage(
            packageType = PackageType.INTERNET,
            packageName = "No Internet Package",
            totalAmount = 0.0,
            remainingAmount = 0.0,
            unit = "MB",
            source = "telebirr",
            validityDays = 0,
            expiryDate = "No expiry",
            expiryTimestamp = 0L,
            language = "en"
        )
    }
    
    fun createZeroVoicePackage(): BalancePackage {
        return BalancePackage(
            packageType = PackageType.VOICE,
            packageName = "No Voice Package",
            totalAmount = 0.0,
            remainingAmount = 0.0,
            unit = "minutes",
            source = "telebirr",
            validityDays = 0,
            expiryDate = "No expiry",
            expiryTimestamp = 0L,
            language = "en"
        )
    }
    
    fun createZeroBonusPackage(): BalancePackage {
        return BalancePackage(
            packageType = PackageType.BONUS_FUND,
            packageName = "No Bonus Fund",
            totalAmount = 0.0,
            remainingAmount = 0.0,
            unit = "Birr",
            source = "telebirr",
            validityDays = 0,
            expiryDate = "No expiry",
            expiryTimestamp = 0L,
            language = "en"
        )
    }
    
    fun createDefaultZeroBalances(): List<BalancePackage> {
        return listOf(
            createZeroInternetPackage(),
            createZeroVoicePackage(),
            createZeroBonusPackage()
        )
    }
    
    fun createZeroBalanceForType(packageType: PackageType): BalancePackage {
        return when (packageType) {
            PackageType.INTERNET -> createZeroInternetPackage()
            PackageType.VOICE -> createZeroVoicePackage()
            PackageType.BONUS_FUND -> createZeroBonusPackage()
            else -> createZeroInternetPackage()
        }
    }
}
