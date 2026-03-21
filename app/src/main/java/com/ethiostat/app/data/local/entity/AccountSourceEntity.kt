package com.ethiostat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ethiostat.app.domain.model.AccountSource
import com.ethiostat.app.domain.model.AccountSourceType

@Entity(
    tableName = "account_sources",
    indices = [androidx.room.Index(value = ["type"], unique = true)]
)
data class AccountSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val phoneNumber: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

fun AccountSourceEntity.toDomain(): AccountSource {
    return AccountSource(
        id = id,
        name = name,
        type = try {
            AccountSourceType.valueOf(type)
        } catch (e: Exception) {
            // Map legacy names to new ones or fallback to UNKNOWN
            when (type) {
                "BANK_CBE" -> AccountSourceType.CBE
                "BANK_BOA" -> AccountSourceType.BOA
                "BANK_AWASH" -> AccountSourceType.AWASH_BIRR
                "TELEBIRR_SERVICE" -> AccountSourceType.TELEBIRR
                else -> AccountSourceType.UNKNOWN
            }
        },
        phoneNumber = phoneNumber,
        displayName = displayName,
        isEnabled = isEnabled,
        createdAt = createdAt
    )
}

fun AccountSource.toEntity(): AccountSourceEntity {
    return AccountSourceEntity(
        id = id,
        name = name,
        type = type.name,
        phoneNumber = phoneNumber,
        displayName = displayName,
        isEnabled = isEnabled,
        createdAt = createdAt
    )
}
