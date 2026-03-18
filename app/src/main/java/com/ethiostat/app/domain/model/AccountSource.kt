package com.ethiostat.app.domain.model

data class AccountSource(
    val id: Long = 0,
    val name: String,
    val type: AccountSourceType,
    val phoneNumber: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AccountSourceType {
    TELEBIRR,
    BANK_CBE,
    BANK_AWASH,
    BANK_OTHER,
    TELECOM
}

data class TransactionSource(
    val sourceId: Long,
    val sourceName: String,
    val sourceType: AccountSourceType,
    val phoneNumber: String
)
