# SMS Parsing Rules - ETHIOSTAT

## Overview

This document defines the regex patterns and parsing logic for extracting balance information from Ethio telecom and telebirr SMS messages in both English and Amharic.

---

## Sender Identification

### Configurable Senders

| Source | Default Sender | Type | Configurable |
|--------|---------------|------|--------------|
| Ethio telecom | 251994 | Phone Number | Yes |
| telebirr | *830* | Short Code | Yes |
| CBE Bank | CBE | Name | Yes |
| ZemenBank | ZemenBank | Name | Yes |
| AWASH Bank | AWASHBANK | Name | Yes |

### Sender Matching Logic

```kotlin
class SenderMatcher(private val config: AppConfig) {
    fun matchesTelecom(sender: String): Boolean {
        val telecomSenders = config.telecomSenders.split(",").map { it.trim() }
        return telecomSenders.any { sender.contains(it, ignoreCase = true) }
    }
    
    fun matchesTelebirr(sender: String): Boolean {
        val telebirrSenders = config.telebirrSenders.split(",").map { it.trim() }
        return telebirrSenders.any { sender.contains(it, ignoreCase = true) }
    }
    
    fun matchesBank(sender: String): Boolean {
        val bankSenders = config.bankSenders.split(",").map { it.trim() }
        return bankSenders.any { sender.equals(it, ignoreCase = true) }
    }
}
```

---

## English SMS Patterns

### Pattern 1: Multi-Package Complex Message

**Example SMS:**
```
Dear Customer, your remaining amount from Monthly Internet Package 12GB from 
telebirr to be expired after 30 days is 4728.760 MB with expiry date on 
2026-04-06 at 17:38:02; from Monthly voice 150 Min from telebirr to be 
expired after 30 days and 76 Min night package bonus valid for 30 days is 
114 minute and 34 second with expiry date on 2026-04-10 at 11:08:07
```

**Regex Patterns:**

```kotlin
object EnglishPatterns {
    // Main multi-package pattern
    val multiPackagePattern = """
        from\s+
        ([^;]+?)                                    # Package description
        \s+is\s+
        ([\d,]+\.?\d*)\s*(MB|GB|minute|second)     # Amount + Unit
        \s+with\s+expiry\s+date\s+on\s+
        (\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2})
    """.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.COMMENTS))
    
    // Package name extraction
    val packageNamePattern = """
        (Monthly\s+Internet\s+Package\s+[\d.]+\s*(?:GB|MB)|
         Monthly\s+voice\s+\d+\s*Min|
         Weekly\s+Youth\s+[\d.]+GB|
         [\d.]+\s*Min\s+night\s+package\s+bonus)
        \s+from\s+(telebirr|tele\s+birr)
    """.toRegex(RegexOption.IGNORE_CASE)
    
    // Validity period
    val validityPattern = """
        to\s+be\s+expired\s+after\s+(\d+)\s+days?
    """.toRegex(RegexOption.IGNORE_CASE)
    
    // Voice balance (minutes and seconds)
    val voiceBalancePattern = """
        (\d+)\s+minute\s+and\s+(\d+)\s+second
    """.toRegex(RegexOption.IGNORE_CASE)
    
    // Internet balance (MB or GB)
    val internetBalancePattern = """
        ([\d,]+\.?\d*)\s*(MB|GB)
    """.toRegex(RegexOption.IGNORE_CASE)
    
    // Expiry date with time
    val expiryDatePattern = """
        (\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2})
    """.toRegex()
    
    // Expiry date only
    val expiryDateOnlyPattern = """
        (\d{4}-\d{2}-\d{2})
    """.toRegex()
}
```

**Extraction Logic:**

```kotlin
class EnglishSmsParser : SmsParser {
    
    fun parseMultiPackage(smsBody: String): List<BalancePackage> {
        val packages = mutableListOf<BalancePackage>()
        
        // Split by semicolon to get individual packages
        val segments = smsBody.split(";")
        
        for (segment in segments) {
            val match = EnglishPatterns.multiPackagePattern.find(segment)
            if (match != null) {
                val packageDesc = match.groupValues[1]
                val amount = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
                val unit = match.groupValues[3]
                val expiryDate = match.groupValues[4]
                val expiryTime = match.groupValues[5]
                
                // Determine package type
                val type = when {
                    unit.equals("MB", ignoreCase = true) || 
                    unit.equals("GB", ignoreCase = true) -> PackageType.INTERNET
                    unit.equals("minute", ignoreCase = true) -> PackageType.VOICE
                    else -> PackageType.UNKNOWN
                }
                
                // Extract package name
                val packageName = EnglishPatterns.packageNamePattern
                    .find(packageDesc)?.groupValues?.get(1) ?: packageDesc.take(50)
                
                // Extract validity days
                val validityDays = EnglishPatterns.validityPattern
                    .find(packageDesc)?.groupValues?.get(1)?.toIntOrNull() ?: 30
                
                // Convert GB to MB for consistency
                val amountInMB = if (unit.equals("GB", ignoreCase = true)) {
                    amount * 1024
                } else amount
                
                packages.add(
                    BalancePackage(
                        packageType = type,
                        packageName = packageName.trim(),
                        remainingAmount = amountInMB,
                        unit = "MB",
                        expiryDate = "$expiryDate at $expiryTime",
                        validityDays = validityDays,
                        language = "en"
                    )
                )
            }
        }
        
        return packages
    }
    
    fun parseVoiceBalance(segment: String): VoiceBalance? {
        val match = EnglishPatterns.voiceBalancePattern.find(segment)
        return match?.let {
            VoiceBalance(
                minutes = it.groupValues[1].toIntOrNull() ?: 0,
                seconds = it.groupValues[2].toIntOrNull() ?: 0
            )
        }
    }
}
```

### Pattern 2: Weekly Package with Night Bonus

**Example SMS:**
```
Your Weekly Youth 3.6GB + 3.6GB night bonus from tele birr with 360MB free 
data for Spotify to be expired after 7 days will be expired on 2026-03-10 
at 20:30:55
```

**Regex Pattern:**

```kotlin
val weeklyPackagePattern = """
    (Weekly\s+[\w\s]+[\d.]+GB)                  # Package name
    \s*\+\s*
    ([\d.]+GB)\s+night\s+bonus                  # Night bonus
    \s+from\s+tele\s+birr
    \s+with\s+([\d.]+\s*(?:MB|GB))\s+free\s+data\s+for\s+(\w+)
    \s+to\s+be\s+expired\s+after\s+(\d+)\s+days?
    \s+will\s+be\s+expired\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2})
""".toRegex(RegexOption.IGNORE_CASE)

fun parseWeeklyPackage(smsBody: String): WeeklyPackageData? {
    val match = weeklyPackagePattern.find(smsBody) ?: return null
    
    return WeeklyPackageData(
        packageName = match.groupValues[1],
        dayData = parseDataAmount(match.groupValues[1]),
        nightBonus = parseDataAmount(match.groupValues[2]),
        freeData = parseDataAmount(match.groupValues[3]),
        freeDataApp = match.groupValues[4],
        validityDays = match.groupValues[5].toIntOrNull() ?: 7,
        expiryDate = match.groupValues[6],
        expiryTime = match.groupValues[7]
    )
}

private fun parseDataAmount(text: String): Double {
    val pattern = """([\d.]+)\s*(MB|GB)""".toRegex(RegexOption.IGNORE_CASE)
    val match = pattern.find(text) ?: return 0.0
    
    val amount = match.groupValues[1].toDoubleOrNull() ?: 0.0
    val unit = match.groupValues[2]
    
    return if (unit.equals("GB", ignoreCase = true)) {
        amount * 1024  // Convert to MB
    } else amount
}
```

### Pattern 3: Bonus Funds

**Example SMS:**
```
Your Bonus Fund balance is 15.50 Birr and will expire on 2026-05-01
```

**Regex Pattern:**

```kotlin
val bonusFundsPattern = """
    Bonus\s+Fund.*?
    ([\d,]+\.?\d*)\s*Birr
""".toRegex(RegexOption.IGNORE_CASE)

fun parseBonusFunds(smsBody: String): Double? {
    val match = bonusFundsPattern.find(smsBody)
    return match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
}
```

---

## Amharic SMS Patterns

### Pattern 1: Telebirr Transaction Notification

**Example SMS:**
```
በቴሌብር ስላደረጉት የገንዘብ ዝውውር እናመሰግናለን። ወደ *830*1# በመደወል በቴሌፕለይ 
ጨዋታዎች እለታዊ 25,000 ብር፣ ሳምንታዊ 100,000 ብር፣ ወርሃዊ 250,000 ብር እና 
ዘመናዊ የኤሌክትሪክ መኪና እንዲሁም ሌሎች አጓጊ ሽልማቶችን ያሸንፉ!
```

**Translation:**
"Thank you for the money transfer you made through telebirr. Call *830*1# to 
win daily 25,000 Birr, weekly 100,000 Birr, monthly 250,000 Birr and a modern 
electric car as well as other exciting prizes on TelePlay games!"

**Regex Patterns:**

```kotlin
object AmharicPatterns {
    // Transaction confirmation
    val transactionPattern = """
        በቴሌብር\s+ስላደረጉት\s+የገንዘብ\s+ዝውውር
    """.toRegex()
    
    // USSD code extraction
    val ussdCodePattern = """
        ወደ\s+(\*\d+\*\d+#)\s+በመደወል
    """.toRegex()
    
    // Prize amounts
    val prizePattern = """
        ([\d,]+)\s+ብር
    """.toRegex()
    
    // Expiry date (Amharic format: በ24/03/2026 ነው)
    val expiryDatePattern = """
        በ(\d{2})/(\d{2})/(\d{4})\s*ነው
    """.toRegex()
    
    // TeleCoin rewards
    val teleCoinPattern = """
        የቴሌብር\s+ግብይቶ\s+(\d+)\s+ነፃ\s+የቴሌኮይን\s+አስገኝቶሎታል
    """.toRegex()
    
    // Service expiry
    val serviceExpiryPattern = """
        የአገልግሎት\s+ማብቅያ\s+ጊዜ\s+በ(\d{2}/\d{2}/\d{4})\s+ነው
    """.toRegex()
}
```

**Extraction Logic:**

```kotlin
class AmharicSmsParser : SmsParser {
    
    fun parseTransaction(smsBody: String): TransactionData? {
        if (!AmharicPatterns.transactionPattern.containsMatchIn(smsBody)) {
            return null
        }
        
        val ussdCode = AmharicPatterns.ussdCodePattern
            .find(smsBody)?.groupValues?.get(1)
        
        val prizes = AmharicPatterns.prizePattern
            .findAll(smsBody)
            .map { it.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0 }
            .toList()
        
        return TransactionData(
            type = TransactionType.INCOME,
            source = "telebirr",
            ussdCode = ussdCode,
            prizeAmounts = prizes,
            language = "am"
        )
    }
    
    fun parseTeleCoinReward(smsBody: String): TeleCoinData? {
        val teleCoinMatch = AmharicPatterns.teleCoinPattern.find(smsBody)
        val expiryMatch = AmharicPatterns.serviceExpiryPattern.find(smsBody)
        
        if (teleCoinMatch == null) return null
        
        val coins = teleCoinMatch.groupValues[1].toIntOrNull() ?: 0
        val expiryDate = expiryMatch?.groupValues?.get(1)?.let { 
            convertAmharicDate(it) 
        }
        
        return TeleCoinData(
            coinCount = coins,
            expiryDate = expiryDate
        )
    }
    
    private fun convertAmharicDate(amharicDate: String): String {
        // Convert DD/MM/YYYY to YYYY-MM-DD
        val parts = amharicDate.split("/")
        if (parts.size != 3) return amharicDate
        
        return "${parts[2]}-${parts[1]}-${parts[0]}"
    }
}
```

### Pattern 2: TeleCoin Reward Notification

**Example SMS:**
```
የቴሌብር ግብይቶ 4 ነፃ የቴሌኮይን አስገኝቶሎታል። ይህን https://teleplay.et/am 
ማስፈንጠሪያ በመንካት ሽልማቶን ይመልከቱ። የቴሌኮይኖት የአገልግሎት ማብቅያ ጊዜ 
በ24/03/2026 ነው።
```

**Translation:**
"Your telebirr transactions have earned you 4 free TeleCoins. View prizes by 
clicking this link https://teleplay.et/am. The TeleCoin service expiry date 
is 24/03/2026."

**Extracted Data:**
- TeleCoin Count: 4
- Expiry Date: 2026-03-24 (converted from Amharic format)
- Link: https://teleplay.et/am

---

## Financial Transaction Patterns

### Income Patterns (English)

```kotlin
object IncomePatterns {
    val keywords = listOf(
        "received",
        "credited",
        "deposited",
        "transfer received",
        "payment received"
    )
    
    val incomePattern = """
        (received|credited|deposited).*?
        ([\d,]+\.?\d*)\s*(Birr|ETB)
    """.toRegex(RegexOption.IGNORE_CASE)
    
    fun isIncomeTransaction(smsBody: String): Boolean {
        return keywords.any { smsBody.contains(it, ignoreCase = true) }
    }
}
```

### Expense Patterns (English)

```kotlin
object ExpensePatterns {
    val keywords = listOf(
        "Monthly Internet Package",
        "Voice Min",
        "from telebirr",
        "purchased",
        "debited",
        "payment for"
    )
    
    val expensePattern = """
        (purchased|debited|payment\s+for).*?
        ([\d,]+\.?\d*)\s*(Birr|ETB)
    """.toRegex(RegexOption.IGNORE_CASE)
    
    fun isExpenseTransaction(smsBody: String): Boolean {
        return keywords.any { smsBody.contains(it, ignoreCase = true) }
    }
}
```

### Financial SMS Examples

#### Bank Deposit (Income)
```
Dear Customer, your account has been credited with 5,000.00 Birr from 
sender Abebe Kebede on 17/03/2026 at 14:30:15. Your new balance is 
12,500.00 Birr. - CBE
```

#### telebirr Purchase (Expense)
```
You have purchased Monthly Internet Package 12GB from telebirr for 
550.00 Birr on 17/03/2026. Thank you for using ethio telecom services.
```

---

## Parser Testing Data

### Test Case 1: Complex Multi-Package (English)

```kotlin
@Test
fun `parse complex multi-package message`() {
    val sms = """
        Dear Customer, your remaining amount from Monthly Internet Package 
        12GB from telebirr to be expired after 30 days is 4728.760 MB with 
        expiry date on 2026-04-06 at 17:38:02; from Monthly voice 150 Min 
        from telebirr to be expired after 30 days and 76 Min night package 
        bonus valid for 30 days is 114 minute and 34 second with expiry date 
        on 2026-04-10 at 11:08:07
    """.trimIndent()
    
    val parser = EnglishSmsParser()
    val result = parser.parseMultiPackage(sms)
    
    // Assert Internet Package
    assertEquals(2, result.size)
    assertEquals(PackageType.INTERNET, result[0].packageType)
    assertEquals(4728.760, result[0].remainingAmount, 0.001)
    assertEquals("2026-04-06 at 17:38:02", result[0].expiryDate)
    
    // Assert Voice Package
    assertEquals(PackageType.VOICE, result[1].packageType)
    assertTrue(result[1].packageName.contains("voice", ignoreCase = true))
}
```

### Test Case 2: Weekly Youth Package (English)

```kotlin
@Test
fun `parse weekly youth package with night bonus`() {
    val sms = """
        Your Weekly Youth 3.6GB + 3.6GB night bonus from tele birr with 
        360MB free data for Spotify to be expired after 7 days will be 
        expired on 2026-03-10 at 20:30:55
    """.trimIndent()
    
    val parser = EnglishSmsParser()
    val result = parser.parseWeeklyPackage(sms)
    
    assertNotNull(result)
    assertEquals(3686.4, result!!.dayData, 0.1)  // 3.6GB in MB
    assertEquals(3686.4, result.nightBonus, 0.1)
    assertEquals(360.0, result.freeData, 0.1)
    assertEquals("Spotify", result.freeDataApp)
    assertEquals(7, result.validityDays)
}
```

### Test Case 3: Amharic Transaction (Amharic)

```kotlin
@Test
fun `parse Amharic telebirr transaction`() {
    val sms = """
        በቴሌብር ስላደረጉት የገንዘብ ዝውውር እናመሰግናለን። ወደ *830*1# በመደወል 
        በቴሌፕለይ ጨዋታዎች እለታዊ 25,000 ብር፣ ሳምንታዊ 100,000 ብር፣ ወርሃዊ 
        250,000 ብር እና ዘመናዊ የኤሌክትሪክ መኪና እንዲሁም ሌሎች አጓጊ 
        ሽልማቶችን ያሸንፉ!
    """.trimIndent()
    
    val parser = AmharicSmsParser()
    val result = parser.parseTransaction(sms)
    
    assertNotNull(result)
    assertEquals("*830*1#", result!!.ussdCode)
    assertEquals(3, result.prizeAmounts.size)
    assertEquals(25000.0, result.prizeAmounts[0], 0.1)
    assertEquals(100000.0, result.prizeAmounts[1], 0.1)
    assertEquals(250000.0, result.prizeAmounts[2], 0.1)
}
```

### Test Case 4: TeleCoin Reward (Amharic)

```kotlin
@Test
fun `parse TeleCoin rewards from Amharic SMS`() {
    val sms = """
        የቴሌብር ግብይቶ 4 ነፃ የቴሌኮይን አስገኝቶሎታል። ይህን 
        https://teleplay.et/am ማስፈንጠሪያ በመንካት ሽልማቶን ይመልከቱ። 
        የቴሌኮይኖት የአገልግሎት ማብቅያ ጊዜ በ24/03/2026 ነው።
    """.trimIndent()
    
    val parser = AmharicSmsParser()
    val result = parser.parseTeleCoinReward(sms)
    
    assertNotNull(result)
    assertEquals(4, result!!.coinCount)
    assertEquals("2026-03-24", result.expiryDate)
}
```

---

## Parsing Best Practices

### 1. Case-Insensitive Matching
Always use `RegexOption.IGNORE_CASE` for user-facing text:

```kotlin
val pattern = """Monthly\s+Internet""".toRegex(RegexOption.IGNORE_CASE)
```

### 2. Handle Number Formats
Remove commas before parsing:

```kotlin
val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
```

### 3. Unit Conversion
Standardize to MB for internet data:

```kotlin
fun toMB(amount: Double, unit: String): Double {
    return when (unit.uppercase()) {
        "GB" -> amount * 1024
        "MB" -> amount
        else -> 0.0
    }
}
```

### 4. Date Normalization
Convert all dates to ISO 8601 format (YYYY-MM-DD):

```kotlin
fun normalizeDate(date: String, format: String): String {
    return when (format) {
        "DD/MM/YYYY" -> {
            val parts = date.split("/")
            "${parts[2]}-${parts[1]}-${parts[0]}"
        }
        "YYYY-MM-DD" -> date
        else -> date
    }
}
```

### 5. Fallback Values
Always provide defaults for missing data:

```kotlin
val validityDays = match.groupValues[5].toIntOrNull() ?: 30
val expiryDate = match.groupValues[6] ?: "Unknown"
```

---

## Error Handling

### Parser Error States

```kotlin
sealed class ParseResult {
    data class Success(val packages: List<BalancePackage>) : ParseResult()
    data class PartialSuccess(
        val packages: List<BalancePackage>,
        val errors: List<String>
    ) : ParseResult()
    data class Failure(val error: String) : ParseResult()
}

fun parseWithErrorHandling(sms: String): ParseResult {
    return try {
        val packages = parseMultiPackage(sms)
        if (packages.isEmpty()) {
            ParseResult.Failure("No packages found in SMS")
        } else {
            ParseResult.Success(packages)
        }
    } catch (e: Exception) {
        ParseResult.Failure("Parse error: ${e.message}")
    }
}
```

### Logging Unparsed SMS

```kotlin
@Entity(tableName = "sms_log")
data class SmsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val parsed: Boolean,
    val errorMessage: String? = null
)

// Log for debugging
suspend fun logUnparsedSms(sms: String, sender: String, error: String?) {
    smsLogDao.insert(
        SmsLog(
            sender = sender,
            body = sms,
            receivedAt = System.currentTimeMillis(),
            parsed = false,
            errorMessage = error
        )
    )
}
```

---

## Summary

### Supported SMS Types

| Type | Language | Sender | Status |
|------|----------|--------|--------|
| Multi-Package Balance | English | 251994 | ✅ Implemented |
| Weekly Youth Package | English | 251994 | ✅ Implemented |
| Bonus Funds | English | 251994 | ✅ Implemented |
| Transaction Notification | Amharic | *830* | ✅ Implemented |
| TeleCoin Reward | Amharic | *830* | ✅ Implemented |
| Bank Deposit | English | Bank Names | ✅ Implemented |
| Package Purchase | English | 251994 | ✅ Implemented |

### Parser Accuracy Goals

- English SMS: **95%+** accuracy
- Amharic SMS: **90%+** accuracy
- Multi-package: Handle **10+** packages per SMS
- Performance: Parse in **<500ms**

---

## Future Enhancements

1. Machine Learning parser for unpredictable formats
2. Support for Oromiffa SMS patterns
3. Support for Tigrinya SMS patterns
4. Auto-learning from user corrections
5. Cloud-based pattern updates (opt-in)
