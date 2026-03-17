# MVI Architecture - ETHIOSTAT

## Why MVI (Model-View-Intent)?

### Architecture Decision

We chose **MVI (Model-View-Intent)** over traditional MVVM for ETHIOSTAT because:

1. **Unidirectional Data Flow**: Simplifies state management for complex SMS parsing scenarios
2. **State Immutability**: Makes debugging easier when tracking balance changes
3. **Predictable State**: Single source of truth for UI state
4. **Testability**: Clear separation between business logic and UI
5. **Time-Travel Debugging**: Can replay state changes for testing parsers

### MVI vs MVVM Comparison

| Feature | MVI | MVVM |
|---------|-----|------|
| Data Flow | Unidirectional | Bidirectional |
| State | Single Immutable State | Multiple LiveData/StateFlow |
| Debugging | Easier (state history) | Harder (multiple sources) |
| Complexity | Higher learning curve | Familiar pattern |
| Best For | Complex state management | Simple CRUD apps |

**Decision**: MVI is ideal for ETHIOSTAT's complex SMS parsing and multi-package tracking.

---

## MVI Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         USER                                 │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   │ User Action (Click, Swipe)
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                         VIEW                                 │
│              (Jetpack Compose UI)                            │
│  - DashboardScreen                                           │
│  - SettingsScreen                                            │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   │ Emits Intent
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                       INTENT                                 │
│  sealed class DashboardIntent {                              │
│    object LoadData                                           │
│    data class SyncUssd(val code: String)                     │
│    data class FilterTransactions(val period: TimePeriod)     │
│  }                                                           │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   │ Processes Intent
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                      VIEWMODEL                               │
│  - Receives Intent                                           │
│  - Calls Use Cases                                           │
│  - Updates State                                             │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   │ Executes Business Logic
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                     USE CASES                                │
│  - ParseSmsUseCase                                           │
│  - SyncBalanceUseCase                                        │
│  - GetFinancialSummaryUseCase                                │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   │ Data Operations
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                     REPOSITORY                               │
│  - Single Source of Truth                                    │
│  - Coordinates Data Sources                                  │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   │
          ┌────────┴────────┐
          ▼                 ▼
┌──────────────────┐  ┌──────────────────┐
│   ROOM DATABASE  │  │   SMS PARSER     │
│   (Local Store)  │  │  (Regex Engine)  │
└──────────────────┘  └──────────────────┘
          │                 │
          │ Emits New State │
          └────────┬────────┘
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                        STATE                                 │
│  data class DashboardState(                                  │
│    val balances: List<BalancePackage>,                       │
│    val transactions: List<Transaction>,                      │
│    val isLoading: Boolean,                                   │
│    val error: String?                                        │
│  )                                                           │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   │ StateFlow emission
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                         VIEW                                 │
│              (Observes State & Re-renders)                   │
└─────────────────────────────────────────────────────────────┘
```

---

## Core Components

### 1. Intent (User Actions)

Represents all possible user interactions:

```kotlin
sealed class DashboardIntent {
    object LoadData : DashboardIntent()
    object RefreshBalances : DashboardIntent()
    data class SyncUssd(val ussdCode: String) : DashboardIntent()
    data class FilterTransactions(val period: TimePeriod) : DashboardIntent()
    data class ChangeLanguage(val language: AppLanguage) : DashboardIntent()
}

sealed class SettingsIntent {
    data class UpdateSenders(val senders: List<String>) : SettingsIntent()
    data class UpdateUssdCode(val codeType: UssdType, val code: String) : SettingsIntent()
    data class ToggleLanguageParser(val language: SmsLanguage, val enabled: Boolean) : SettingsIntent()
}
```

### 2. State (UI State)

Immutable data representing the entire UI state:

```kotlin
data class DashboardState(
    val balances: List<BalancePackage> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val financialSummary: FinancialSummary = FinancialSummary(),
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedPeriod: TimePeriod = TimePeriod.WEEKLY
)

data class FinancialSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0
)
```

### 3. ViewModel (State Manager)

Processes intents and updates state:

```kotlin
class DashboardViewModel(
    private val parseSmsUseCase: ParseSmsUseCase,
    private val syncBalanceUseCase: SyncBalanceUseCase,
    private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
    private val repository: EthioStatRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    
    fun processIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadData -> loadData()
            is DashboardIntent.RefreshBalances -> refreshBalances()
            is DashboardIntent.SyncUssd -> syncViaUssd(intent.ussdCode)
            is DashboardIntent.FilterTransactions -> filterTransactions(intent.period)
            is DashboardIntent.ChangeLanguage -> changeLanguage(intent.language)
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val balances = repository.getBalances().first()
                val transactions = repository.getTransactions().first()
                val summary = getFinancialSummaryUseCase(transactions)
                
                _state.update {
                    it.copy(
                        balances = balances,
                        transactions = transactions,
                        financialSummary = summary,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
    
    private fun syncViaUssd(ussdCode: String) {
        viewModelScope.launch {
            syncBalanceUseCase(ussdCode)
        }
    }
}
```

### 4. View (Compose UI)

Observes state and emits intents:

```kotlin
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    DashboardContent(
        state = state,
        onIntent = { intent -> viewModel.processIntent(intent) }
    )
}

@Composable
private fun DashboardContent(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ETHIOSTAT") },
                actions = {
                    IconButton(onClick = { 
                        onIntent(DashboardIntent.SyncUssd("*805#")) 
                    }) {
                        Icon(Icons.Default.Refresh, "Sync")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingIndicator()
            state.error != null -> ErrorMessage(state.error)
            else -> {
                LazyColumn(Modifier.padding(padding)) {
                    items(state.balances) { balance ->
                        BalanceCard(balance)
                    }
                    item {
                        FinancialSummaryCard(state.financialSummary)
                    }
                    items(state.transactions) { transaction ->
                        TransactionItem(transaction)
                    }
                }
            }
        }
    }
}
```

---

## Layer Responsibilities

### Domain Layer
- **Models**: Pure Kotlin data classes (no Android dependencies)
- **Use Cases**: Single-responsibility business logic
- **Repository Interface**: Abstract data operations

### Data Layer
- **Repository Implementation**: Coordinates Room + SMS Parser
- **Room Database**: Local persistence
- **SMS Parser**: Regex-based extraction
- **Type Converters**: Serialize complex types

### Presentation Layer
- **ViewModels**: Process Intents, emit States
- **Compose Screens**: Observe State, send Intents
- **UI Components**: Reusable composables

---

## State Management Best Practices

### 1. Single State Object
```kotlin
// ✅ Good: Single immutable state
data class DashboardState(
    val balances: List<BalancePackage>,
    val transactions: List<Transaction>,
    val isLoading: Boolean
)

// ❌ Bad: Multiple mutable states
var balances: MutableList<BalancePackage>
var transactions: MutableList<Transaction>
var isLoading: Boolean
```

### 2. Sealed Classes for Intents
```kotlin
// ✅ Good: Exhaustive when expressions
sealed class DashboardIntent {
    object LoadData : DashboardIntent()
    data class SyncUssd(val code: String) : DashboardIntent()
}

// ❌ Bad: String-based actions
fun processAction(action: String, data: Any?)
```

### 3. State Updates via Copy
```kotlin
// ✅ Good: Immutable updates
_state.update { it.copy(isLoading = true) }

// ❌ Bad: Mutable updates
_state.value.isLoading = true
```

---

## Testing Strategy

### ViewModel Tests
```kotlin
@Test
fun `LoadData intent updates state with balances`() = runTest {
    val viewModel = DashboardViewModel(...)
    val states = mutableListOf<DashboardState>()
    
    viewModel.state.onEach { states.add(it) }.launchIn(this)
    
    viewModel.processIntent(DashboardIntent.LoadData)
    
    assertEquals(true, states[0].isLoading)
    assertEquals(false, states[1].isLoading)
    assertTrue(states[1].balances.isNotEmpty())
}
```

### Use Case Tests
```kotlin
@Test
fun `ParseSmsUseCase extracts internet balance correctly`() {
    val sms = "Your balance is 4728.760 MB"
    val result = parseSmsUseCase(sms)
    
    assertEquals(4728.760, result.internetMB, 0.001)
}
```

---

## Benefits for ETHIOSTAT

1. **SMS Parsing Reliability**: Immutable state prevents parsing errors from corrupting UI
2. **Multi-Package Handling**: Single state object handles 10+ packages easily
3. **Debugging**: Can log entire state history for troubleshooting
4. **Language Switching**: State update automatically re-renders UI
5. **Transaction Tracking**: Clear audit trail of financial state changes
6. **Testability**: Each layer independently testable

---

## Migration Path

If starting with MVVM, migrate to MVI by:

1. Combine multiple LiveData into single StateFlow
2. Replace direct ViewModel calls with Intent emissions
3. Extract business logic into Use Cases
4. Make State data class immutable
5. Update UI to observe single State

---

## References

- [Google MVI Guide](https://developer.android.com/topic/architecture)
- [Kotlin Flow Best Practices](https://developer.android.com/kotlin/flow)
- [Jetpack Compose State Management](https://developer.android.com/jetpack/compose/state)
