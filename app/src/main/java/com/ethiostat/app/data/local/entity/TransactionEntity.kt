package com.ethiostat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ethiostat.app.domain.model.Transaction
import com.ethiostat.app.domain.model.TransactionType
import com.ethiostat.app.domain.model.AccountSourceType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val category: String,
    val source: String,
    val description: String,
    val timestamp: Long,
    val accountSource: String? = null,
    val sourcePhoneNumber: String? = null,
    val isClassified: Boolean = false
)

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        amount = amount,
        type = TransactionType.valueOf(type),
        category = category,
        source = source,
        description = description,
        timestamp = timestamp,
        accountSource = accountSource?.let { AccountSourceType.valueOf(it) },
        sourcePhoneNumber = sourcePhoneNumber,
        isClassified = isClassified
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount,
        type = type.name,
        category = category,
        source = source,
        description = description,
        timestamp = timestamp,
        accountSource = accountSource?.name,
        sourcePhoneNumber = sourcePhoneNumber,
        isClassified = isClassified
    )
}
