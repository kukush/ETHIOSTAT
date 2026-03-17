# Multilingual Support Guide - ETHIOSTAT

## Overview

ETHIOSTAT supports multiple Ethiopian languages for both UI localization and SMS parsing:
- **English** (Primary)
- **Amharic (አማርኛ)** (Primary)
- **Oromiffa** (Optional)
- **Tigrinya** (Optional)

---

## Architecture

### Two-Layer Multilingual System

#### Layer 1: UI Localization (Android Resources)
Standard Android localization using `strings.xml` files:
```
res/values/strings.xml           → English (default)
res/values-am/strings.xml        → Amharic
res/values-om/strings.xml        → Oromiffa (optional)
res/values-ti/strings.xml        → Tigrinya (optional)
```

#### Layer 2: SMS Parsing (Language Detection + Parsers)
Smart parsing system that automatically detects SMS language and applies appropriate regex patterns.

---

## Language Detection

### Unicode Range Detection

```kotlin
enum class SmsLanguage {
    ENGLISH,
    AMHARIC,
    MIXED,
    UNKNOWN
}

class SmsLanguageDetector {
    fun detectLanguage(text: String): SmsLanguage {
        val amharicChars = text.count { it in '\u1200'..'\u137F' }
        val latinChars = text.count { it in 'A'..'Z' || it in 'a'..'z' }
        val totalChars = text.length
        
        return when {
            amharicChars > totalChars * 0.3 -> SmsLanguage.AMHARIC
            latinChars > totalChars * 0.5 -> SmsLanguage.ENGLISH
            amharicChars > 0 && latinChars > 0 -> SmsLanguage.MIXED
            else -> SmsLanguage.UNKNOWN
        }
    }
    
    fun hasAmharicCharacters(text: String): Boolean {
        return text.any { it in '\u1200'..'\u137F' }
    }
    
    fun hasEnglishCharacters(text: String): Boolean {
        return text.any { it in 'A'..'Z' || it in 'a'..'z' }
    }
}
```

### Amharic Unicode Blocks

| Block Name | Range | Characters |
|------------|-------|------------|
| Ethiopic | U+1200–U+137F | ሀ-፿ |
| Ethiopic Supplement | U+1380–U+139F | ᎀ-᎙ |
| Ethiopic Extended | U+2D80–U+2DDF | ⶀ-⷟ |

**Most Common**: U+1200–U+137F covers standard Amharic script.

---

## UI Localization

### String Resources Structure

#### English (`res/values/strings.xml`)
```xml
<resources>
    <string name="app_name">ETHIOSTAT</string>
    <string name="dashboard_title">Dashboard</string>
    <string name="internet_balance">Internet Balance</string>
    <string name="voice_balance">Voice Balance</string>
    <string name="bonus_funds">Bonus Funds</string>
    <string name="sync_now">Sync Now</string>
    <string name="settings">Settings</string>
    <string name="language">Language</string>
    <string name="income">Income</string>
    <string name="expense">Expense</string>
    <string name="expires_in">Expires in %d days</string>
    <string name="mb_unit">MB</string>
    <string name="gb_unit">GB</string>
    <string name="minutes_unit">Minutes</string>
    <string name="birr_unit">Birr</string>
</resources>
```

#### Amharic (`res/values-am/strings.xml`)
```xml
<resources>
    <string name="app_name">ኢትዮስታት</string>
    <string name="dashboard_title">ዋና ገጽ</string>
    <string name="internet_balance">የኢንተርኔት ቀሪ</string>
    <string name="voice_balance">የድምፅ ቀሪ</string>
    <string name="bonus_funds">ጉርሻ ገንዘብ</string>
    <string name="sync_now">አሁን አመሳስል</string>
    <string name="settings">ቅንብሮች</string>
    <string name="language">ቋንቋ</string>
    <string name="income">ገቢ</string>
    <string name="expense">ወጪ</string>
    <string name="expires_in">በ %d ቀናት ውስጥ ያበቃል</string>
    <string name="mb_unit">ሜባ</string>
    <string name="gb_unit">ጊባ</string>
    <string name="minutes_unit">ደቂቃዎች</string>
    <string name="birr_unit">ብር</string>
</resources>
```

### Language Preference Management

```kotlin
enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    AMHARIC("am", "አማርኛ"),
    OROMIFFA("om", "Afaan Oromoo"),
    TIGRINYA("ti", "ትግርኛ")
}

class LanguageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    fun getCurrentLanguage(): AppLanguage {
        val code = prefs.getString("language", "en") ?: "en"
        return AppLanguage.values().find { it.code == code } ?: AppLanguage.ENGLISH
    }
    
    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString("language", language.code).apply()
        updateLocale(language)
    }
    
    private fun updateLocale(language: AppLanguage) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }
}
```

---

## SMS Parsing by Language

### Multilingual Parser Architecture

```kotlin
interface SmsParser {
    fun parse(smsBody: String, sender: String): ParsedSmsData
    fun canParse(smsBody: String): Boolean
}

class MultilingualSmsParser(
    private val languageDetector: SmsLanguageDetector,
    private val englishParser: EnglishSmsParser,
    private val amharicParser: AmharicSmsParser
) : SmsParser {
    
    override fun parse(smsBody: String, sender: String): ParsedSmsData {
        val language = languageDetector.detectLanguage(smsBody)
        
        return when (language) {
            SmsLanguage.ENGLISH -> englishParser.parse(smsBody, sender)
            SmsLanguage.AMHARIC -> amharicParser.parse(smsBody, sender)
            SmsLanguage.MIXED -> parseMixed(smsBody, sender)
            SmsLanguage.UNKNOWN -> ParsedSmsData.empty()
        }
    }
    
    override fun canParse(smsBody: String): Boolean {
        return englishParser.canParse(smsBody) || 
               amharicParser.canParse(smsBody)
    }
    
    private fun parseMixed(smsBody: String, sender: String): ParsedSmsData {
        val englishResult = englishParser.parse(smsBody, sender)
        val amharicResult = amharicParser.parse(smsBody, sender)
        
        return englishResult.merge(amharicResult)
    }
}
```

---

## Amharic SMS Patterns

### Common Amharic Keywords

#### Financial Terms
| Amharic | English | Usage |
|---------|---------|-------|
| የገንዘብ ዝውውር | Money transfer | Transaction notification |
| ገቢ | Income | Credit notification |
| ወጪ | Expense | Debit notification |
| ተቀባይ | Recipient | Transfer recipient |
| ላኪ | Sender | Transfer sender |
| ቀሪ ሂሳብ | Balance | Account balance |

#### Telecom Terms
| Amharic | English | Usage |
|---------|---------|-------|
| የኢንተርኔት ቦታ | Internet data | Data package |
| የድምፅ ደቂቃዎች | Voice minutes | Voice package |
| ጉርሻ | Bonus | Bonus package |
| ማብቂያ ቀን | Expiry date | Package expiration |
| ቴሌብር | telebirr | Payment method |
| ቴሌኮይን | TeleCoin | Reward coin |

### Amharic Number Patterns

```kotlin
class AmharicNumberParser {
    private val amharicDigits = mapOf(
        '0' to '0', '1' to '1', '2' to '2', '3' to '3', '4' to '4',
        '5' to '5', '6' to '6', '7' to '7', '8' to '8', '9' to '9'
    )
    
    fun parseNumber(amharicText: String): Double? {
        val numberPattern = """(\d+,?\d*\.?\d*)\s*ብር""".toRegex()
        val match = numberPattern.find(amharicText)
        
        return match?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
    }
    
    fun parseDateAmharic(dateText: String): String? {
        // Format: በ24/03/2026 ነው → 2026-03-24
        val pattern = """በ(\d{2})/(\d{2})/(\d{4})\s*ነው""".toRegex()
        val match = pattern.find(dateText)
        
        return match?.let {
            val day = it.groupValues[1]
            val month = it.groupValues[2]
            val year = it.groupValues[3]
            "$year-$month-$day"
        }
    }
}
```

### Date Format Conversion

**Amharic SMS Format**: `በ24/03/2026 ነው`  
**Converted to ISO**: `2026-03-24`

**English SMS Format**: `2026-04-06 at 17:38:02`  
**ISO Format**: `2026-04-06T17:38:02`

---

## Language Switcher UI

### Compose Implementation

```kotlin
@Composable
fun LanguagePicker(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageOption(
                    language = AppLanguage.ENGLISH,
                    flag = "🇬🇧",
                    label = "English",
                    isSelected = currentLanguage == AppLanguage.ENGLISH,
                    onClick = { onLanguageChange(AppLanguage.ENGLISH) },
                    modifier = Modifier.weight(1f)
                )
                
                LanguageOption(
                    language = AppLanguage.AMHARIC,
                    flag = "🇪🇹",
                    label = "አማርኛ",
                    isSelected = currentLanguage == AppLanguage.AMHARIC,
                    onClick = { onLanguageChange(AppLanguage.AMHARIC) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LanguageOption(
    language: AppLanguage,
    flag: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
                else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = flag,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

---

## SMS Parser Configuration

### Enable/Disable Language Parsers

```kotlin
data class ParserConfig(
    val parseEnglish: Boolean = true,
    val parseAmharic: Boolean = true,
    val parseOromiffa: Boolean = false,
    val parseTigrinya: Boolean = false
)

class ConfigurableSmsParser(
    private val config: ParserConfig,
    private val parsers: Map<SmsLanguage, SmsParser>
) {
    fun parse(sms: String, sender: String): ParsedSmsData {
        val language = detectLanguage(sms)
        
        val parser = when {
            language == SmsLanguage.ENGLISH && config.parseEnglish -> 
                parsers[SmsLanguage.ENGLISH]
            language == SmsLanguage.AMHARIC && config.parseAmharic -> 
                parsers[SmsLanguage.AMHARIC]
            else -> null
        }
        
        return parser?.parse(sms, sender) ?: ParsedSmsData.empty()
    }
}
```

---

## Testing Multilingual Features

### Unit Tests

```kotlin
@Test
fun `detect Amharic language correctly`() {
    val amharicText = "በቴሌብር ስላደረጉት የገንዘብ ዝውውር እናመሰግናለን"
    val detector = SmsLanguageDetector()
    
    assertEquals(SmsLanguage.AMHARIC, detector.detectLanguage(amharicText))
}

@Test
fun `detect English language correctly`() {
    val englishText = "Your balance is 4728.760 MB"
    val detector = SmsLanguageDetector()
    
    assertEquals(SmsLanguage.ENGLISH, detector.detectLanguage(englishText))
}

@Test
fun `detect mixed language correctly`() {
    val mixedText = "Dear Customer በ24/03/2026 ነው"
    val detector = SmsLanguageDetector()
    
    assertEquals(SmsLanguage.MIXED, detector.detectLanguage(mixedText))
}

@Test
fun `parse Amharic date format`() {
    val dateText = "የአገልግሎት ማብቅያ ጊዜ በ24/03/2026 ነው"
    val parser = AmharicNumberParser()
    
    assertEquals("2026-03-24", parser.parseDateAmharic(dateText))
}
```

---

## Best Practices

### 1. Always Use String Resources
```kotlin
// ✅ Good
Text(text = stringResource(R.string.internet_balance))

// ❌ Bad
Text(text = "Internet Balance")
```

### 2. Handle Right-to-Left (RTL)
Amharic is Left-to-Right (LTR), but be prepared for other languages:
```kotlin
CompositionLocalProvider(
    LocalLayoutDirection provides LayoutDirection.Ltr
) {
    // Your composable
}
```

### 3. Use Unicode-Safe Regex
```kotlin
// ✅ Good: Unicode-aware
val pattern = """[\u1200-\u137F]+""".toRegex()

// ❌ Bad: ASCII-only
val pattern = """[a-zA-Z]+""".toRegex()
```

### 4. Store Language in Database
```kotlin
@Entity
data class ParsedSms(
    val id: Long,
    val originalLanguage: String, // "en" or "am"
    val content: String,
    val parsedData: String
)
```

---

## Future Extensions

### Adding New Languages

1. Create `values-{code}/strings.xml`
2. Implement `{Language}SmsParser`
3. Add to `AppLanguage` enum
4. Update `MultilingualSmsParser`
5. Add unit tests

### Oromiffa Support (Future)
- Language code: `om`
- Script: Latin with special characters
- SMS patterns: TBD

### Tigrinya Support (Future)
- Language code: `ti`
- Script: Ge'ez (same as Amharic)
- SMS patterns: Similar to Amharic

---

## Summary

ETHIOSTAT's multilingual architecture provides:
- ✅ Automatic language detection for SMS
- ✅ Separate parsers for English and Amharic
- ✅ UI localization via Android resources
- ✅ User-selectable app language
- ✅ Configurable parser enablement
- ✅ Unicode-safe regex patterns
- ✅ Date format conversion
- ✅ Extensible for future languages
