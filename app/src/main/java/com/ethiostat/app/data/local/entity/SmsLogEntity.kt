package com.ethiostat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_log")
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val parsed: Boolean,
    val language: String? = null,
    val errorMessage: String? = null
)
