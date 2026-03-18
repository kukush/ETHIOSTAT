package com.ethiostat.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ethiostat.app.R
import com.ethiostat.app.domain.model.FinancialSummary
import com.ethiostat.app.domain.model.TimePeriod
import com.ethiostat.app.domain.model.AccountSourceType
import com.ethiostat.app.ui.theme.ErrorRed
import com.ethiostat.app.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSummaryCard(
    summary: FinancialSummary,
    selectedPeriod: TimePeriod = TimePeriod.WEEKLY,
    selectedSourceFilter: AccountSourceType? = null,
    showNetBalance: Boolean = true,
    onPeriodChange: (TimePeriod) -> Unit = {},
    onSourceFilterChange: (AccountSourceType?) -> Unit = {},
    onToggleNetBalance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Text(
                    text = stringResource(R.string.transaction_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    TimePeriod.DAILY to stringResource(R.string.daily),
                    TimePeriod.WEEKLY to stringResource(R.string.weekly),
                    TimePeriod.MONTHLY to stringResource(R.string.monthly)
                ).forEach { (period, label) ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodChange(period) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedSourceFilter == null,
                    onClick = { onSourceFilterChange(null) },
                    label = { Text("All", style = MaterialTheme.typography.labelMedium) }
                )
                listOf(
                    AccountSourceType.TELEBIRR to "Telebirr",
                    AccountSourceType.BANK_CBE to "CBE",
                    AccountSourceType.BANK_AWASH to "Awash"
                ).forEach { (sourceType, label) ->
                    FilterChip(
                        selected = selectedSourceFilter == sourceType,
                        onClick = { onSourceFilterChange(sourceType) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FinancialItem(
                    label = stringResource(R.string.total_income),
                    amount = summary.totalIncome,
                    color = SuccessGreen,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FinancialItem(
                    label = stringResource(R.string.total_expense),
                    amount = summary.totalExpense,
                    color = ErrorRed,
                    icon = Icons.Default.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.net_balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleNetBalance,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (showNetBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showNetBalance) "Hide net balance" else "Show net balance",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    val netColor = if (summary.netBalance >= 0) SuccessGreen else ErrorRed
                    
                    Text(
                        text = if (showNetBalance) {
                            "${if (summary.netBalance >= 0) "+" else ""}%.2f Birr".format(summary.netBalance)
                        } else {
                            "••••••"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (showNetBalance) netColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialItem(
    label: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "%.2f Birr".format(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
