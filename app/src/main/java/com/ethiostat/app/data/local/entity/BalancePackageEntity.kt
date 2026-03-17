package com.ethiostat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ethiostat.app.domain.model.BalancePackage
import com.ethiostat.app.domain.model.PackageType

@Entity(tableName = "balance_packages")
data class BalancePackageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageType: String,
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
)

fun BalancePackageEntity.toDomain(): BalancePackage {
    return BalancePackage(
        id = id,
        packageType = PackageType.valueOf(packageType),
        packageName = packageName,
        totalAmount = totalAmount,
        remainingAmount = remainingAmount,
        unit = unit,
        source = source,
        validityDays = validityDays,
        expiryDate = expiryDate,
        expiryTimestamp = expiryTimestamp,
        createdAt = createdAt,
        language = language
    )
}

fun BalancePackage.toEntity(): BalancePackageEntity {
    return BalancePackageEntity(
        id = id,
        packageType = packageType.name,
        packageName = packageName,
        totalAmount = totalAmount,
        remainingAmount = remainingAmount,
        unit = unit,
        source = source,
        validityDays = validityDays,
        expiryDate = expiryDate,
        expiryTimestamp = expiryTimestamp,
        createdAt = createdAt,
        language = language
    )
}
