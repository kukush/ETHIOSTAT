package com.ethiostat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ethiostat.app.domain.repository.AppConfig

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1,
    val lastReadTimestamp: Long = 0,
    val telecomSenders: String = "",
    val telebirrSenders: String = "",
    val bankSenders: String = "",
    val ussdBalanceCode: String = "",
    val ussdPackagesCode: String = "",
    val ussdDataCheckCode: String = "",
    val appLanguage: String = "en",
    val parseEnglishSms: Boolean = true,
    val parseAmharicSms: Boolean = true,
    val parseOromiffaSms: Boolean = false,
    val parseTigrinyaSms: Boolean = false,
    val showExpiredPackages: Boolean = true,
    val expiryWarningDays: Int = 3,
    val currencySymbol: String = "Birr",
    val enableSmsLogging: Boolean = false,
    val logUnparsedSms: Boolean = false
)

fun AppConfigEntity.toDomain(): AppConfig {
    return AppConfig(
        id = id,
        lastReadTimestamp = lastReadTimestamp,
        telecomSenders = telecomSenders,
        telebirrSenders = telebirrSenders,
        bankSenders = bankSenders,
        ussdBalanceCode = ussdBalanceCode,
        ussdPackagesCode = ussdPackagesCode,
        ussdDataCheckCode = ussdDataCheckCode,
        appLanguage = appLanguage,
        parseEnglishSms = parseEnglishSms,
        parseAmharicSms = parseAmharicSms,
        parseOromiffaSms = parseOromiffaSms,
        parseTigrinyaSms = parseTigrinyaSms,
        showExpiredPackages = showExpiredPackages,
        expiryWarningDays = expiryWarningDays,
        currencySymbol = currencySymbol,
        enableSmsLogging = enableSmsLogging,
        logUnparsedSms = logUnparsedSms
    )
}

fun AppConfig.toEntity(): AppConfigEntity {
    return AppConfigEntity(
        id = id,
        lastReadTimestamp = lastReadTimestamp,
        telecomSenders = telecomSenders,
        telebirrSenders = telebirrSenders,
        bankSenders = bankSenders,
        ussdBalanceCode = ussdBalanceCode,
        ussdPackagesCode = ussdPackagesCode,
        ussdDataCheckCode = ussdDataCheckCode,
        appLanguage = appLanguage,
        parseEnglishSms = parseEnglishSms,
        parseAmharicSms = parseAmharicSms,
        parseOromiffaSms = parseOromiffaSms,
        parseTigrinyaSms = parseTigrinyaSms,
        showExpiredPackages = showExpiredPackages,
        expiryWarningDays = expiryWarningDays,
        currencySymbol = currencySymbol,
        enableSmsLogging = enableSmsLogging,
        logUnparsedSms = logUnparsedSms
    )
}
