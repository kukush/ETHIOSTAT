package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.*
import java.util.regex.Pattern

class OromoSmsParser : SmsParser {
    
    override fun canParse(smsBody: String): Boolean {
        val cleanedBody = smsBody.trim().lowercase()
        val oromoKeywords = listOf(
            "hanqina", "balansi", "argatte", "fudhatte", "ergite", "kaffale",
            "dabarsuu", "kaffaltii", "kuusuu", "baasuu", "birr", "telebirr",
            "herrega", "akawuntii", "daldalaa",
            // TeleBirr Oromo transfer keywords (sender: 127)
            "qarshii", "hafteen", "kabajamoo"
        )
        return oromoKeywords.any { cleanedBody.contains(it) }
    }
    
    companion object {
        // Oromo keywords for transaction detection
        private val OROMO_KEYWORDS = mapOf(
            "balance" to listOf("hanqina", "balansi", "hafe"),
            "received" to listOf("argatte", "fudhatte", "galte", "qaqqabeera", "seene", "galii"),
            "sent" to listOf("ergite", "kaffale", "bahe", "ergeera", "baasii"),
            "transfer" to listOf("dabarsuu", "erguu"),
            "payment" to listOf("kaffaltii", "baasii"),
            "deposit" to listOf("kuusuu", "galchuu", "galii", "seene"),
            "withdrawal" to listOf("baasuu", "fudhachuu", "bahe", "baasii"),
            "birr" to listOf("birr", "br", "qarshii", "etb"),
            "telebirr" to listOf("telebirr", "telebir"),
            "account" to listOf("herrega", "akawuntii"),
            "transaction" to listOf("daldalaa", "jijjiirraa")
        )
        
        // Oromo number patterns
        private val OROMO_AMOUNT_PATTERNS = listOf(
            Pattern.compile("birr\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("br\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([\\d,]+(?:\\.\\d{2})?)\\s*birr", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([\\d,]+(?:\\.\\d{2})?)\\s*br", Pattern.CASE_INSENSITIVE),
            Pattern.compile("qarshii\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([\\d,]+(?:\\.\\d{2})?)\\s*qarshii", Pattern.CASE_INSENSITIVE),
            Pattern.compile("etb\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([\\d,]+(?:\\.\\d{2})?)\\s*etb", Pattern.CASE_INSENSITIVE)
        )
        
        // Telebirr specific patterns in Oromo
        private val TELEBIRR_PATTERNS = mapOf(
            "balance_check" to Pattern.compile("hanqina.*telebirr.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            "received_money" to Pattern.compile("argatte.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            "sent_money" to Pattern.compile("ergite.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            "payment" to Pattern.compile("kaffaltii.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE)
        )

        // TeleBirr Oromo transfer patterns (sender: 127)
        // Sample: "Qarshii 1,200.00 herrega telebirr keessan ... irraa gara ..."
        private val TELEBIRR_OROMO_TRANSFER_OUT = Regex(
            """Qarshii\s+([\d,]+\.?\d*)\s+herrega\s+telebirr\s+keessan\s+(\d+)\s+irraa\s+gara\s+(.+?)\s+lakkoofsa""",
            RegexOption.IGNORE_CASE
        )
        // Sample: "Hafteen herregaa amma qabdan Qarshii 13.95 dha."
        private val TELEBIRR_OROMO_BALANCE = Regex(
            """Hafteen\s+herregaa\s+amma\s+qabdan\s+Qarshii\s+([\d,]+\.?\d*)""",
            RegexOption.IGNORE_CASE
        )
        // Service fee: "Kaffaltiin tajaajilla Qarshii 5.22 dha."
        private val TELEBIRR_OROMO_FEE = Regex(
            """Kaffaltiin\s+tajaajilla\s+Qarshii\s+([\d,]+\.?\d*)\s+dha""",
            RegexOption.IGNORE_CASE
        )
        
        // Bank transaction patterns in Oromo
        private val BANK_PATTERNS = mapOf(
            "deposit" to Pattern.compile("kuusuu.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            "withdrawal" to Pattern.compile("baasuu.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            "transfer_in" to Pattern.compile("argatte.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            "transfer_out" to Pattern.compile("dabarsuu.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE)
        )
    }
    
    override fun parse(smsBody: String, sender: String): ParsedSmsData {
        val cleanedBody = smsBody.trim().lowercase()
        
        return when {
            isTelebirrMessage(sender, cleanedBody) -> parseTelebirrMessage(cleanedBody, sender)
            isBankMessage(sender, cleanedBody) -> parseBankMessage(cleanedBody, sender)
            isTelecomMessage(sender, cleanedBody) -> parseTelecomMessage(cleanedBody, sender)
            else -> ParsedSmsData.empty()
        }
    }
    
    private fun isTelebirrMessage(sender: String, body: String): Boolean {
        return sender.contains("830") ||
               sender.contains("127") ||   // TeleBirr Oromo sender
               body.contains("telebirr") ||
               body.contains("qarshii") || // Oromo currency keyword
               OROMO_KEYWORDS["telebirr"]?.any { body.contains(it) } == true
    }
    
    private fun isBankMessage(sender: String, body: String): Boolean {
        val bankKeywords = listOf("bank", "banki", "herrega", "akawuntii")
        return bankKeywords.any { body.contains(it) } ||
               sender.contains("cbe", ignoreCase = true) ||
               sender.contains("awash", ignoreCase = true) ||
               sender.contains("zemen", ignoreCase = true)
    }
    
    private fun isTelecomMessage(sender: String, body: String): Boolean {
        return sender.contains("251994") ||
               sender.contains("ethio", ignoreCase = true) ||
               body.contains("ethio telecom")
    }
    
    private fun parseTelebirrMessage(body: String, sender: String): ParsedSmsData {
        // Try specific Oromo transfer-out pattern first (sender 127)
        val transferMatch = TELEBIRR_OROMO_TRANSFER_OUT.find(body)
        if (transferMatch != null) {
            val amount = transferMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                ?: return ParsedSmsData.empty()
            val destination = transferMatch.groupValues[3].trim()
            val transaction = Transaction(
                amount = amount,
                type = TransactionType.EXPENSE,
                category = "Transfer",
                source = "TeleBirr",
                description = "TeleBirr transfer to $destination",
                timestamp = System.currentTimeMillis(),
                accountSource = AccountSourceType.TELEBIRR,
                sourcePhoneNumber = sender,
                isClassified = true
            )
            return ParsedSmsData(
                isParsed = true,
                transaction = transaction,
                packages = emptyList(),
                language = SmsLanguage.OROMO
            )
        }

        // Fallback: generic amount extraction
        val amount = extractAmount(body) ?: return ParsedSmsData.empty()
        
        // Determine transaction type
        val transactionType = when {
            OROMO_KEYWORDS["received"]?.any { body.contains(it) } == true -> TransactionType.INCOME
            OROMO_KEYWORDS["sent"]?.any { body.contains(it) } == true -> TransactionType.EXPENSE
            OROMO_KEYWORDS["payment"]?.any { body.contains(it) } == true -> TransactionType.EXPENSE
            body.contains("irraa gara") -> TransactionType.EXPENSE // "from ... to ..." = transfer out
            else -> TransactionType.INCOME
        }
        
        val transaction = Transaction(
            amount = amount,
            type = transactionType,
            category = "Mobile Money",
            source = "TeleBirr",
            description = "TeleBirr transaction (Oromo)",
            timestamp = System.currentTimeMillis(),
            accountSource = AccountSourceType.TELEBIRR,
            sourcePhoneNumber = sender,
            isClassified = true
        )
        
        return ParsedSmsData(
            isParsed = true,
            transaction = transaction,
            packages = emptyList(),
            language = SmsLanguage.OROMO
        )
    }
    
    private fun parseBankMessage(body: String, sender: String): ParsedSmsData {
        val amount = extractAmount(body) ?: return ParsedSmsData.empty()
        
        val transactionType = when {
            OROMO_KEYWORDS["deposit"]?.any { body.contains(it) } == true -> TransactionType.INCOME
            OROMO_KEYWORDS["received"]?.any { body.contains(it) } == true -> TransactionType.INCOME
            OROMO_KEYWORDS["withdrawal"]?.any { body.contains(it) } == true -> TransactionType.EXPENSE
            OROMO_KEYWORDS["sent"]?.any { body.contains(it) } == true -> TransactionType.EXPENSE
            else -> TransactionType.INCOME
        }
        
        val bankType = when {
            sender.contains("cbe", ignoreCase = true) -> AccountSourceType.BANK_CBE
            sender.contains("awash", ignoreCase = true) -> AccountSourceType.BANK_AWASH
            else -> AccountSourceType.BANK_OTHER
        }
        
        val transaction = Transaction(
            amount = amount,
            type = transactionType,
            category = "Banking",
            source = "Bank",
            description = "Bank transaction (Oromo)",
            timestamp = System.currentTimeMillis(),
            accountSource = bankType,
            sourcePhoneNumber = sender,
            isClassified = true
        )
        
        return ParsedSmsData(
            isParsed = true,
            transaction = transaction,
            packages = emptyList(),
            language = SmsLanguage.OROMO
        )
    }
    
    private fun parseTelecomMessage(body: String, sender: String): ParsedSmsData {
        val packages = mutableListOf<BalancePackage>()
        
        // 1. Check for specific Account Balance pattern
        // Sample: "Hafteen herregaa amma qabdan Qarshii 13.95 dha."
        val balanceMatch = TELEBIRR_OROMO_BALANCE.find(body)
        if (balanceMatch != null) {
            val amount = balanceMatch.groupValues[1].replace(",", "").toDoubleOrNull()
            if (amount != null) {
                packages.add(BalancePackage(
                    packageType = PackageType.MAIN_BALANCE,
                    packageName = "Account Balance",
                    remainingAmount = amount,
                    totalAmount = amount,
                    unit = "Birr",
                    source = "Ethio Telecom",
                    validityDays = 365,
                    expiryDate = "Check SMS for details",
                    expiryTimestamp = System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L),
                    language = "om"
                ))
            }
        }
        
        // 2. Check for Awarded Bonus pattern
        // Sample: "Boonasii Qarshii 7.50 badhaafamtaniittu"
        val bonusPattern = Regex("""(?:Boonasii\s+)?Qarshii\s*([\d,]+\.?\d*)\s*(?:badhaafamtaniittu|badhaafamteera)""", RegexOption.IGNORE_CASE)
        val bonusMatch = bonusPattern.find(body)
        if (bonusMatch != null) {
            val amount = bonusMatch.groupValues[1].replace(",", "").toDoubleOrNull()
            if (amount != null) {
                packages.add(BalancePackage(
                    packageType = PackageType.BONUS_FUND,
                    packageName = "Recharge Bonus",
                    remainingAmount = amount,
                    totalAmount = amount,
                    unit = "Birr",
                    source = "Ethio Telecom",
                    validityDays = 3,
                    expiryDate = "Bonus reward",
                    expiryTimestamp = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L),
                    language = "om"
                ))
            }
        }
        
        // 3. Fallback: only if nothing specific matched and we find a "Balance/Birr" context
        if (packages.isEmpty() && (body.contains("hafeen") || body.contains("balansii"))) {
            extractAmount(body)?.let { amount ->
                packages.add(BalancePackage(
                    packageType = PackageType.BONUS_FUND,
                    remainingAmount = amount,
                    totalAmount = amount,
                    unit = "Birr",
                    source = "Ethio Telecom",
                    packageName = "Telecom Balance",
                    validityDays = 30,
                    expiryDate = "",
                    expiryTimestamp = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
                    language = "om"
                ))
            }
        }
        
        return if (packages.isNotEmpty()) {
            ParsedSmsData(
                isParsed = true,
                transaction = null,
                packages = packages,
                language = SmsLanguage.OROMO
            )
        } else {
            ParsedSmsData.empty()
        }
    }
    
    private fun extractAmount(text: String): Double? {
        for (pattern in OROMO_AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return try {
                    matcher.group(1).replace(",", "").toDouble()
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        return null
    }
}
