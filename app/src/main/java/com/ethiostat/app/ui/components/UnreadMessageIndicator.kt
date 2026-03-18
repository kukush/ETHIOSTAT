package com.ethiostat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiostat.app.domain.model.UnreadMessageCount

@Composable
fun UnreadMessageIndicator(
    unreadCount: UnreadMessageCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = when {
        unreadCount.hasHighPriority -> MaterialTheme.colorScheme.error
        unreadCount.hasUnread -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val badgeColor = when {
        unreadCount.hasHighPriority -> MaterialTheme.colorScheme.error
        unreadCount.hasUnread -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val badgeText = if (unreadCount.totalCount > 99) "99+" else unreadCount.totalCount.toString()

    Box(
        modifier = modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Unread messages",
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )
        
        // Badge with count (always visible so user can see zero state)
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(badgeColor)
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UnreadMessageDialog(
    messages: List<com.ethiostat.app.domain.model.UnreadMessage>,
    onDismiss: () -> Unit,
    onMarkAsRead: (Long) -> Unit,
    onMarkAllAsRead: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Unread Messages")
                if (messages.isNotEmpty()) {
                    TextButton(onClick = onMarkAllAsRead) {
                        Text("Mark All Read")
                    }
                }
            }
        },
        text = {
            if (messages.isEmpty()) {
                Text("No unread messages")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(count = messages.size) { index ->
                        val message = messages[index]
                        UnreadMessageItem(
                            message = message,
                            onMarkAsRead = { onMarkAsRead(message.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun UnreadMessageItem(
    message: com.ethiostat.app.domain.model.UnreadMessage,
    onMarkAsRead: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (message.priority) {
                com.ethiostat.app.domain.model.MessagePriority.URGENT -> 
                    MaterialTheme.colorScheme.errorContainer
                com.ethiostat.app.domain.model.MessagePriority.HIGH -> 
                    MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = message.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onMarkAsRead,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(
                        text = "Mark Read",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> "${diff / 604800_000}w ago"
    }
}
