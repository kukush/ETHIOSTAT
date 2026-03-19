package com.ethiostat.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.ethiostat.app.ui.components.UnreadMessageIndicator
import com.ethiostat.app.ui.settings.AccountSourcesScreen
import com.ethiostat.app.ui.theme.FundsAmber
import com.ethiostat.app.ui.theme.InternetBlue
import com.ethiostat.app.ui.theme.SmsTeal
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Unread message indicator on the left
                        UnreadMessageIndicator(
                            unreadCount = state.unreadMessageCount,
                            onClick = { onIntent(DashboardIntent.ShowUnreadMessages) }
                        )
                        
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        // Spacer to balance the layout
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(DashboardIntent.RefreshUssd804) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh with *804#"
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
                mainBalance = state.mainBalance,
                internetPackages = state.internetPackages,
                voicePackages = state.voicePackages,
                bonusFunds = state.bonusFunds,
                smsPackages = state.smsPackages
            )
        }

        if (state.transactions.isNotEmpty()) {
            item {
                FinancialSummaryCard(
                    summary = state.financialSummary,
                    selectedPeriod = state.selectedPeriod,
                    selectedSourceFilter = state.selectedSourceFilter,
                    showNetBalance = state.showNetBalance,
                    accountSources = state.accountSources,
                    selectedAccountSource = state.selectedAccountSource,
                    onPeriodChange = { onIntent(DashboardIntent.FilterTransactions(it)) },
                    onSourceFilterChange = { onIntent(DashboardIntent.FilterBySource(it)) },
                    onAccountSourceChange = { onIntent(DashboardIntent.SelectAccountSource(it)) },
                    onAddSource = { onIntent(DashboardIntent.ShowAccountSourcesScreen) },
                    onToggleNetBalance = { onIntent(DashboardIntent.ToggleNetBalanceVisibility) }
                )
            }
        }
    }

    // Account Sources Screen
    if (state.showAccountSourcesScreen) {
        AccountSourcesScreen(
            accountSources = state.accountSources,
            onAddSource = { source ->
                onIntent(DashboardIntent.AddAccountSource(source))
            },
            onEditSource = { source ->
                onIntent(DashboardIntent.EditAccountSource(source))
            },
            onDeleteSource = { source ->
                onIntent(DashboardIntent.DeleteAccountSource(source))
            },
            onToggleSource = { source ->
                onIntent(DashboardIntent.ToggleAccountSource(source))
            },
            onNavigateBack = { onIntent(DashboardIntent.HideAccountSourcesScreen) }
        )
    }
}

@Composable
private fun TelecomServiceSection(
    mainBalance: List<BalancePackage>,
    internetPackages: List<BalancePackage>,
    voicePackages: List<BalancePackage>,
    bonusFunds: List<BalancePackage>,
    smsPackages: List<BalancePackage>
) {
    var isExpanded by androidx.compose.runtime.remember { mutableStateOf(true) }
    
    val internetPkg = internetPackages.firstOrNull() ?: BalancePackage.createZeroInternet()
    val voicePkg = voicePackages.firstOrNull() ?: BalancePackage.createZeroVoice()
    val bonusPkg = bonusFunds.filter { !it.unit.equals("coins", ignoreCase = true) }.firstOrNull()
        ?: BalancePackage.createZeroBonus()
    val smsPkg = smsPackages.firstOrNull() ?: BalancePackage.createZeroSms()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.telecom_service),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Display account balance in Birr from MAIN_BALANCE or specific fragments
                    val accountBalance = mainBalance.firstOrNull() 
                        ?: bonusFunds.firstOrNull { it.packageName == "Account Balance" }
                    if (accountBalance != null) {
                        Text(
                            text = "%.2f Birr".format(accountBalance.remainingAmount),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryTab(
                        label = stringResource(R.string.internet_balance),
                        value = if (internetPkg.remainingAmount >= 1024) "%.1f GB".format(internetPkg.remainingAmount / 1024)
                            else "%.0f MB".format(internetPkg.remainingAmount),
                        subValue = "/ " + if (internetPkg.totalAmount >= 1024) "%.1f GB".format(internetPkg.totalAmount / 1024)
                            else "%.0f MB".format(internetPkg.totalAmount),
                        progress = if (internetPkg.totalAmount > 0) (internetPkg.remainingAmount / internetPkg.totalAmount).toFloat().coerceIn(0f, 1f) else 0f,
                        expiryText = packageExpiryLabel(internetPkg),
                        color = InternetBlue,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryTab(
                        label = stringResource(R.string.voice_balance),
                        value = "${voicePkg.remainingAmount.toInt()} min",
                        subValue = "/ ${voicePkg.totalAmount.toInt()} min",
                        progress = if (voicePkg.totalAmount > 0) (voicePkg.remainingAmount / voicePkg.totalAmount).toFloat().coerceIn(0f, 1f) else 0f,
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
                        value = if (bonusPkg.unit.equals("coins", ignoreCase = true)) "${bonusPkg.remainingAmount.toInt()} Coins"
                            else "%.0f Br".format(bonusPkg.remainingAmount),
                        subValue = "",
                        progress = if (bonusPkg.totalAmount > 0) (bonusPkg.remainingAmount / bonusPkg.totalAmount).toFloat().coerceIn(0f, 1f) else 1f,
                        expiryText = packageExpiryLabel(bonusPkg),
                        color = FundsAmber,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryTab(
                        label = stringResource(R.string.sms_balance),
                        value = "${smsPkg.remainingAmount.toInt()} SMS",
                        subValue = if (smsPkg.totalAmount > 0) "/ ${smsPkg.totalAmount.toInt()} SMS" else "",
                        progress = if (smsPkg.totalAmount > 0) (smsPkg.remainingAmount / smsPkg.totalAmount).toFloat().coerceIn(0f, 1f) else 1f,
                        expiryText = packageExpiryLabel(smsPkg),
                        color = SmsTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun packageExpiryLabel(pkg: BalancePackage): String {
    if (pkg.expiryTimestamp == 0L) return pkg.expiryDate
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
                bonusFunds = state.bonusFunds,
                smsPackages = state.smsPackages
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
                        text = "Tap the sync icon or scan your inbox",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onIntent(DashboardIntent.ScanSmsHistory) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Scan SMS Inbox")
                    }
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
