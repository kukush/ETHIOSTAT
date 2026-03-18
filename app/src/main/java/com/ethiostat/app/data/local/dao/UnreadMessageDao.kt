package com.ethiostat.app.data.local.dao

import androidx.room.*
import com.ethiostat.app.data.local.entity.UnreadMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnreadMessageDao {
    
    @Query("SELECT * FROM unread_messages WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadMessages(): Flow<List<UnreadMessageEntity>>
    
    @Query("SELECT * FROM unread_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<UnreadMessageEntity>>
    
    @Query("SELECT COUNT(*) FROM unread_messages WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM unread_messages WHERE isRead = 0 AND priority IN ('HIGH', 'URGENT')")
    fun getHighPriorityUnreadCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM unread_messages WHERE isRead = 0 AND priority = 'URGENT'")
    fun getUrgentUnreadCount(): Flow<Int>
    
    @Query("SELECT * FROM unread_messages WHERE id = :id")
    suspend fun getMessageById(id: Long): UnreadMessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: UnreadMessageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<UnreadMessageEntity>)
    
    @Update
    suspend fun updateMessage(message: UnreadMessageEntity)
    
    @Delete
    suspend fun deleteMessage(message: UnreadMessageEntity)
    
    @Query("UPDATE unread_messages SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
    
    @Query("UPDATE unread_messages SET isRead = 1")
    suspend fun markAllAsRead()
    
    @Query("DELETE FROM unread_messages WHERE isRead = 1 AND timestamp < :olderThan")
    suspend fun deleteOldReadMessages(olderThan: Long)
}
