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
}
