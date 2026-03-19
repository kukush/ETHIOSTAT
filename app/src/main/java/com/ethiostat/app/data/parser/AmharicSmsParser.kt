package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class AmharicSmsParser : SmsParser {

    override fun canParse(smsBody: String): Boolean {
        val amharicCharRegex = Regex("""[\u1200-\u137F]""")
        // Consider it parsable if it contains Amharic script
        return amharicCharRegex.containsMatchIn(smsBody)
    }

    override fun parse(smsBody: String, sender: String): ParsedSmsData {
        // Skip telecom sender regarding bank transactions
        val transaction = if (sender.contains("251994", ignoreCase = true) || 
                              sender.contains("ethio telecom", ignoreCase = true)) {
            null
        } else {
            parseTransactionForSender(smsBody, sender)
        }

        // Packages parsing could go here, omitting for simplicity since we mostly need financial
        // But keeping it compatible if needed
        val packages = emptyList<BalancePackage>()

        return if (transaction != null) {
            ParsedSmsData.success(
                packages = packages,
                transaction = transaction,
                language = SmsLanguage.AMHARIC
            )
        } else {
            ParsedSmsData.empty()
        }
    }

    private fun parseTransactionForSender(smsBody: String, sender: String): Transaction? {
        val uppercaseSender = sender.uppercase()
        return when {
            uppercaseSender.contains("CBE") -> parseCbeTransaction(smsBody, sender)
            uppercaseSender.contains("BOA") || uppercaseSender.contains("ABYSSINIA") -> parseBoaTransaction(smsBody, sender)
            uppercaseSender.contains("AWASH") -> parseAwashTransaction(smsBody, sender)
            uppercaseSender.contains("830") || uppercaseSender.contains("127") -> parseTelebirrTransaction(smsBody, sender)
            else -> parseGenericTransaction(smsBody, sender)
        }
    }

    private fun parseCbeTransaction(smsBody: String, sender: String): Transaction? {
        // CBE Amharic Credit: በብር 5,000.00 ተመዝግቧል
        val creditPattern = Regex("""በብር\s*([\d,]+\.?\d*)\s*ተመዝግቧል""")
        val creditMatch = creditPattern.find(smsBody)
        if (creditMatch != null) {
            val amount = creditMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return createTransaction(amount, TransactionType.INCOME, "CBE Credit (Amharic)", sender, AccountSourceType.BANK_CBE)
        }

        // CBE Amharic Debit: በብር 1,000.00 ተቀንሷል
        val debitPattern = Regex("""በብር\s*([\d,]+\.?\d*)\s*ተቀንሷል""")
        val debitMatch = debitPattern.find(smsBody)
        if (debitMatch != null) {
            val amount = debitMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return createTransaction(amount, TransactionType.EXPENSE, "CBE Debit (Amharic)", sender, AccountSourceType.BANK_CBE)
        }
        return null
    }

    private fun parseBoaTransaction(smsBody: String, sender: String): Transaction? {
        // BOA Amharic Credit: ብር 5,000.00 ተቀማጭ ተደርጓል
        val creditPattern = Regex("""ብር\s*([\d,]+\.?\d*)\s*ተቀማጭ""")
        val creditMatch = creditPattern.find(smsBody)
        if (creditMatch != null) {
            val amount = creditMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return createTransaction(amount, TransactionType.INCOME, "BOA Credit (Amharic)", sender, AccountSourceType.BANK_BOA)
        }

        // BOA Amharic Debit: ብር 1,000.00 ወጪ ተደርጓል
        val debitPattern = Regex("""ብር\s*([\d,]+\.?\d*)\s*ወጪ""")
        val debitMatch = debitPattern.find(smsBody)
        if (debitMatch != null) {
            val amount = debitMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return createTransaction(amount, TransactionType.EXPENSE, "BOA Debit (Amharic)", sender, AccountSourceType.BANK_BOA)
        }
        return null
    }

    private fun parseAwashTransaction(smsBody: String, sender: String): Transaction? {
        // Awash Amharic Credit: በ 5,000.00 ብር ገቢ ሆኗል
        val creditPattern = Regex("""(?:በ\s*)?([\d,]+\.?\d*)\s*ብር\s*ገቢ""")
        val creditMatch = creditPattern.find(smsBody)
        if (creditMatch != null) {
            val amount = creditMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return createTransaction(amount, TransactionType.INCOME, "Awash Credit (Amharic)", sender, AccountSourceType.BANK_AWASH)
        }

        // Awash Amharic Debit: በ 2,000.00 ብር ወጪ ሆኗል
        val debitPattern = Regex("""(?:በ\s*)?([\d,]+\.?\d*)\s*ብር\s*ወጪ""")
        val debitMatch = debitPattern.find(smsBody)
        if (debitMatch != null) {
            val amount = debitMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return createTransaction(amount, TransactionType.EXPENSE, "Awash Debit (Amharic)", sender, AccountSourceType.BANK_AWASH)
        }
        return null
    }

    private fun parseTelebirrTransaction(smsBody: String, sender: String): Transaction? {
        // Telebirr Amharic Credit: ብር 500.00 ደርሶዎታል
        val creditPattern = Regex("""ብር\s*([\d,]+\.?\d*)\s*ደርሶዎታል""")
        val creditMatch = creditPattern.find(smsBody)
        if (creditMatch != null) {
            val amount = creditMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return createTransaction(amount, TransactionType.INCOME, "TeleBirr Receive (Amharic)", sender, AccountSourceType.TELEBIRR)
        }

        // Telebirr Amharic Debit: ብር 200.00 በተሳካ ሁኔታ ልከዋል
        val debitPattern = Regex("""ብር\s*([\d,]+\.?\d*)\s*በተሳካ\s*ሁኔታ\s*(?:ልከዋል|ከፍለዋል)""")
        val debitMatch = debitPattern.find(smsBody)
        if (debitMatch != null) {
            val amount = debitMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return createTransaction(amount, TransactionType.EXPENSE, "TeleBirr Send (Amharic)", sender, AccountSourceType.TELEBIRR)
        }
        return null
    }

    private fun parseGenericTransaction(smsBody: String, sender: String): Transaction? {
        // Generic fallback using keywords
        val isIncome = smsBody.contains("ገቢ") || smsBody.contains("ተቀማጭ") || smsBody.contains("ደርሶዎታል") || smsBody.contains("ተመዝግቧል")
        val isExpense = smsBody.contains("ወጪ") || smsBody.contains("ልከዋል") || smsBody.contains("ተቀንሷል")

        if (!isIncome && !isExpense) return null

        val amountPattern = Regex("""(?:ብር|በብር|በ)\s*([\d,]+\.?\d*)""")
        val match = amountPattern.find(smsBody)
        val amount = match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        return amount?.let {
            createTransaction(
                amount = it,
                type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                category = "Generic Amharic Transaction",
                sourcePhoneNumber = sender,
                accountSourceType = AccountSourceType.BANK_OTHER
            )
        }
    }

    private fun createTransaction(amount: Double, type: TransactionType, category: String, sourcePhoneNumber: String, accountSourceType: AccountSourceType): Transaction {
        return Transaction(
            amount = amount,
            type = type,
            category = category,
            source = sourcePhoneNumber,
            description = category,
            timestamp = System.currentTimeMillis(),
            accountSource = accountSourceType,
            sourcePhoneNumber = sourcePhoneNumber,
            isClassified = true
        )
    }
}
