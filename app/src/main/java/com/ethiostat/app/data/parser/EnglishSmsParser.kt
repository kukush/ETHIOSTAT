package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.*
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class EnglishSmsParser : SmsParser {

    // -----------------------------------------------------------------------
    // Primary multi-package pattern (splits on ';' segments from 251994 SMS)
    // FIX: Changed \d+\s+days  →  \d+\s*days? to handle "30days" (no space)
    // -----------------------------------------------------------------------
    private val multiPackagePattern = Regex(
        """from\s+([^;]+?)\s+from\s+telebirr\s+to\s+be\s+expired\s+after\s+\d+\s*days?(?:\s+and\s+[^;]+?)?\s*is\s+([\d,]+\.?\d*)\s*(MB|GB|minute)(?:\s+and\s+([\d,]+)\s+second)?\s+with\s+expiry\s+date\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2})""",
        setOf(RegexOption.IGNORE_CASE)
    )

    private val packageNamePattern = Regex(
        """(Monthly\s+Internet\s+Package\s+[\d.]+\s*(?:GB|MB)|Monthly\s+voice\s+\d+\s*Min|Weekly\s+Youth\s+[\d.]+GB|[\d.]+\s*Min\s+night\s+package\s+bonus)\s+from\s+(telebirr|tele\s+birr)""",
        RegexOption.IGNORE_CASE
    )

    private val validityPattern = Regex(
        """to\s+be\s+expired\s+after\s+(\d+)\s*days?""",
        RegexOption.IGNORE_CASE
    )

    private val voiceBalancePattern = Regex(
        """(\d+)\s+minute\s+and\s+(\d+)\s+second""",
        RegexOption.IGNORE_CASE
    )

    private val weeklyPackagePattern = Regex(
        """(Weekly\s+[\w\s]+[\d.]+GB)\s*\+\s*([\d.]+GB)\s+night\s+bonus\s+from\s+tele\s+birr\s+with\s+([\d.]+\s*(?:MB|GB))\s+free\s+data\s+for\s+(\w+)\s+to\s+be\s+expired\s+after\s+(\d+)\s+days?\s+will\s+be\s+expired\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2})""",
        RegexOption.IGNORE_CASE
    )

    private val bonusFundsPattern = Regex(
        """Bonus\s+Fund.*?([\d,]+\.?\d*)\s*Birr""",
        RegexOption.IGNORE_CASE
    )

    // Account balance in Birr from the first *804# response fragment
    // FIX: More specific pattern to avoid matching "Recharged balance" 
    private val accountBalanceBirrPattern = Regex(
        """Your\s+(?:total\s+)?balance\s+is\s+([\d,]+\.?\d*)\s*Birr""",
        RegexOption.IGNORE_CASE
    )

    // Bonus reward for recharging via telebirr (e.g. "awarded an ETB 7.50 bonus")
    private val awardedBonusPattern = Regex(
        """awarded\s+an\s+(?:ETB\s+)?([\d,]+\.?\d*)\s*bonus""",
        RegexOption.IGNORE_CASE
    )

    // -----------------------------------------------------------------------
    // Telecom USSD fallback patterns — used ONLY when multiPackagePattern yields nothing
    // -----------------------------------------------------------------------
    private val telecomInternetPattern = Regex(
        """(?i)internet\s+package.*?to\s+be\s+expired\s+after\s+\d+\s*days?\s+is\s+([\d,]+\.?\d*)\s*(MB|GB)(?:.*?expiry\s+date\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2}))?""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )
    private val telecomVoicePattern = Regex(
        """(?i)voice\s+\d+\s*Min.*?to\s+be\s+expired\s+after\s+\d+\s*days?(?:\s+and\s+[^;]+?)?\s+is\s+([\d,]+\.?\d*)\s*minute(?:\s+and\s+([\d,]+)\s*second)?(?:\s+with\s+expiry\s+date\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2}))?""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    // -----------------------------------------------------------------------
    // CBE transaction patterns (sender: CBE)
    // -----------------------------------------------------------------------
    private val cbeDebitPattern = Regex(
        """(?:debited\s+with|transfered)\s+ETB\s+([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )
    private val cbeBalancePattern = Regex(
        """(?:Your\s+Current\s+Balance|Available\s+Balance)\s+is\s+ETB\s+([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )
    private val cbeCreditPattern = Regex(
        """credited\s+with\s+ETB\s+([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )

    // -----------------------------------------------------------------------
    // BOA (Bank of Abyssinia) transaction patterns (sender: BOA)
    // -----------------------------------------------------------------------
    private val boaCreditPattern = Regex(
        """credited\s+(?:with\s+)?ETB\s+([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )
    private val boaDebitPattern = Regex(
        """debited\s+(?:with\s+)?ETB\s+([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )

    // -----------------------------------------------------------------------
    // Awash Bank transaction patterns (sender: AwashBank)
    // -----------------------------------------------------------------------
    private val awashCreditPattern = Regex(
        """credited\s+with\s+ETB\s+([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )
    private val awashDebitPattern = Regex(
        """debited\s+with\s+ETB\s+([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )

    private val incomeKeywords = listOf("received", "credited", "deposited")
    private val expenseKeywords = listOf("purchased", "debited", "transfered", "transferred", "sent")

    // -----------------------------------------------------------------------
    // Main parse() entry point
    // -----------------------------------------------------------------------
    override fun parse(smsBody: String, sender: String): ParsedSmsData {
        if (sender.contains("251994", ignoreCase = true)) {
            Log.d("EthioStat", "EnglishSmsParser.parse() - Telecom SMS length: ${smsBody.length}")
            Log.d("EthioStat", "EnglishSmsParser.parse() - Telecom SMS body: ${smsBody.take(500)}")
        }
        val allRaw = mutableListOf<BalancePackage>()

        // Step 1: primary multi-package parser
        val multiRaw = parseMultiPackageRaw(smsBody)
        allRaw.addAll(multiRaw)
        parseWeeklyPackage(smsBody)?.let { allRaw.add(it) }

        // Step 2: account balance in Birr from first 251994 fragment
        parseAccountBalanceBirr(smsBody)?.let { allRaw.add(it) }
        parseAwardedBonus(smsBody)?.let { allRaw.add(it) }

        // Step 3: Telecom USSD fallback — ONLY if primary parser found NO packages
        // FIX: previously parseTelecomUssdResponse() was always called regardless,
        // causing double-counting of internet packages for 251994 sender.
        if (allRaw.isEmpty()) {
            val telecomPackages = parseTelecomUssdResponse(smsBody)
            if (telecomPackages.isEmpty()) {
                allRaw.addAll(parseTelecomFallback(smsBody, sender))
            } else {
                allRaw.addAll(telecomPackages)
            }
        }

        // Step 4: Filter expired packages before combining
        val now = System.currentTimeMillis()
        val activeRaw = allRaw.filter { pkg ->
            // Keep package if: no expiry set (expiryTimestamp == 0L) OR expiry is in the future
            pkg.expiryTimestamp == 0L || pkg.expiryTimestamp > now
        }
        Log.d("EthioStat", "After expiry filter: ${allRaw.size} raw → ${activeRaw.size} active")

        // Step 5: Combine by type
        val packages = mutableListOf<BalancePackage>()
        val internetPackages = activeRaw.filter { it.packageType == PackageType.INTERNET }
        val voicePackages = activeRaw.filter { it.packageType == PackageType.VOICE }
        val otherPackages = activeRaw.filter {
            it.packageType != PackageType.INTERNET && it.packageType != PackageType.VOICE
        }

        if (internetPackages.isNotEmpty()) {
            Log.d("EthioStat", "Combining ${internetPackages.size} internet packages")
            packages.add(combineInternetPackages(internetPackages))
        }
        if (voicePackages.isNotEmpty()) {
            Log.d("EthioStat", "Combining ${voicePackages.size} voice packages")
            packages.add(combineVoicePackages(voicePackages))
        }
        packages.addAll(otherPackages)

        // Step 6: Parse financial transaction (never for 251994)
        val transaction = if (sender.contains("251994", ignoreCase = true) ||
            sender.contains("ethio telecom", ignoreCase = true)
        ) {
            null
        } else {
            parseTransactionForSender(smsBody, sender)
        }

        return if (packages.isNotEmpty() || transaction != null) {
            ParsedSmsData.success(
                packages = packages,
                transaction = transaction,
                language = SmsLanguage.ENGLISH
            )
        } else {
            ParsedSmsData.empty()
        }
    }

    override fun canParse(smsBody: String): Boolean {
        return multiPackagePattern.containsMatchIn(smsBody) ||
                weeklyPackagePattern.containsMatchIn(smsBody) ||
                bonusFundsPattern.containsMatchIn(smsBody) ||
                telecomInternetPattern.containsMatchIn(smsBody) ||
                telecomVoicePattern.containsMatchIn(smsBody) ||
                cbeDebitPattern.containsMatchIn(smsBody) ||
                cbeCreditPattern.containsMatchIn(smsBody) ||
                boaCreditPattern.containsMatchIn(smsBody) ||
                boaDebitPattern.containsMatchIn(smsBody) ||
                (smsBody.contains("remaining amount", ignoreCase = true) &&
                        (smsBody.contains("internet", ignoreCase = true) ||
                                smsBody.contains("voice", ignoreCase = true)))
    }

    // -----------------------------------------------------------------------
    // Primary segment-based parser for 251994 multi-package messages
    // -----------------------------------------------------------------------
    private fun parseMultiPackageRaw(smsBody: String): List<BalancePackage> {
        val packages = mutableListOf<BalancePackage>()
        val segments = smsBody.split(";")

        Log.d("EthioStat", "parseMultiPackageRaw: Processing ${segments.size} segments")

        for (segment in segments) {
            Log.d("EthioStat", "parseMultiPackageRaw: Segment = ${segment.take(100)}")
            val matches = multiPackagePattern.findAll(segment).toList()
            Log.d("EthioStat", "parseMultiPackageRaw: Found ${matches.size} matches in segment")

            matches.forEach { match ->
                val packageDesc = match.groupValues[1]
                val amount = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
                val unit = match.groupValues[3]
                val seconds = match.groupValues[4].replace(",", "").toDoubleOrNull() ?: 0.0
                val expiryDate = match.groupValues[5]
                val expiryTime = match.groupValues[6]

                Log.d("EthioStat", "Matched: $packageDesc, amount=$amount $unit, seconds=$seconds, expiry=$expiryDate $expiryTime")

                val type = when {
                    unit.equals("MB", ignoreCase = true) ||
                            unit.equals("GB", ignoreCase = true) -> PackageType.INTERNET
                    unit.equals("minute", ignoreCase = true) -> PackageType.VOICE
                    else -> PackageType.UNKNOWN
                }

                val packageName = packageNamePattern.find(packageDesc)?.groupValues?.get(1)?.trim()
                    ?: packageDesc.take(50).trim()

                val validityDays = validityPattern.find(packageDesc)?.groupValues?.get(1)?.toIntOrNull() ?: 30

                val amountInMB = if (unit.equals("GB", ignoreCase = true)) {
                    amount * 1024
                } else if (unit.equals("minute", ignoreCase = true)) {
                    amount + (seconds / 60.0)
                } else {
                    amount
                }

                val totalAmount = when (type) {
                    PackageType.INTERNET -> extractTotalFromPackageName(packageName)
                    PackageType.VOICE -> extractVoiceTotal(packageDesc)
                    else -> amountInMB
                }

                val expiryTimestamp = parseExpiryTimestamp("$expiryDate $expiryTime")

                packages.add(
                    BalancePackage(
                        packageType = type,
                        packageName = packageName,
                        totalAmount = totalAmount,
                        remainingAmount = amountInMB,
                        unit = if (type == PackageType.INTERNET) "MB" else "minutes",
                        source = "telebirr",
                        validityDays = validityDays,
                        expiryDate = "$expiryDate at $expiryTime",
                        expiryTimestamp = expiryTimestamp,
                        language = "en"
                    )
                )
            }
        }

        return packages
    }

    private fun parseWeeklyPackage(smsBody: String): BalancePackage? {
        val match = weeklyPackagePattern.find(smsBody) ?: return null

        val packageName = match.groupValues[1]
        val dayData = parseDataAmount(match.groupValues[1])
        val nightBonus = parseDataAmount(match.groupValues[2])
        val freeData = parseDataAmount(match.groupValues[3])
        val expiryDate = match.groupValues[6]
        val expiryTime = match.groupValues[7]

        val totalAmount = dayData + nightBonus + freeData
        val expiryTimestamp = parseExpiryTimestamp("$expiryDate $expiryTime")

        return BalancePackage(
            packageType = PackageType.INTERNET,
            packageName = packageName,
            totalAmount = totalAmount,
            remainingAmount = totalAmount,
            unit = "MB",
            source = "telebirr",
            validityDays = 7,
            expiryDate = "$expiryDate at $expiryTime",
            expiryTimestamp = expiryTimestamp,
            language = "en"
        )
    }

    // -----------------------------------------------------------------------
    // Financial transaction parsing — dispatches by sender
    // -----------------------------------------------------------------------
    private fun parseTransactionForSender(smsBody: String, sender: String): Transaction? {
        return when {
            sender.contains("CBE", ignoreCase = true) -> parseCbeTransaction(smsBody, sender)
            sender.contains("BOA", ignoreCase = true) -> parseBoaTransaction(smsBody, sender)
            sender.contains("AWASH", ignoreCase = true) -> parseAwashTransaction(smsBody, sender)
            sender.contains("127") || sender.contains("830") -> parseTelebirrGenericTransaction(smsBody, sender)
            else -> parseGenericTransaction(smsBody, sender)
        }
    }

    private fun parseCbeTransaction(smsBody: String, sender: String): Transaction? {
        // Try debit (transfer out)
        val debitMatch = cbeDebitPattern.find(smsBody)
        if (debitMatch != null) {
            val amount = debitMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return Transaction(
                amount = amount,
                type = TransactionType.EXPENSE,
                category = "Debit",
                source = sender,
                description = "CBE Account Debited",
                timestamp = System.currentTimeMillis(),
                accountSource = AccountSourceType.BANK_CBE,
                sourcePhoneNumber = sender,
                isClassified = true
            )
        }
        // Try credit (money received)
        val creditMatch = cbeCreditPattern.find(smsBody)
        if (creditMatch != null) {
            val amount = creditMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return Transaction(
                amount = amount,
                type = TransactionType.INCOME,
                category = "Credit",
                source = sender,
                description = "CBE Account Credited",
                timestamp = System.currentTimeMillis(),
                accountSource = AccountSourceType.BANK_CBE,
                sourcePhoneNumber = sender,
                isClassified = true
            )
        }
        return null
    }

    private fun parseBoaTransaction(smsBody: String, sender: String): Transaction? {
        // Try credit first
        val creditMatch = boaCreditPattern.find(smsBody)
        if (creditMatch != null) {
            val amount = creditMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return Transaction(
                amount = amount,
                type = TransactionType.INCOME,
                category = "Credit",
                source = sender,
                description = "BOA Account Credited",
                timestamp = System.currentTimeMillis(),
                accountSource = AccountSourceType.BANK_BOA,
                sourcePhoneNumber = sender,
                isClassified = true
            )
        }
        // Try debit
        val debitMatch = boaDebitPattern.find(smsBody)
        if (debitMatch != null) {
            val amount = debitMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return Transaction(
                amount = amount,
                type = TransactionType.EXPENSE,
                category = "Debit",
                source = sender,
                description = "BOA Account Debited",
                timestamp = System.currentTimeMillis(),
                accountSource = AccountSourceType.BANK_BOA,
                sourcePhoneNumber = sender,
                isClassified = true
            )
        }
        return null
    }

    private fun parseAwashTransaction(smsBody: String, sender: String): Transaction? {
        val creditMatch = awashCreditPattern.find(smsBody)
        if (creditMatch != null) {
            val amount = creditMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return Transaction(
                amount = amount,
                type = TransactionType.INCOME,
                category = "Credit",
                source = sender,
                description = "Awash Account Credited",
                timestamp = System.currentTimeMillis(),
                accountSource = AccountSourceType.BANK_AWASH,
                sourcePhoneNumber = sender,
                isClassified = true
            )
        }
        val debitMatch = awashDebitPattern.find(smsBody)
        if (debitMatch != null) {
            val amount = debitMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return Transaction(
                amount = amount,
                type = TransactionType.EXPENSE,
                category = "Debit",
                source = sender,
                description = "Awash Account Debited",
                timestamp = System.currentTimeMillis(),
                accountSource = AccountSourceType.BANK_AWASH,
                sourcePhoneNumber = sender,
                isClassified = true
            )
        }
        return null
    }

    private fun parseTelebirrGenericTransaction(smsBody: String, sender: String): Transaction? {
        val isIncome = incomeKeywords.any { smsBody.contains(it, ignoreCase = true) }
        val isExpense = expenseKeywords.any { smsBody.contains(it, ignoreCase = true) } || smsBody.contains("paid", ignoreCase = true)

        if (!isIncome && !isExpense) return null

        val amountPattern = Regex("""([\d,]+\.?\d*)\s*(Birr|ETB)""", RegexOption.IGNORE_CASE)
        val amount = amountPattern.find(smsBody)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        return amount?.let {
            Transaction(
                amount = it,
                type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                category = if (isIncome) "TeleBirr Receive" else "TeleBirr Send/Pay",
                source = sender,
                description = "TeleBirr English Transaction",
                timestamp = System.currentTimeMillis(),
                accountSource = AccountSourceType.TELEBIRR,
                sourcePhoneNumber = sender,
                isClassified = true
            )
        }
    }

    private fun parseGenericTransaction(smsBody: String, sender: String): Transaction? {
        val isIncome = incomeKeywords.any { smsBody.contains(it, ignoreCase = true) }
        val isExpense = expenseKeywords.any { smsBody.contains(it, ignoreCase = true) }

        if (!isIncome && !isExpense) return null

        val amountPattern = Regex("""([\d,]+\.?\d*)\s*(Birr|ETB)""", RegexOption.IGNORE_CASE)
        val amount = amountPattern.find(smsBody)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        return amount?.let {
            Transaction(
                amount = it,
                type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                category = if (isIncome) "Credit" else "Package Purchase",
                source = sender,
                description = smsBody.take(100),
                timestamp = System.currentTimeMillis(),
                accountSource = AccountSourceType.BANK_OTHER,
                sourcePhoneNumber = sender,
                isClassified = false
            )
        }
    }

    // -----------------------------------------------------------------------
    // Telecom USSD fallback (used only when primary parser finds nothing)
    // -----------------------------------------------------------------------
    private fun parseTelecomUssdResponse(smsBody: String): List<BalancePackage> {
        val packages = mutableListOf<BalancePackage>()

        telecomInternetPattern.findAll(smsBody).forEach { match ->
            Log.d("EthioStat", "Telecom USSD fallback internet match: ${match.value.take(80)}")
            val amountText = match.groupValues[1].replace(",", "")
            val unit = match.groupValues[2]
            val expiryDate = match.groupValues[3]
            val expiryTime = match.groupValues[4].ifEmpty { "00:00:00" }
            val amountMb = if (unit.equals("GB", ignoreCase = true)) {
                amountText.toDoubleOrNull()?.times(1024) ?: 0.0
            } else {
                amountText.toDoubleOrNull() ?: 0.0
            }
            val expiryTimestamp = parseExpiryTimestamp("$expiryDate $expiryTime")
            packages.add(
                BalancePackage(
                    packageType = PackageType.INTERNET,
                    packageName = "Telecom Internet",
                    totalAmount = amountMb,
                    remainingAmount = amountMb,
                    unit = "MB",
                    source = "Ethio Telecom",
                    validityDays = calculateDaysUntil(expiryTimestamp),
                    expiryDate = if (expiryDate.isNotEmpty()) "$expiryDate at $expiryTime" else "No data",
                    expiryTimestamp = expiryTimestamp,
                    language = "en"
                )
            )
        }

        telecomVoicePattern.findAll(smsBody).forEach { match ->
            Log.d("EthioStat", "Telecom USSD fallback voice match: ${match.value.take(80)}")
            val minutesText = match.groupValues[1].replace(",", "")
            val secondsText = match.groupValues[2].replace(",", "")
            val expiryDate = match.groupValues[3]
            val expiryTime = match.groupValues[4].ifEmpty { "00:00:00" }
            val minutes = minutesText.toDoubleOrNull() ?: 0.0
            val seconds = secondsText.toDoubleOrNull() ?: 0.0
            val totalMinutes = minutes + (seconds / 60.0)
            val expiryTimestamp = parseExpiryTimestamp("$expiryDate $expiryTime")
            packages.add(
                BalancePackage(
                    packageType = PackageType.VOICE,
                    packageName = "Telecom Voice",
                    totalAmount = totalMinutes,
                    remainingAmount = totalMinutes,
                    unit = "minutes",
                    source = "Ethio Telecom",
                    validityDays = calculateDaysUntil(expiryTimestamp),
                    expiryDate = if (expiryDate.isNotEmpty()) "$expiryDate at $expiryTime" else "No data",
                    expiryTimestamp = expiryTimestamp,
                    language = "en"
                )
            )
        }

        return packages
    }

    private fun parseTelecomFallback(smsBody: String, sender: String): List<BalancePackage> {
        val packages = mutableListOf<BalancePackage>()

        val internetFallback = Regex(
            """(?is)remaining\s+amount.*?internet.*?is\s+([\d.,]+)\s*(MB|GB).*?expiry\s+date\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2})""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        val voiceFallback = Regex(
            """(?is)remaining\s+amount.*?voice.*?is\s+([\d.,]+)\s*minute(?:\s+and\s+([\d.,]+)\s*second)?\s+with\s+expiry\s+date\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2})""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        internetFallback.findAll(smsBody).forEach { match ->
            val amountText = match.groupValues[1].replace(",", "")
            val unit = match.groupValues[2]
            val expiryDate = match.groupValues[3]
            val expiryTime = match.groupValues[4]
            val amountMb = if (unit.equals("GB", ignoreCase = true)) {
                amountText.toDoubleOrNull()?.times(1024) ?: 0.0
            } else {
                amountText.toDoubleOrNull() ?: 0.0
            }
            val expiryTimestamp = parseExpiryTimestamp("$expiryDate $expiryTime")
            packages.add(
                BalancePackage(
                    packageType = PackageType.INTERNET,
                    packageName = "Telecom Internet",
                    totalAmount = amountMb,
                    remainingAmount = amountMb,
                    unit = "MB",
                    source = "Ethio Telecom",
                    validityDays = calculateDaysUntil(expiryTimestamp),
                    expiryDate = "$expiryDate at $expiryTime",
                    expiryTimestamp = expiryTimestamp,
                    language = "en"
                )
            )
        }

        voiceFallback.findAll(smsBody).forEach { match ->
            val minutesText = match.groupValues[1].replace(",", "")
            val secondsText = match.groupValues.getOrNull(2)?.replace(",", "")
            val expiryDate = match.groupValues[3]
            val expiryTime = match.groupValues[4]
            val minutes = minutesText.toDoubleOrNull() ?: 0.0
            val seconds = secondsText?.toDoubleOrNull() ?: 0.0
            val totalMinutes = minutes + (seconds / 60.0)
            val expiryTimestamp = parseExpiryTimestamp("$expiryDate $expiryTime")
            packages.add(
                BalancePackage(
                    packageType = PackageType.VOICE,
                    packageName = "Telecom Voice",
                    totalAmount = totalMinutes,
                    remainingAmount = totalMinutes,
                    unit = "minutes",
                    source = "Ethio Telecom",
                    validityDays = calculateDaysUntil(expiryTimestamp),
                    expiryDate = "$expiryDate at $expiryTime",
                    expiryTimestamp = expiryTimestamp,
                    language = "en"
                )
            )
        }

        return packages
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private fun parseDataAmount(text: String): Double {
        val pattern = Regex("""([\d.]+)\s*(MB|GB)""", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return 0.0
        val amount = match.groupValues[1].toDoubleOrNull() ?: 0.0
        val unit = match.groupValues[2]
        return if (unit.equals("GB", ignoreCase = true)) amount * 1024 else amount
    }

    /**
     * FIX: Made IGNORE_CASE and handles space between number and unit (e.g. "4.8 GB").
     */
    private fun extractTotalFromPackageName(packageName: String): Double {
        val pattern = Regex("""([\d.]+)\s*(GB|MB)""", RegexOption.IGNORE_CASE)
        val match = pattern.find(packageName) ?: return 0.0
        val amount = match.groupValues[1].toDoubleOrNull() ?: 0.0
        val unit = match.groupValues[2]
        return if (unit.equals("GB", ignoreCase = true)) amount * 1024 else amount
    }

    private fun extractVoiceTotal(description: String): Double {
        val pattern = Regex("""(\d+)\s*Min""", RegexOption.IGNORE_CASE)
        val match = pattern.find(description) ?: return 0.0
        return match.groupValues[1].toDoubleOrNull() ?: 0.0
    }

    private fun parseExpiryTimestamp(dateTimeString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            format.parse(dateTimeString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)
        }
    }

    private fun calculateDaysUntil(timestamp: Long): Int {
        val diff = timestamp - System.currentTimeMillis()
        return maxOf(0, (diff / (1000 * 60 * 60 * 24)).toInt())
    }

    // -----------------------------------------------------------------------
    // combineVoicePackages — already active-only (expiry filter applied upstream)
    // -----------------------------------------------------------------------
    private fun combineVoicePackages(voicePackages: List<BalancePackage>): BalancePackage {
        if (voicePackages.isEmpty()) return createZeroVoicePackage()

        var totalMinutes = 0.0
        var combinedTotal = 0.0
        var latestExpiry = ""
        var latestTimestamp = 0L
        val packageNames = mutableListOf<String>()

        voicePackages.forEach { pkg ->
            totalMinutes += pkg.remainingAmount
            combinedTotal += pkg.totalAmount

            val simplifiedName = pkg.packageName
                .replace("Monthly voice ", "", ignoreCase = true)
                .replace("Min night package bonus", "Night Bonus", ignoreCase = true)
                .replace(" from telebirr", "", ignoreCase = true)
                .trim()
            if (simplifiedName.isNotEmpty() && !packageNames.contains(simplifiedName)) {
                packageNames.add(simplifiedName)
            }

            if (pkg.expiryTimestamp > latestTimestamp) {
                latestTimestamp = pkg.expiryTimestamp
                latestExpiry = pkg.expiryDate
            }
        }

        val firstPackage = voicePackages.first()
        val combinedName = if (packageNames.size > 1) {
            packageNames.joinToString(" + ")
        } else {
            packageNames.firstOrNull() ?: "Voice Package"
        }

        Log.d("EthioStat", "Combined ${voicePackages.size} voice pkgs: $combinedName, total: $totalMinutes min")

        return BalancePackage(
            packageType = PackageType.VOICE,
            packageName = combinedName,
            totalAmount = combinedTotal,
            remainingAmount = totalMinutes,
            unit = "minutes",
            source = firstPackage.source,
            validityDays = firstPackage.validityDays,
            expiryDate = latestExpiry,
            expiryTimestamp = latestTimestamp,
            language = firstPackage.language
        )
    }

    private fun createZeroVoicePackage() = BalancePackage(
        packageType = PackageType.VOICE,
        packageName = "No Voice Package",
        totalAmount = 0.0,
        remainingAmount = 0.0,
        unit = "minutes",
        source = "telebirr",
        validityDays = 0,
        expiryDate = "No expiry",
        expiryTimestamp = 0L,
        language = "en"
    )

    // -----------------------------------------------------------------------
    // combineInternetPackages — already active-only (expiry filter applied upstream)
    // -----------------------------------------------------------------------
    private fun combineInternetPackages(internetPackages: List<BalancePackage>): BalancePackage {
        if (internetPackages.isEmpty()) return createZeroInternetPackage()

        var totalMB = 0.0
        var combinedTotal = 0.0
        var latestExpiry = ""
        var latestTimestamp = 0L
        val packageNames = mutableListOf<String>()

        internetPackages.forEach { pkg ->
            totalMB += pkg.remainingAmount
            combinedTotal += pkg.totalAmount

            val simplifiedName = pkg.packageName
                .replace("Monthly Internet Package ", "", ignoreCase = true)
                .replace("Monthly Internet package ", "", ignoreCase = true)
                .replace("Weekly Internet Package ", "Weekly ", ignoreCase = true)
                .replace(" from telebirr", "", ignoreCase = true)
                .trim()
            if (simplifiedName.isNotEmpty() && !packageNames.contains(simplifiedName)) {
                packageNames.add(simplifiedName)
            }

            if (pkg.expiryTimestamp > latestTimestamp) {
                latestTimestamp = pkg.expiryTimestamp
                latestExpiry = pkg.expiryDate
            }
        }

        val firstPackage = internetPackages.first()
        val combinedName = if (packageNames.size > 1) {
            packageNames.joinToString(" + ")
        } else {
            packageNames.firstOrNull() ?: "Internet Package"
        }

        Log.d("EthioStat", "Combined ${internetPackages.size} internet pkgs: $combinedName, total: $totalMB MB")

        return BalancePackage(
            packageType = PackageType.INTERNET,
            packageName = combinedName,
            totalAmount = combinedTotal,
            remainingAmount = totalMB,
            unit = "MB",
            source = firstPackage.source,
            validityDays = firstPackage.validityDays,
            expiryDate = latestExpiry,
            expiryTimestamp = latestTimestamp,
            language = firstPackage.language
        )
    }

    private fun createZeroInternetPackage() = BalancePackage(
        packageType = PackageType.INTERNET,
        packageName = "No Internet Package",
        totalAmount = 0.0,
        remainingAmount = 0.0,
        unit = "MB",
        source = "telebirr",
        validityDays = 0,
        expiryDate = "No expiry",
        expiryTimestamp = 0L,
        language = "en"
    )

    private fun parseAccountBalanceBirr(smsBody: String): BalancePackage? {
        val match = accountBalanceBirrPattern.find(smsBody) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

        Log.d("EthioStat", "Parsed account balance: $amount Birr")

        return BalancePackage(
            packageType = PackageType.MAIN_BALANCE,
            packageName = "Account Balance",
            totalAmount = amount,
            remainingAmount = amount,
            unit = "Birr",
            source = "Ethio Telecom",
            validityDays = 365,
            expiryDate = "Check SMS for details",
            expiryTimestamp = System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L),
            language = "en"
        )
    }

    private fun parseAwardedBonus(smsBody: String): BalancePackage? {
        val match = awardedBonusPattern.find(smsBody) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

        Log.d("EthioStat", "Parsed awarded bonus: $amount ETB")

        return BalancePackage(
            packageType = PackageType.BONUS_FUND,
            packageName = "Recharge Bonus",
            totalAmount = amount,
            remainingAmount = amount,
            unit = "Birr",
            source = "Ethio Telecom",
            validityDays = 3,
            expiryDate = "Bonus reward",
            expiryTimestamp = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L),
            language = "en"
        )
    }
}
