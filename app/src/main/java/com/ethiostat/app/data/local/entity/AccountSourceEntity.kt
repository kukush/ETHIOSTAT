package com.ethiostat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ethiostat.app.domain.model.AccountSource
import com.ethiostat.app.domain.model.AccountSourceType

@Entity(tableName = "account_sources")
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
        type = AccountSourceType.valueOf(type),
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
