package com.ethiostat.app.domain.model

data class BalancePackage(
    val id: Long = 0,
    val packageType: PackageType,
    val packageName: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val unit: String,
    val source: String,
    val validityDays: Int,
    val expiryDate: String,
    val expiryTimestamp: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val language: String
) {
    val usedAmount: Double
        get() = totalAmount - remainingAmount
    
    val usagePercentage: Float
        get() = if (totalAmount > 0) {
            ((totalAmount - remainingAmount) / totalAmount * 100).toFloat()
        } else 0f
    
    val isExpired: Boolean
        get() = expiryTimestamp < System.currentTimeMillis()
    
    val daysUntilExpiry: Int
        get() {
            val diff = expiryTimestamp - System.currentTimeMillis()
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }
    
    companion object {
        fun createZeroInternet(): BalancePackage {
            return BalancePackage(
                packageType = PackageType.INTERNET,
                packageName = "Internet Balance",
                totalAmount = 0.0,
                remainingAmount = 0.0,
                unit = "GB",
                source = "Ethio Telecom",
                validityDays = 0,
                expiryDate = "No data",
                expiryTimestamp = 0L,
                language = "en"
            )
        }
        
        fun createZeroVoice(): BalancePackage {
            return BalancePackage(
                packageType = PackageType.VOICE,
                packageName = "Voice Balance",
                totalAmount = 0.0,
                remainingAmount = 0.0,
                unit = "min",
                source = "Ethio Telecom",
                validityDays = 0,
                expiryDate = "No data",
                expiryTimestamp = 0L,
                language = "en"
            )
        }
        
        fun createZeroBonus(): BalancePackage {
            return BalancePackage(
                packageType = PackageType.BONUS_FUND,
                packageName = "Bonus Funds",
                totalAmount = 0.0,
                remainingAmount = 0.0,
                unit = "Birr",
                source = "Ethio Telecom",
                validityDays = 0,
                expiryDate = "No data",
                expiryTimestamp = 0L,
                language = "en"
            )
        }
        
        fun createZeroPromotion(): BalancePackage {
            return BalancePackage(
                packageType = PackageType.BONUS_FUND,
                packageName = "Promotion",
                totalAmount = 0.0,
                remainingAmount = 0.0,
                unit = "Coins",
                source = "Ethio Telecom",
                validityDays = 0,
                expiryDate = "No data",
                expiryTimestamp = 0L,
                language = "en"
            )
        }
    }
}
