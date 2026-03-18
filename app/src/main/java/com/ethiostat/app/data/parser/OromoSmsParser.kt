package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.*
import java.util.regex.Pattern

class OromoSmsParser : SmsParser {
    
    override fun canParse(smsBody: String): Boolean {
        val cleanedBody = smsBody.trim().lowercase()
        val oromoKeywords = listOf(
            "hanqina", "balansi", "argatte", "fudhatte", "ergite", "kaffale",
            "dabarsuu", "kaffaltii", "kuusuu", "baasuu", "birr", "telebirr",
            "herrega", "akawuntii", "daldalaa"
        )
        return oromoKeywords.any { cleanedBody.contains(it) }
    }
    
    companion object {
        // Oromo keywords for transaction detection
        private val OROMO_KEYWORDS = mapOf(
            "balance" to listOf("hanqina", "balansi", "hafe"),
            "received" to listOf("argatte", "fudhatte", "galte"),
            "sent" to listOf("ergite", "kaffale", "bahe"),
            "transfer" to listOf("dabarsuu", "erguu"),
            "payment" to listOf("kaffaltii", "baasii"),
            "deposit" to listOf("kuusuu", "galchuu"),
            "withdrawal" to listOf("baasuu", "fudhachuu"),
            "birr" to listOf("birr", "br"),
            "telebirr" to listOf("telebirr", "telebir"),
            "account" to listOf("herrega", "akawuntii"),
            "transaction" to listOf("daldalaa", "jijjiirraa")
        )
        
        // Oromo number patterns
        private val OROMO_AMOUNT_PATTERNS = listOf(
            Pattern.compile("birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("br\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:\\.\\d{2})?)\\s*birr", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:\\.\\d{2})?)\\s*br", Pattern.CASE_INSENSITIVE)
        )
        
        // Telebirr specific patterns in Oromo
        private val TELEBIRR_PATTERNS = mapOf(
            "balance_check" to Pattern.compile("hanqina.*telebirr.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            "received_money" to Pattern.compile("argatte.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            "sent_money" to Pattern.compile("ergite.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            "payment" to Pattern.compile("kaffaltii.*birr\\s*(\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE)
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
               body.contains("telebirr") ||
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
        // Try to extract amount
        val amount = extractAmount(body) ?: return ParsedSmsData.empty()
        
        // Determine transaction type
        val transactionType = when {
            OROMO_KEYWORDS["received"]?.any { body.contains(it) } == true -> TransactionType.INCOME
            OROMO_KEYWORDS["sent"]?.any { body.contains(it) } == true -> TransactionType.EXPENSE
            OROMO_KEYWORDS["payment"]?.any { body.contains(it) } == true -> TransactionType.EXPENSE
            else -> TransactionType.INCOME // Default for balance checks
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
        // For telecom messages, we mainly extract balance packages
        val amount = extractAmount(body) ?: return ParsedSmsData.empty()
        
        // Create a simple balance package for telecom services
        val balancePackage = BalancePackage(
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
        )
        
        return ParsedSmsData(
            isParsed = true,
            transaction = null,
            packages = listOf(balancePackage),
            language = SmsLanguage.OROMO
        )
    }
    
    private fun extractAmount(text: String): Double? {
        for (pattern in OROMO_AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return try {
                    matcher.group(1).toDouble()
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        return null
    }
}
