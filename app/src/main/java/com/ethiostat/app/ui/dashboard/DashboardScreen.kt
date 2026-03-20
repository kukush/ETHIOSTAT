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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    ZeroBalanceState(state, onIntent)
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
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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

        item {
            FinancialSummaryCard(
                summary = state.financialSummary,
                selectedPeriod = state.selectedPeriod,
                showNetBalance = state.showNetBalance,
                accountSources = state.accountSources,
                selectedAccountSource = state.selectedAccountSource,
                onPeriodChange = { onIntent(DashboardIntent.FilterTransactions(it)) },
                onAccountSourceChange = { onIntent(DashboardIntent.SelectAccountSource(it)) },
                onAddSource = { onIntent(DashboardIntent.ShowAccountSourcesScreen) },
                onToggleNetBalance = { onIntent(DashboardIntent.ToggleNetBalanceVisibility) }
            )
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
            onResetTransactions = { source, fromTimestamp ->
                onIntent(
                    DashboardIntent.ResetTransactionsForSource(
                        accountSourceType = source.type.name,
                        fromTimestamp = fromTimestamp
                    )
                )
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

    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.telecom_service),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        
        if (isExpanded) {
            // First Row: Internet and Voice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryTab(
                    label = stringResource(R.string.internet),
                    value = if (internetPkg.remainingAmount >= 1024) "%.1f GB".format(internetPkg.remainingAmount / 1024)
                        else "%.0f MB".format(internetPkg.remainingAmount),
                    progress = if (internetPkg.totalAmount > 0) (internetPkg.remainingAmount / internetPkg.totalAmount).toFloat().coerceIn(0f, 1f) else 0f,
                    expiryText = packageExpiryLabel(internetPkg).replace(" left", ""),
                    color = InternetBlue,
                    icon = Icons.Default.Public,
                    modifier = Modifier.weight(1f)
                )
                SummaryTab(
                    label = stringResource(R.string.voice),
                    value = "${voicePkg.remainingAmount.toInt()} Min",
                    progress = if (voicePkg.totalAmount > 0) (voicePkg.remainingAmount / voicePkg.totalAmount).toFloat().coerceIn(0f, 1f) else 0f,
                    expiryText = packageExpiryLabel(voicePkg).replace(" left", ""),
                    color = VoiceGreen,
                    icon = Icons.Default.Call,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Second Row: Bonus and SMS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryTab(
                    label = stringResource(R.string.bonus),
                    value = if (bonusPkg.unit.equals("coins", ignoreCase = true)) "${bonusPkg.remainingAmount.toInt()} Coins"
                        else "%.0f Br".format(bonusPkg.remainingAmount),
                    progress = if (bonusPkg.totalAmount > 0) (bonusPkg.remainingAmount / bonusPkg.totalAmount).toFloat().coerceIn(0f, 1f) else 1f,
                    expiryText = packageExpiryLabel(bonusPkg).replace(" left", ""),
                    color = FundsAmber,
                    icon = Icons.Default.Redeem,
                    modifier = Modifier.weight(1f)
                )
                SummaryTab(
                    label = stringResource(R.string.sms),
                    value = "${smsPkg.remainingAmount.toInt()} SMS",
                    progress = if (smsPkg.totalAmount > 0) (smsPkg.remainingAmount / smsPkg.totalAmount).toFloat().coerceIn(0f, 1f) else 1f,
                    expiryText = packageExpiryLabel(smsPkg).replace(" left", ""),
                    color = SmsTeal,
                    icon = Icons.Default.Email,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun packageExpiryLabel(pkg: BalancePackage): String {
    if (pkg.expiryTimestamp == 0L) return pkg.expiryDate
    return when {
        pkg.isExpired -> stringResource(R.string.expired)
        pkg.daysUntilExpiry == 0 -> stringResource(R.string.expires_today)
        else -> "${pkg.daysUntilExpiry}d"
    }
}

@Composable
private fun SummaryTab(
    label: String,
    value: String,
    progress: Float,
    expiryText: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(85.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.3.dp)
    ) {
        Column {
            // Very slim label header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = color.copy(alpha = 0.85f),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(vertical = 1.dp, horizontal = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress + Icon (Minimum Space)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = 1f,
                        modifier = Modifier.fillMaxSize(),
                        color = color.copy(alpha = 0.08f),
                        strokeWidth = 2.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        color = color,
                        strokeWidth = 2.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (expiryText.isNotEmpty()) {
                        Text(
                            text = expiryText,
                            style = MaterialTheme.typography.labelSmall,
                            color = color.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZeroBalanceState(state: DashboardState, onIntent: (DashboardIntent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        text = stringResource(R.string.no_data_available),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tap_sync_or_scan),
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
                        Text(stringResource(R.string.scan_inbox))
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
                text = stringResource(R.string.sync_to_load),
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
                    Text(stringResource(R.string.dismiss))
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
                Text(stringResource(R.string.ok))
            }
        }
    ) {
        Text(stringResource(R.string.sync_success))
    }
}
