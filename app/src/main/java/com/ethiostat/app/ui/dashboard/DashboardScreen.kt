package com.ethiostat.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ethiostat.app.R
import com.ethiostat.app.domain.model.BalancePackage
import com.ethiostat.app.domain.model.TimePeriod
import com.ethiostat.app.ui.components.FinancialSummaryCard
import com.ethiostat.app.ui.theme.FundsAmber
import com.ethiostat.app.ui.theme.InternetBlue
import com.ethiostat.app.ui.theme.PromotionPurple
import com.ethiostat.app.ui.theme.VoiceGreen

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
                    IconButton(onClick = { 
                        android.util.Log.d("EthioStat", "Settings icon clicked")
                        onNavigateToSettings()
                    }) {
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
                !state.hasRealData -> {
                    ZeroBalanceState(state)
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
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            TelecomServiceSection(
                internetPackages = state.internetPackages,
                voicePackages = state.voicePackages,
                bonusFunds = state.bonusFunds
            )
        }

        if (state.transactions.isNotEmpty()) {
            item {
                FinancialSummaryCard(
                    summary = state.financialSummary,
                    selectedPeriod = state.selectedPeriod,
                    onPeriodChange = { onIntent(DashboardIntent.FilterTransactions(it)) }
                )
            }
        }
    }
}

@Composable
private fun TelecomServiceSection(
    internetPackages: List<BalancePackage>,
    voicePackages: List<BalancePackage>,
    bonusFunds: List<BalancePackage>
) {
    val internetPkg = internetPackages.firstOrNull()
    val voicePkg = voicePackages.firstOrNull()
    val bonusPkg = bonusFunds.filter { !it.unit.equals("coins", ignoreCase = true) }.firstOrNull()
        ?: bonusFunds.firstOrNull()
    val promoPkg = bonusFunds.filter { it.unit.equals("coins", ignoreCase = true) }.firstOrNull()
        ?: bonusFunds.firstOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.telecom_service),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryTab(
                    label = stringResource(R.string.internet_balance),
                    value = internetPkg?.let {
                        if (it.remainingAmount >= 1024) "%.1f GB".format(it.remainingAmount / 1024)
                        else "%.0f MB".format(it.remainingAmount)
                    } ?: "--",
                    subValue = internetPkg?.let {
                        "/ " + if (it.totalAmount >= 1024) "%.1f GB".format(it.totalAmount / 1024)
                        else "%.0f MB".format(it.totalAmount)
                    } ?: "",
                    progress = internetPkg?.let {
                        if (it.totalAmount > 0) (it.remainingAmount / it.totalAmount).toFloat().coerceIn(0f, 1f) else 0f
                    } ?: 0f,
                    expiryText = packageExpiryLabel(internetPkg),
                    color = InternetBlue,
                    modifier = Modifier.weight(1f)
                )
                SummaryTab(
                    label = stringResource(R.string.voice_balance),
                    value = voicePkg?.let { "${it.remainingAmount.toInt()} min" } ?: "--",
                    subValue = voicePkg?.let { "/ ${it.totalAmount.toInt()} min" } ?: "",
                    progress = voicePkg?.let {
                        if (it.totalAmount > 0) (it.remainingAmount / it.totalAmount).toFloat().coerceIn(0f, 1f) else 0f
                    } ?: 0f,
                    expiryText = packageExpiryLabel(voicePkg),
                    color = VoiceGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryTab(
                    label = stringResource(R.string.bonus_funds),
                    value = bonusPkg?.let {
                        if (it.unit.equals("coins", ignoreCase = true)) "${it.remainingAmount.toInt()} Coins"
                        else "%.0f Br".format(it.remainingAmount)
                    } ?: "--",
                    subValue = "",
                    progress = bonusPkg?.let {
                        if (it.totalAmount > 0) (it.remainingAmount / it.totalAmount).toFloat().coerceIn(0f, 1f) else 1f
                    } ?: 0f,
                    expiryText = packageExpiryLabel(bonusPkg),
                    color = FundsAmber,
                    modifier = Modifier.weight(1f)
                )
                SummaryTab(
                    label = stringResource(R.string.promotion),
                    value = promoPkg?.let {
                        if (it.unit.equals("coins", ignoreCase = true)) "${it.remainingAmount.toInt()} Coins"
                        else "%.0f Br".format(it.remainingAmount)
                    } ?: "--",
                    subValue = "",
                    progress = promoPkg?.let {
                        if (it.totalAmount > 0) (it.remainingAmount / it.totalAmount).toFloat().coerceIn(0f, 1f) else 1f
                    } ?: 0f,
                    expiryText = packageExpiryLabel(promoPkg),
                    color = PromotionPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun packageExpiryLabel(pkg: BalancePackage?): String {
    if (pkg == null || pkg.expiryTimestamp == 0L) return ""
    return when {
        pkg.isExpired -> "Expired"
        pkg.daysUntilExpiry == 0 -> "Today"
        else -> "${pkg.daysUntilExpiry}d left"
    }
}

@Composable
private fun SummaryTab(
    label: String,
    value: String,
    subValue: String,
    progress: Float,
    expiryText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (subValue.isNotEmpty()) {
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.55f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
            if (expiryText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = expiryText,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ZeroBalanceState(state: DashboardState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            TelecomServiceSection(
                internetPackages = state.internetPackages,
                voicePackages = state.voicePackages,
                bonusFunds = state.bonusFunds
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No balance data available",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the sync icon to load your balances",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
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
