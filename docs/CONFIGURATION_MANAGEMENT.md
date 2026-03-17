# Configuration Management - ETHIOSTAT

## Overview

ETHIOSTAT uses a **hybrid configuration approach** combining BuildConfig defaults with user-configurable settings stored in Room database. This allows for deployment-time configuration while maintaining flexibility for end users.

---

## Configuration Architecture

### Two-Tier System

```
┌─────────────────────────────────────────┐
│         BuildConfig (Tier 1)            │
│      Default Values at Compile Time     │
│  - Set in gradle.properties             │
│  - Read-only at runtime                 │
│  - Used as initial defaults             │
└──────────────────┬──────────────────────┘
                   │
                   │ Initialize
                   ▼
┌─────────────────────────────────────────┐
│      Room Database (Tier 2)             │
│      User Overrides at Runtime          │
│  - Stored in AppConfig entity           │
│  - Modifiable via Settings UI           │
│  - Persists across app restarts         │
└─────────────────────────────────────────┘
```

---

## BuildConfig (Deployment Configuration)

### gradle.properties

```properties
# Telecom Senders
DEFAULT_TELECOM_SENDER=251994
DEFAULT_TELEBIRR_SENDER=*830*

# USSD Codes
DEFAULT_USSD_BALANCE=*805#
DEFAULT_USSD_PACKAGES=*804#
DEFAULT_USSD_DATA_CHECK=*804*1#

# Bank Senders (comma-separated)
DEFAULT_BANK_SENDERS=CBE,ZemenBank,AWASHBANK,DashenBank,BankOfAbyssinia

# App Settings
DEFAULT_LANGUAGE=en
DEFAULT_PARSE_ENGLISH=true
DEFAULT_PARSE_AMHARIC=true
```

### build.gradle.kts

```kotlin
android {
    // ...
    
    defaultConfig {
        // Load from gradle.properties
        val properties = Properties()
        properties.load(project.rootProject.file("gradle.properties").inputStream())
        
        buildConfigField("String", "DEFAULT_TELECOM_SENDER", 
            "\"${properties.getProperty("DEFAULT_TELECOM_SENDER")}\"")
        buildConfigField("String", "DEFAULT_TELEBIRR_SENDER", 
            "\"${properties.getProperty("DEFAULT_TELEBIRR_SENDER")}\"")
        buildConfigField("String", "DEFAULT_USSD_BALANCE", 
            "\"${properties.getProperty("DEFAULT_USSD_BALANCE")}\"")
        buildConfigField("String", "DEFAULT_USSD_PACKAGES", 
            "\"${properties.getProperty("DEFAULT_USSD_PACKAGES")}\"")
        buildConfigField("String", "DEFAULT_BANK_SENDERS", 
            "\"${properties.getProperty("DEFAULT_BANK_SENDERS")}\"")
        buildConfigField("String", "DEFAULT_LANGUAGE", 
            "\"${properties.getProperty("DEFAULT_LANGUAGE")}\"")
    }
    
    buildFeatures {
        buildConfig = true
    }
}
```

### Accessing BuildConfig

```kotlin
// In code
val defaultSender = BuildConfig.DEFAULT_TELECOM_SENDER
val defaultUssd = BuildConfig.DEFAULT_USSD_BALANCE
```

---

## AppConfig Entity (User Configuration)

### Room Entity Definition

```kotlin
@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    
    // Last Read Checkpoint
    val lastReadTimestamp: Long = 0,
    
    // Configurable Senders
    val telecomSenders: String = BuildConfig.DEFAULT_TELECOM_SENDER,
    val telebirrSenders: String = BuildConfig.DEFAULT_TELEBIRR_SENDER,
    val bankSenders: String = BuildConfig.DEFAULT_BANK_SENDERS,
    
    // Configurable USSD Codes
    val ussdBalanceCode: String = BuildConfig.DEFAULT_USSD_BALANCE,
    val ussdPackagesCode: String = BuildConfig.DEFAULT_USSD_PACKAGES,
    val ussdDataCheckCode: String = "",
    
    // Localization Settings
    val appLanguage: String = BuildConfig.DEFAULT_LANGUAGE,
    val parseEnglishSms: Boolean = true,
    val parseAmharicSms: Boolean = true,
    val parseOromiffaSms: Boolean = false,
    val parseTigrinyaSms: Boolean = false,
    
    // Display Settings
    val showExpiredPackages: Boolean = true,
    val expiryWarningDays: Int = 3,
    val currencySymbol: String = "Birr",
    
    // Privacy Settings
    val enableSmsLogging: Boolean = false,
    val logUnparsedSms: Boolean = false
)
```

### DAO Interface

```kotlin
@Dao
interface ConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1")
    fun getConfig(): Flow<AppConfig?>
    
    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun getConfigOnce(): AppConfig?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfig)
    
    @Update
    suspend fun updateConfig(config: AppConfig)
    
    // Specific updates
    @Query("UPDATE app_config SET lastReadTimestamp = :timestamp WHERE id = 1")
    suspend fun updateLastReadTimestamp(timestamp: Long)
    
    @Query("UPDATE app_config SET appLanguage = :language WHERE id = 1")
    suspend fun updateLanguage(language: String)
    
    @Query("UPDATE app_config SET telecomSenders = :senders WHERE id = 1")
    suspend fun updateTelecomSenders(senders: String)
    
    @Query("UPDATE app_config SET bankSenders = :senders WHERE id = 1")
    suspend fun updateBankSenders(senders: String)
}
```

---

## Configuration Initialization

### First Launch Setup

```kotlin
class ConfigInitializer(
    private val configDao: ConfigDao
) {
    suspend fun initializeIfNeeded() {
        val existingConfig = configDao.getConfigOnce()
        
        if (existingConfig == null) {
            val defaultConfig = AppConfig(
                id = 1,
                telecomSenders = BuildConfig.DEFAULT_TELECOM_SENDER,
                telebirrSenders = BuildConfig.DEFAULT_TELEBIRR_SENDER,
                bankSenders = BuildConfig.DEFAULT_BANK_SENDERS,
                ussdBalanceCode = BuildConfig.DEFAULT_USSD_BALANCE,
                ussdPackagesCode = BuildConfig.DEFAULT_USSD_PACKAGES,
                appLanguage = BuildConfig.DEFAULT_LANGUAGE
            )
            
            configDao.insertConfig(defaultConfig)
        }
    }
}

// In Application class
class EthioStatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        lifecycleScope.launch {
            val db = EthioStatDatabase.getDatabase(this@EthioStatApplication)
            val initializer = ConfigInitializer(db.configDao())
            initializer.initializeIfNeeded()
        }
    }
}
```

---

## Configuration Use Cases

### Use Case: Get Telecom Senders

```kotlin
class GetTelecomSendersUseCase(
    private val repository: EthioStatRepository
) {
    operator fun invoke(): Flow<List<String>> {
        return repository.getConfig().map { config ->
            config?.telecomSenders?.split(",")?.map { it.trim() } ?: emptyList()
        }
    }
}
```

### Use Case: Update USSD Code

```kotlin
class UpdateUssdCodeUseCase(
    private val repository: EthioStatRepository
) {
    suspend operator fun invoke(codeType: UssdType, code: String) {
        val config = repository.getConfigOnce() ?: return
        
        val updatedConfig = when (codeType) {
            UssdType.BALANCE -> config.copy(ussdBalanceCode = code)
            UssdType.PACKAGES -> config.copy(ussdPackagesCode = code)
            UssdType.DATA_CHECK -> config.copy(ussdDataCheckCode = code)
        }
        
        repository.updateConfig(updatedConfig)
    }
}

enum class UssdType {
    BALANCE,
    PACKAGES,
    DATA_CHECK
}
```

---

## Settings UI

### Settings Screen State

```kotlin
data class SettingsState(
    val config: AppConfig? = null,
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class SettingsIntent {
    data class LoadConfig : SettingsIntent()
    data class UpdateTelecomSenders(val senders: List<String>) : SettingsIntent()
    data class UpdateBankSenders(val senders: List<String>) : SettingsIntent()
    data class UpdateUssdCode(val type: UssdType, val code: String) : SettingsIntent()
    data class UpdateLanguage(val language: AppLanguage) : SettingsIntent()
    data class ToggleSmsParser(val language: SmsLanguage, val enabled: Boolean) : SettingsIntent()
    object ResetToDefaults : SettingsIntent()
    object SaveConfig : SettingsIntent()
}
```

### Settings ViewModel

```kotlin
class SettingsViewModel(
    private val repository: EthioStatRepository,
    private val updateUssdCodeUseCase: UpdateUssdCodeUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        loadConfig()
    }
    
    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.LoadConfig -> loadConfig()
            is SettingsIntent.UpdateTelecomSenders -> updateTelecomSenders(intent.senders)
            is SettingsIntent.UpdateBankSenders -> updateBankSenders(intent.senders)
            is SettingsIntent.UpdateUssdCode -> updateUssdCode(intent.type, intent.code)
            is SettingsIntent.UpdateLanguage -> updateLanguage(intent.language)
            is SettingsIntent.ToggleSmsParser -> toggleSmsParser(intent.language, intent.enabled)
            is SettingsIntent.ResetToDefaults -> resetToDefaults()
            is SettingsIntent.SaveConfig -> saveConfig()
        }
    }
    
    private fun loadConfig() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            repository.getConfig().collectLatest { config ->
                _state.update {
                    it.copy(
                        config = config,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    private fun resetToDefaults() {
        viewModelScope.launch {
            val defaultConfig = AppConfig(
                id = 1,
                telecomSenders = BuildConfig.DEFAULT_TELECOM_SENDER,
                telebirrSenders = BuildConfig.DEFAULT_TELEBIRR_SENDER,
                bankSenders = BuildConfig.DEFAULT_BANK_SENDERS,
                ussdBalanceCode = BuildConfig.DEFAULT_USSD_BALANCE,
                ussdPackagesCode = BuildConfig.DEFAULT_USSD_PACKAGES
            )
            
            repository.updateConfig(defaultConfig)
            
            _state.update {
                it.copy(
                    config = defaultConfig,
                    saveSuccess = true
                )
            }
        }
    }
}
```

### Settings Screen Composable

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    SettingsContent(
        state = state,
        onIntent = { viewModel.processIntent(it) }
    )
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                actions = {
                    TextButton(onClick = { 
                        onIntent(SettingsIntent.ResetToDefaults) 
                    }) {
                        Text("Reset")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Language Section
            item {
                SectionHeader(title = stringResource(R.string.language))
                LanguagePicker(
                    currentLanguage = AppLanguage.fromCode(state.config?.appLanguage ?: "en"),
                    onLanguageChange = { 
                        onIntent(SettingsIntent.UpdateLanguage(it)) 
                    }
                )
            }
            
            // Sender Configuration
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Sender Configuration")
                SenderConfigCard(
                    title = "Telecom Senders",
                    senders = state.config?.telecomSenders?.split(",") ?: emptyList(),
                    onSendersChange = { 
                        onIntent(SettingsIntent.UpdateTelecomSenders(it)) 
                    }
                )
            }
            
            item {
                SenderConfigCard(
                    title = "Bank Senders",
                    senders = state.config?.bankSenders?.split(",") ?: emptyList(),
                    onSendersChange = { 
                        onIntent(SettingsIntent.UpdateBankSenders(it)) 
                    }
                )
            }
            
            // USSD Codes
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "USSD Codes")
                UssdConfigCard(
                    config = state.config,
                    onCodeChange = { type, code -> 
                        onIntent(SettingsIntent.UpdateUssdCode(type, code)) 
                    }
                )
            }
            
            // SMS Parser Settings
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "SMS Parser")
                ParserConfigCard(
                    config = state.config,
                    onToggle = { lang, enabled -> 
                        onIntent(SettingsIntent.ToggleSmsParser(lang, enabled)) 
                    }
                )
            }
        }
    }
}
```

---

## Sender Configuration Components

### Sender Chip Group

```kotlin
@Composable
fun SenderConfigCard(
    title: String,
    senders: List<String>,
    onSendersChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Add Sender")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                senders.forEach { sender ->
                    InputChip(
                        selected = false,
                        onClick = { },
                        label = { Text(sender) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.clickable {
                                    onSendersChange(senders - sender)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddSenderDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newSender ->
                onSendersChange(senders + newSender)
                showAddDialog = false
            }
        )
    }
}
```

### USSD Code Configuration

```kotlin
@Composable
fun UssdConfigCard(
    config: AppConfig?,
    onCodeChange: (UssdType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            UssdCodeField(
                label = "Balance Check",
                value = config?.ussdBalanceCode ?: "",
                onValueChange = { onCodeChange(UssdType.BALANCE, it) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            UssdCodeField(
                label = "Package Info",
                value = config?.ussdPackagesCode ?: "",
                onValueChange = { onCodeChange(UssdType.PACKAGES, it) }
            )
        }
    }
}

@Composable
fun UssdCodeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(Icons.Default.Phone, contentDescription = null)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("*805#") }
    )
}
```

---

## Environment-Specific Builds

### Build Variants

```kotlin
android {
    flavorDimensions += "version"
    
    productFlavors {
        create("ethiopia") {
            dimension = "version"
            buildConfigField("String", "DEFAULT_TELECOM_SENDER", "\"251994\"")
            buildConfigField("String", "DEFAULT_TELEBIRR_SENDER", "\"*830*\"")
        }
        
        create("demo") {
            dimension = "version"
            buildConfigField("String", "DEFAULT_TELECOM_SENDER", "\"12345\"")
            buildConfigField("String", "DEFAULT_TELEBIRR_SENDER", "\"*TEST*\"")
        }
    }
}
```

### Runtime Environment Detection

```kotlin
object EnvironmentConfig {
    val isProduction: Boolean
        get() = BuildConfig.BUILD_TYPE == "release"
    
    val isDemoMode: Boolean
        get() = BuildConfig.FLAVOR == "demo"
    
    fun getDefaultSenders(): List<String> {
        return BuildConfig.DEFAULT_TELECOM_SENDER.split(",")
    }
}
```

---

## Configuration Validation

### Validator Class

```kotlin
class ConfigValidator {
    fun validateUssdCode(code: String): ValidationResult {
        return when {
            code.isBlank() -> ValidationResult.Error("USSD code cannot be empty")
            !code.startsWith("*") -> ValidationResult.Error("USSD code must start with *")
            !code.endsWith("#") -> ValidationResult.Error("USSD code must end with #")
            code.length < 3 -> ValidationResult.Error("USSD code too short")
            else -> ValidationResult.Success
        }
    }
    
    fun validateSender(sender: String): ValidationResult {
        return when {
            sender.isBlank() -> ValidationResult.Error("Sender cannot be empty")
            sender.length < 2 -> ValidationResult.Error("Sender name too short")
            else -> ValidationResult.Success
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

---

## Migration Strategy

### Version 1 to Version 2

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            ALTER TABLE app_config 
            ADD COLUMN ussdDataCheckCode TEXT NOT NULL DEFAULT ''
        """)
        
        database.execSQL("""
            ALTER TABLE app_config 
            ADD COLUMN enableSmsLogging INTEGER NOT NULL DEFAULT 0
        """)
    }
}
```

---

## Best Practices

### 1. Never Hardcode Configuration
```kotlin
// ✅ Good
val sender = BuildConfig.DEFAULT_TELECOM_SENDER

// ❌ Bad
val sender = "251994"
```

### 2. Provide Sensible Defaults
```kotlin
// ✅ Good
val config = AppConfig(
    telecomSenders = BuildConfig.DEFAULT_TELECOM_SENDER,
    expiryWarningDays = 3
)

// ❌ Bad
val config = AppConfig()  // Empty values
```

### 3. Validate User Input
```kotlin
// ✅ Good
fun updateUssdCode(code: String) {
    val result = validator.validateUssdCode(code)
    if (result is ValidationResult.Success) {
        repository.updateUssdCode(code)
    }
}

// ❌ Bad
fun updateUssdCode(code: String) {
    repository.updateUssdCode(code)  // No validation
}
```

### 4. Use Flow for Reactive Configuration
```kotlin
// ✅ Good
repository.getConfig().collectLatest { config ->
    // UI updates automatically
}

// ❌ Bad
val config = repository.getConfigOnce()  // Won't update
```

---

## Summary

### Configuration Hierarchy

1. **BuildConfig** (Compile-time defaults)
2. **Room Database** (Runtime overrides)
3. **User Settings UI** (User modifications)

### Key Benefits

- ✅ Flexible deployment options
- ✅ User customization without app rebuild
- ✅ Persistent settings across app restarts
- ✅ Environment-specific builds
- ✅ Easy rollback to defaults
- ✅ Validation and error handling
