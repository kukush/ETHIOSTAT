package com.ethiostat.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ethiostat.app.R
import com.ethiostat.app.ui.components.BalanceCard
import com.ethiostat.app.ui.components.FinancialSummaryCard

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    
    DashboardContent(
        state = state,
        onIntent = { viewModel.processIntent(it) },
        onNavigateToSettings = onNavigateToSettings,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { onIntent(DashboardIntent.RefreshBalances) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onIntent(DashboardIntent.SyncUssd("*805#")) },
                icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                text = { Text(stringResource(R.string.sync_now)) },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        Box(modifier = modifier.padding(paddingValues)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    ErrorMessage(
                        message = state.error,
                        onDismiss = { onIntent(DashboardIntent.ClearError) }
                    )
                }
                state.balances.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    BalanceList(
                        state = state,
                        onIntent = onIntent
                    )
                }
            }
            
            if (state.syncSuccess) {
                SuccessSnackbar(
                    onDismiss = { onIntent(DashboardIntent.ClearError) }
                )
            }
        }
    }
}

@Composable
private fun BalanceList(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            SummaryHeader(
                internetMB = state.totalInternetMB,
                voiceMinutes = state.totalVoiceMinutes,
                bonusBirr = state.totalBonusBirr
            )
        }
        
        if (state.internetPackages.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.internet_balance))
            }
            items(state.internetPackages) { balance ->
                BalanceCard(balance = balance)
            }
        }
        
        if (state.voicePackages.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.voice_balance))
            }
            items(state.voicePackages) { balance ->
                BalanceCard(balance = balance)
            }
        }
        
        if (state.bonusFunds.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.bonus_funds))
            }
            items(state.bonusFunds) { balance ->
                BalanceCard(balance = balance)
            }
        }
        
        if (state.transactions.isNotEmpty()) {
            item {
                FinancialSummaryCard(summary = state.financialSummary)
            }
        }
    }
}

@Composable
private fun SummaryHeader(
    internetMB: Double,
    voiceMinutes: Double,
    bonusBirr: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickStat(
                value = if (internetMB >= 1024) "%.1f GB".format(internetMB / 1024) else "%.0f MB".format(internetMB),
                label = stringResource(R.string.internet_balance),
                modifier = Modifier.weight(1f)
            )
            QuickStat(
                value = "${voiceMinutes.toInt()} min",
                label = stringResource(R.string.voice_balance),
                modifier = Modifier.weight(1f)
            )
            QuickStat(
                value = "%.0f Br".format(bonusBirr),
                label = stringResource(R.string.bonus_funds),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.no_packages),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap Sync to load your balances",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun SuccessSnackbar(onDismiss: () -> Unit) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    ) {
        Text(stringResource(R.string.sync_success))
    }
}
