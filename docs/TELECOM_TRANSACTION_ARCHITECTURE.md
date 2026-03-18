# EthioStat: Telecom Service & Transaction Architecture

## Overview

This document describes the complete architecture for telecom service data management, transaction tracking, USSD automation, and related features in the EthioStat application.

---

## 1. Telecom Service Data Flow

### 1.1 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                    APP START / REFRESH BUTTON                       │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Step 1: Read Last SMS from 251994                                 │
│  - Check AppConfig.lastReadTimestamp                                │
│  - Query SMS ContentProvider (sender=251994)                        │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Step 2: Trigger USSD *805# Call                                    │
│  - SyncBalanceUseCase.invoke("*805#")                              │
│  - Intent.ACTION_CALL with tel:*805%23                             │
│  - UssdAccessibilityService listens for dialog                      │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Step 3: UssdAccessibilityService Auto-Dismisses Dialog            │
│  - Detects USSD dialog (com.android.phone package)                 │
│  - Waits 2 seconds                                                  │
│  - Clicks "Cancel" or "OK" button                                   │
│  - Falls back to BACK button if needed                              │
└────────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Step 4: SMS Response Arrives                                       │
│  - SmsReceiver captures SMS_RECEIVED_ACTION                         │
│  - Filters sender: contains "251994"                                │
│  - Extracts: body, timestamp                                        │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Step 5: SMS Parsing                                                │
│  - EnglishSmsParser.parseTelecomUssdResponse()                     │
│  - Regex: telecomInternetPattern, telecomVoicePattern              │
│  - Extract: MB/GB, minutes, expiry date/time                        │
│  - Create BalancePackage objects                                    │
│  - NO transaction created (Telecom excluded)                        │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Step 6: Data Storage                                               │
│  - Delete old: balanceDao.deleteByTypeAndSource()                  │
│  - Insert new: balanceDao.insert(BalanceEntity)                    │
│  - Update: configDao.updateLastReadTimestamp()                     │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Step 7: UI Update                                                  │
│  - DashboardViewModel observes Flow<List<BalancePackage>>          │
│  - DashboardState updates                                           │
│  - TelecomServiceSection recomposes                                 │
│  - Displays: amounts, progress bars, expiry dates                   │
│  - Zero-value fallbacks if no data                                  │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Key Components

#### EnglishSmsParser
- **Location**: `app/src/main/java/com/ethiostat/app/data/parser/EnglishSmsParser.kt`
- **Responsibility**: Parse telecom USSD SMS responses
- **Regex Patterns**:
  - `telecomInternetPattern`: Matches internet balance with optional expiry
  - `telecomVoicePattern`: Matches voice balance (minutes + seconds) with optional expiry
- **Critical Rule**: Sender "251994" NEVER creates transactions, only BalancePackages

#### BalancePackage
- **Location**: `app/src/main/java/com/ethiostat/app/domain/model/BalancePackage.kt`
- **Zero-Value Factories**:
  - `createZeroInternet()`: Returns 0 GB package with "No data" expiry
  - `createZeroVoice()`: Returns 0 min package
  - `createZeroBonus()`: Returns 0 Birr package
  - `createZeroPromotion()`: Returns 0 Coins package

#### UssdAccessibilityService
- **Location**: `app/src/main/java/com/ethiostat/app/service/UssdAccessibilityService.kt`
- **Responsibility**: Auto-dismiss USSD dialogs
- **Mechanism**:
  1. Listens for `TYPE_WINDOW_STATE_CHANGED` events
  2. Detects phone/telecom package dialogs
  3. Waits 2 seconds for SMS to arrive
  4. Finds and clicks "Cancel", "OK", or "Dismiss" button
  5. Falls back to BACK button if needed

---

## 2. Transaction Management Architecture

### 2.1 Transaction Source Exclusion

**Rule**: Telecom (sender 251994) is NOT a transaction source.

**Implementation**:
```kotlin
// EnglishSmsParser.parse()
val transaction = if (sender.contains("251994", ignoreCase = true) || 
                      sender.contains("ethio telecom", ignoreCase = true)) {
    null  // Telecom only creates packages, never transactions
} else {
    parseTransaction(smsBody, sender)
}
```

### 2.2 Transaction Source Configuration

**Maximum Sources**: 3 (configurable via environment variable)

**Default Mapping**:
- TeleBirr → Phone: "830"
- CBE → Phone: "CBE"
- Awash → Phone: "AWASH"

**User Flow**:
```
Settings → Transaction Sources
    ↓
[Add New Source]
    ↓
Enter: Name (e.g., "CBE"), Type (BANK_CBE), Phone ("CBE")
    ↓
App scans past week's SMS from "CBE" sender
    ↓
Dashboard shows CBE card with Income/Expense
    ↓
If no data: shows "0.00 Birr" for both
```

### 2.3 Time Period Filtering

**Implementation**: `GetFinancialSummaryUseCase`

**Time Ranges**:
- **DAILY**: Last 24 hours (`currentTime - 1 day`)
- **WEEKLY**: Last 7 days (`currentTime - 7 days`)
- **MONTHLY**: Last 30 days (`currentTime - 30 days`)
- **ALL_TIME**: All transactions (`startTime = 0L`)

**Filter Logic**:
```kotlin
val startTime = when (period) {
    TimePeriod.DAILY -> currentTime - TimeUnit.DAYS.toMillis(1)
    TimePeriod.WEEKLY -> currentTime - TimeUnit.DAYS.toMillis(7)
    TimePeriod.MONTHLY -> currentTime - TimeUnit.DAYS.toMillis(30)
    TimePeriod.ALL_TIME -> 0L
}
return transactions.filter { it.timestamp >= startTime }
```

---

## 3. Data Models

### 3.1 BalancePackage
```kotlin
data class BalancePackage(
    val packageType: PackageType,      // INTERNET, VOICE, BONUS_FUND
    val packageName: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val unit: String,                  // "GB", "MB", "min", "Birr", "Coins"
    val source: String,                // "Ethio Telecom"
    val validityDays: Int,
    val expiryDate: String,            // "2026-04-06" or "No data"
    val expiryTimestamp: Long,         // 0L if no data
    val language: String               // "en", "am", "om"
)
```

### 3.2 Transaction
```kotlin
data class Transaction(
    val amount: Double,
    val type: TransactionType,         // INCOME, EXPENSE
    val category: String,
    val source: String,                // "TeleBirr", "CBE", etc.
    val description: String,
    val timestamp: Long,
    val accountSource: AccountSourceType,
    val sourcePhoneNumber: String,
    val isClassified: Boolean
)
```

### 3.3 AccountSource
```kotlin
data class AccountSource(
    val name: String,                  // "TeleBirr", "CBE", "Awash"
    val type: AccountSourceType,       // TELEBIRR, BANK_CBE, BANK_AWASH
    val phoneNumber: String,           // "830", "CBE", "AWASH"
    val displayName: String,
    val isEnabled: Boolean
)

enum class AccountSourceType {
    TELEBIRR,
    BANK_CBE,
    BANK_AWASH,
    BANK_OTHER,
    TELECOM  // NOT used for transactions
}
```

---

## 4. UI Components

### 4.1 TelecomServiceSection
**Location**: `DashboardScreen.kt`

**Layout**: 2×2 grid
```
┌──────────────────┐ ┌──────────────────┐
│ Internet Balance │ │  Voice Balance   │
│ 9.3 GB / 16.8 GB │ │  190 min / 300   │
│ ████████░░░░     │ │  ████████░░      │
│ 21d left         │ │  23d left        │
└──────────────────┘ └──────────────────┘
┌──────────────────┐ ┌──────────────────┐
│ Bonus Funds      │ │  Promotion       │
│ 4 Coins          │ │  4 Coins         │
│ ████████████     │ │  ████████████    │
│ 29d left         │ │  29d left        │
└──────────────────┘ └──────────────────┘
```

**Zero-Value Fallback**:
- Shows "0 GB / 0 GB" for internet
- Shows "0 min / 0 min" for voice
- Shows "No data" for expiry when `expiryTimestamp == 0L`

### 4.2 FinancialSummaryCard
**Features**:
- Period filter chips: Daily, Weekly, Monthly
- Net balance visibility toggle (eye icon)
- Source filter dropdown

### 4.3 TransactionSourcesSection
**Features**:
- List of enabled sources (max 3)
- Add/Edit/Delete buttons
- Auto-scan past week on add

---

## 5. Permissions & Setup

### 5.1 Required Permissions
```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

### 5.2 Accessibility Service Setup
**User Action Required**:
1. Settings → Accessibility → EthioStat
2. Enable "Auto-dismiss USSD dialogs"

**Why Needed**: Android doesn't allow apps to programmatically dismiss system dialogs without accessibility permission.

---

## 6. Error Handling

### 6.1 Telecom Data Errors
- **No SMS response**: Show zero-value fallbacks
- **Regex mismatch**: Log error, use fallback parser
- **Invalid expiry date**: Set `expiryTimestamp = 0L`, show "No data"

### 6.2 USSD Errors
- **CALL_PHONE permission denied**: Show error, request permission
- **Accessibility service disabled**: Show prompt to enable
- **Dialog not dismissed**: Fallback to BACK button

### 6.3 Transaction Errors
- **Duplicate detection**: Check `source + timestamp` before insert
- **Invalid amount**: Skip transaction, log error
- **Unknown source**: Categorize as "Other"

---

## 7. Testing Checklist

### 7.1 Telecom Service
- [ ] App start triggers *805# USSD
- [ ] Accessibility service dismisses dialog
- [ ] SMS response parsed correctly
- [ ] Internet balance shows MB/GB with expiry
- [ ] Voice balance shows min with expiry
- [ ] Zero values show "0 GB", "0 min", "No data"

### 7.2 Transactions
- [ ] Telecom (251994) does NOT create transactions
- [ ] TeleBirr (830) creates transactions
- [ ] Daily filter shows last 24 hours
- [ ] Weekly filter shows last 7 days
- [ ] Monthly filter shows last 30 days
- [ ] No duplicate transactions on re-sync

### 7.3 Transaction Sources
- [ ] Can add up to 3 sources
- [ ] Auto-scans past week on add
- [ ] Dashboard shows source cards
- [ ] Shows "0.00 Birr" when no data
- [ ] Can edit/delete sources

---

## 8. Future Enhancements

1. **Configurable USSD codes**: Allow user to set custom codes per carrier
2. **Multiple telecom providers**: Support Safaricom, MTN, etc.
3. **Transaction categories**: Auto-categorize by merchant
4. **Export data**: CSV/PDF export of transactions
5. **Notifications**: Alert on low balance or large transactions
6. **Widgets**: Home screen widget for quick balance view

---

## 9. File Structure

```
app/src/main/java/com/ethiostat/app/
├── data/
│   ├── parser/
│   │   ├── EnglishSmsParser.kt          ✓ Enhanced regex, exclude telecom
│   │   ├── AmharicSmsParser.kt
│   │   └── OromoSmsParser.kt
│   └── repository/
│       └── EthioStatRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── BalancePackage.kt            ✓ Zero-value factories
│   │   ├── Transaction.kt
│   │   └── AccountSource.kt
│   └── usecase/
│       ├── SyncBalanceUseCase.kt
│       └── GetFinancialSummaryUseCase.kt ✓ Time filtering
├── service/
│   ├── UssdAccessibilityService.kt      ✓ NEW - Auto-dismiss USSD
│   └── SmsMonitoringService.kt
├── receiver/
│   └── SmsReceiver.kt
└── ui/
    ├── dashboard/
    │   ├── DashboardScreen.kt           ✓ Zero fallbacks
    │   ├── DashboardViewModel.kt
    │   └── DashboardState.kt
    └── settings/
        ├── SettingsScreen.kt
        └── TransactionSourceScreen.kt   ⏳ TODO

app/src/main/res/
├── xml/
│   └── accessibility_service_config.xml ✓ NEW
└── values/
    └── strings.xml                      ✓ Accessibility description

AndroidManifest.xml                      ✓ Service declaration
```

---

## 10. Implementation Status

### Completed ✓
1. Zero-value fallback factory methods
2. Enhanced regex patterns for USSD
3. Exclude Telecom from transactions
4. UssdAccessibilityService implementation
5. Accessibility service configuration
6. Manifest updates

### In Progress ⏳
1. Transaction Source configuration UI
2. Oromo language switching fix
3. Net balance visibility toggle
4. Collapsible sections

### Pending ⏺
1. USSD auto-trigger on app start
2. Transaction source auto-scan
3. Settings UI for transaction sources

---

**Last Updated**: 2026-03-18  
**Version**: 1.0  
**Author**: EthioStat Development Team
