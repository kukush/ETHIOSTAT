package com.ethiostat.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ethiostat.app.domain.model.UnreadMessage
import com.ethiostat.app.domain.model.MessageType
import com.ethiostat.app.domain.model.MessagePriority

@Entity(tableName = "unread_messages")
data class UnreadMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val messageType: String = MessageType.INFO.name,
    val priority: String = MessagePriority.NORMAL.name
)

fun UnreadMessageEntity.toDomain(): UnreadMessage {
    return UnreadMessage(
        id = id,
        title = title,
        content = content,
        timestamp = timestamp,
        isRead = isRead,
        messageType = MessageType.valueOf(messageType),
        priority = MessagePriority.valueOf(priority)
    )
}

fun UnreadMessage.toEntity(): UnreadMessageEntity {
    return UnreadMessageEntity(
        id = id,
        title = title,
        content = content,
        timestamp = timestamp,
        isRead = isRead,
        messageType = messageType.name,
        priority = priority.name
    )
}
