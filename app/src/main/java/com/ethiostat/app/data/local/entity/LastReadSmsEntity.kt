package com.ethiostat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "last_read_sms")
data class LastReadSmsEntity(
    @PrimaryKey val phoneNumber: String,
    val lastReadTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
