package com.ethiostat.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ethiostat.app.domain.model.BalancePackage
import com.ethiostat.app.domain.model.PackageType
import com.ethiostat.app.ui.theme.FundsAmber
import com.ethiostat.app.ui.theme.InternetBlue
import com.ethiostat.app.ui.theme.VoiceGreen
import kotlin.math.roundToInt

@Composable
fun BalanceCard(
    balance: BalancePackage,
    modifier: Modifier = Modifier
) {
    val cardColor = when (balance.packageType) {
        PackageType.INTERNET -> InternetBlue
        PackageType.VOICE -> VoiceGreen
        PackageType.BONUS_FUND -> FundsAmber
        PackageType.NIGHT_BONUS -> VoiceGreen.copy(alpha = 0.7f)
        else -> Color.Gray
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = balance.packageName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardColor
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = formatAmount(balance.remainingAmount, balance.unit),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "of ${formatAmount(balance.totalAmount, balance.unit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                CircularProgressIndicator(
                    progress = (balance.usagePercentage / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.size(80.dp),
                    color = cardColor,
                    strokeWidth = 8.dp,
                    trackColor = cardColor.copy(alpha = 0.2f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExpiryBadge(balance = balance)
                
                UsageBadge(
                    usagePercentage = balance.usagePercentage,
                    cardColor = cardColor
                )
            }
        }
    }
}

@Composable
private fun ExpiryBadge(balance: BalancePackage) {
    val daysRemaining = balance.daysUntilExpiry
    val (text, color) = when {
        balance.isExpired -> "Expired" to MaterialTheme.colorScheme.error
        daysRemaining == 0 -> "Expires today" to MaterialTheme.colorScheme.error
        daysRemaining == 1 -> "Expires tomorrow" to Color(0xFFFFA726)
        daysRemaining <= 3 -> "$daysRemaining days left" to Color(0xFFFFA726)
        else -> "$daysRemaining days left" to Color(0xFF66BB6A)
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun UsageBadge(usagePercentage: Float, cardColor: Color) {
    Surface(
        color = cardColor.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "${usagePercentage.roundToInt()}% used",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = cardColor,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatAmount(amount: Double, unit: String): String {
    return when (unit.lowercase()) {
        "mb" -> {
            if (amount >= 1024) {
                "%.2f GB".format(amount / 1024)
            } else {
                "%.2f MB".format(amount)
            }
        }
        "minutes" -> {
            val totalMinutes = amount.toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes} min"
            }
        }
        "birr" -> "%.2f Birr".format(amount)
        "coins" -> "${amount.toInt()} Coins"
        else -> "%.2f $unit".format(amount)
    }
}
