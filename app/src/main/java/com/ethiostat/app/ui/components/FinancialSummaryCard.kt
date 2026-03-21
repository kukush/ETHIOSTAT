package com.ethiostat.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethiostat.app.R
import com.ethiostat.app.domain.model.FinancialSummary
import com.ethiostat.app.domain.model.TimePeriod
import com.ethiostat.app.domain.model.AccountSourceType
import com.ethiostat.app.ui.theme.ErrorRed
import com.ethiostat.app.ui.theme.SuccessGreen
import com.ethiostat.app.ui.settings.getDisplayNameResId

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FinancialSummaryCard(
    summary: FinancialSummary,
    selectedPeriod: TimePeriod = TimePeriod.WEEKLY,
    showNetBalance: Boolean = true,
    accountSources: List<com.ethiostat.app.domain.model.AccountSource> = emptyList(),
    selectedAccountSource: com.ethiostat.app.domain.model.AccountSource? = null,
    onPeriodChange: (TimePeriod) -> Unit = {},
    onAccountSourceChange: (com.ethiostat.app.domain.model.AccountSource?) -> Unit = {},
    onAddSource: () -> Unit = {},
    onToggleNetBalance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Dashboard Title

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Row (Full Width)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.transaction_history),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = onAddSource) {
                            Icon(Icons.Default.Add, "Add", tint = Color.White)
                        }
                    }
                }

                // Body content with padding
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {

                Spacer(modifier = Modifier.height(8.dp))

        
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = Color.LightGray.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            TimePeriod.DAILY to stringResource(R.string.daily),
                            TimePeriod.WEEKLY to stringResource(R.string.weekly),
                            TimePeriod.MONTHLY to stringResource(R.string.monthly)
                        ).forEach { (period, label) ->
                            val isSelected = selectedPeriod == period
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPeriodChange(period) },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFFD1A7D1) else Color.Transparent // Lavender color from image
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF4A148C) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

            
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = Color.LightGray.copy(alpha = 0.2f)
                ) {
                FlowRow(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 4
                ) {
                    // All Sources
                    SourceTab(
                        label = stringResource(R.string.all_sources),
                        isSelected = selectedAccountSource == null,
                        onClick = { onAccountSourceChange(null) }
                    )
                    
                    accountSources.filter { it.isEnabled }.forEach { source ->
                        // Always use the localized abbreviation (e.g. CBE, BOA, Telebirr) for the dashboard tabs
                        val displayStr = stringResource(id = source.type.getDisplayNameResId())
                        SourceTab(
                            label = displayStr,
                            isSelected = selectedAccountSource?.id == source.id,
                            onClick = { onAccountSourceChange(source) }
                        )
                    }
                }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                FinancialMetricCard(
                    label = stringResource(R.string.total_income),
                    amount = summary.totalIncome,
                    color = Color(0xFF81C784), // Soft green
                    icon = Icons.Default.TrendingUp,
                    isPositive = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                FinancialMetricCard(
                    label = stringResource(R.string.total_expense),
                    amount = summary.totalExpense,
                    color = Color(0xFFE57373), // Soft red/coral
                    icon = Icons.Default.TrendingDown,
                    isPositive = false
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                // Net Balance Visibility Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                    color = Color.White
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showNetBalance) {
                                    "${if (summary.netBalance >= 0) "+" else ""}%.2f ${stringResource(R.string.birr_unit)}".format(summary.netBalance)
                                } else {
                                    stringResource(R.string.net_balance)
                                },
                                style = if (showNetBalance) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (showNetBalance) {
                                    if (summary.netBalance >= 0) SuccessGreen else ErrorRed
                                } else Color(0xFF1A237E)
                            )
                            IconButton(onClick = onToggleNetBalance) {
                                Icon(
                                    imageVector = if (showNetBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = Color(0xFF1A237E),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        if (!showNetBalance) {
                            Text(
                                text = "••••••",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.LightGray
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(2.dp)
            .clickable { onClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        color = if (isSelected) Color(0xFFD1A7D1) else Color.Transparent
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isSelected) Color(0xFF4A148C) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FinancialMetricCard(
    label: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPositive: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPositive) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                modifier = Modifier.size(48.dp)
            )
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                )
                Text(
                    text = "%.2f %s".format(amount, stringResource(R.string.birr_unit)),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
