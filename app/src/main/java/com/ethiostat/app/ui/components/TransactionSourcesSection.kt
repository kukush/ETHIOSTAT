package com.ethiostat.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ethiostat.app.domain.model.AccountSource
import com.ethiostat.app.domain.model.AccountSourceType
import com.ethiostat.app.domain.usecase.TransactionSummary

@Composable
fun TransactionSourcesSection(
    accountSources: List<AccountSource>,
    transactionSummaryBySource: Map<AccountSourceType, TransactionSummary>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Transaction Sources",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        accountSources.forEach { source ->
            val summary = transactionSummaryBySource[source.type] ?: TransactionSummary(
                totalIncome = 0.0,
                totalExpense = 0.0,
                transactionCount = 0,
                lastTransactionTime = 0L
            )
            
            TransactionSourceCard(
                sourceType = source.type,
                sourceName = source.displayName,
                summary = summary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        if (accountSources.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "No transaction sources configured",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add transaction sources in Settings to track your financial activity by bank or mobile money service.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
