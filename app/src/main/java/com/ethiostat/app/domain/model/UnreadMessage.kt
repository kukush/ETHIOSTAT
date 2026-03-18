package com.ethiostat.app.domain.model

data class UnreadMessage(
    val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val messageType: MessageType = MessageType.INFO,
    val priority: MessagePriority = MessagePriority.NORMAL
)

enum class MessageType {
    INFO,
    WARNING,
    ERROR,
    SUCCESS,
    TRANSACTION_ALERT,
    SYSTEM_UPDATE
}

enum class MessagePriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

data class UnreadMessageCount(
    val totalCount: Int,
    val highPriorityCount: Int,
    val urgentCount: Int
) {
    val hasUnread: Boolean get() = totalCount > 0
    val hasHighPriority: Boolean get() = highPriorityCount > 0 || urgentCount > 0
}
