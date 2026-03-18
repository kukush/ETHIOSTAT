package com.ethiostat.app.data.local.dao

import androidx.room.*
import com.ethiostat.app.data.local.entity.LastReadSmsEntity

@Dao
interface LastReadSmsDao {
    
    @Query("SELECT * FROM last_read_sms WHERE phoneNumber = :phoneNumber")
    suspend fun getLastReadSms(phoneNumber: String): LastReadSmsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(lastReadSms: LastReadSmsEntity)
    
    @Update
    suspend fun update(lastReadSms: LastReadSmsEntity)
    
    @Delete
    suspend fun delete(lastReadSms: LastReadSmsEntity)
    
    @Query("DELETE FROM last_read_sms WHERE phoneNumber = :phoneNumber")
    suspend fun deleteByPhoneNumber(phoneNumber: String)
    
    @Query("SELECT * FROM last_read_sms ORDER BY updatedAt DESC")
    suspend fun getAllLastReadSms(): List<LastReadSmsEntity>
}
