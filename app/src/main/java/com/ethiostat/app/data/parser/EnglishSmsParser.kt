package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.*
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class EnglishSmsParser : SmsParser {
    
    private val multiPackagePattern = Regex(
        """from\s+([^;]+?)\s+from\s+telebirr\s+to\s+be\s+expired\s+after\s+\d+\s+days\s+(?:and\s+[^;]+?)?\s*is\s+([\d,]+\.?\d*)\s*(MB|GB|minute)(?:\s+and\s+([\d,]+)\s+second)?\s+with\s+expiry\s+date\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2})""",
        setOf(RegexOption.IGNORE_CASE)
    )
    
    private val packageNamePattern = Regex(
        """(Monthly\s+Internet\s+Package\s+[\d.]+\s*(?:GB|MB)|Monthly\s+voice\s+\d+\s*Min|Weekly\s+Youth\s+[\d.]+GB|[\d.]+\s*Min\s+night\s+package\s+bonus)\s+from\s+(telebirr|tele\s+birr)""",
        RegexOption.IGNORE_CASE
    )
    
    private val validityPattern = Regex(
        """to\s+be\s+expired\s+after\s+(\d+)\s+days?""",
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
    
    // Pattern to parse account balance in Birr from first fragment of *804# USSD
    // Matches: "Dear Customer, your remaining amount from ... is 0.01 Birr"
    private val accountBalanceBirrPattern = Regex(
        """(?:remaining\s+amount|account\s+balance).*?is\s+([\d,]+\.?\d*)\s*Birr""",
        RegexOption.IGNORE_CASE
    )
    
    // Telecom USSD SMS patterns (Internet/Voice balances with expiry)
    // Match patterns like:
    // "from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 3775.232 MB with expiry date on 2026-04-06 at 17:38:02"
    // "from Monthly voice 150 Min from telebirr to be expired after 30 days and 76 Min night package bonus valid for 30 days is 111 minute and 50 second with expiry date on 2026-04-10 at 11:08:07"
    // Also handles: "is 0 MB" / "is 0 minute" (zero balance) and optional expiry dates
    private val telecomInternetPattern = Regex(
        """(?i)internet\s+package.*?to\s+be\s+expired\s+after\s+\d+\s*days?\s+is\s+([\d,]+\.?\d*)\s*(MB|GB)(?:.*?expiry\s+date\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2}))?""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )
    private val telecomVoicePattern = Regex(
        """(?i)voice\s+\d+\s*Min.*?to\s+be\s+expired\s+after\s+\d+\s*days?(?:\s+and\s+[^;]+?)?\s+is\s+([\d,]+\.?\d*)\s*minute(?:\s+and\s+([\d,]+)\s*second)?(?:\s+with\s+expiry\s+date\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2}))?""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )
    
    private val incomeKeywords = listOf("received", "credited", "deposited")
    private val expenseKeywords = listOf("Monthly Internet Package", "Voice Min", "from telebirr", "purchased")
    
    override fun parse(smsBody: String, sender: String): ParsedSmsData {
        if (sender.contains("251994", ignoreCase = true)) {
            Log.d("EthioStat", "EnglishSmsParser.parse() - Telecom SMS length: ${smsBody.length}")
            Log.d("EthioStat", "EnglishSmsParser.parse() - Telecom SMS body: ${smsBody.take(500)}")
        }
        val allRaw = mutableListOf<BalancePackage>()
        
        allRaw.addAll(parseMultiPackageRaw(smsBody))
        parseWeeklyPackage(smsBody)?.let { allRaw.add(it) }
        
        // Parse account balance in Birr from first fragment (e.g., "remaining amount is 0.01 Birr")
        parseAccountBalanceBirr(smsBody)?.let { allRaw.add(it) }
        
        val telecomPackages = parseTelecomUssdResponse(smsBody, sender)
        if (telecomPackages.isEmpty()) {
            val fallback = parseTelecomFallback(smsBody, sender)
            allRaw.addAll(fallback)
        } else {
            allRaw.addAll(telecomPackages)
        }
        
        val packages = mutableListOf<BalancePackage>()
        val internetPackages = allRaw.filter { it.packageType == PackageType.INTERNET }
        val voicePackages = allRaw.filter { it.packageType == PackageType.VOICE }
        val otherPackages = allRaw.filter { it.packageType != PackageType.INTERNET && it.packageType != PackageType.VOICE }
        
        if (internetPackages.isNotEmpty()) {
            android.util.Log.d("EthioStat", "Combining ${internetPackages.size} internet packages")
            packages.add(combineInternetPackages(internetPackages))
        }
        if (voicePackages.isNotEmpty()) {
            android.util.Log.d("EthioStat", "Combining ${voicePackages.size} voice packages")
            packages.add(combineVoicePackages(voicePackages))
        }
        packages.addAll(otherPackages)
        
        // CRITICAL: Telecom sender (251994) should NEVER create transactions, only packages
        val transaction = if (sender.contains("251994", ignoreCase = true) || 
                              sender.contains("ethio telecom", ignoreCase = true)) {
            null
        } else {
            parseTransaction(smsBody, sender)
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
                // Fallback acceptance for "remaining amount ... internet/voice" telecom replies
                smsBody.contains("remaining amount", ignoreCase = true) &&
                (smsBody.contains("internet", ignoreCase = true) || smsBody.contains("voice", ignoreCase = true))
    }
    
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
                val seconds = match.groupValues[4].toDoubleOrNull() ?: 0.0
                val expiryDate = match.groupValues[5]
                val expiryTime = match.groupValues[6]
                
                Log.d("EthioStat", "Matched package: $packageDesc, amount: $amount $unit, seconds: $seconds, expiry: $expiryDate $expiryTime")
                
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
                    // For voice: convert minutes + seconds to total minutes
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
                
                val balancePackage = BalancePackage(
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
                
                packages.add(balancePackage)
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
    
    private fun parseTransaction(smsBody: String, sender: String): Transaction? {
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
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Parse telecom balances from USSD-triggered SMS (e.g., *804# responses).
     */
    private fun parseTelecomUssdResponse(smsBody: String, sender: String): List<BalancePackage> {
        val packages = mutableListOf<BalancePackage>()

        telecomInternetPattern.findAll(smsBody).forEach { match ->
            Log.d("EthioStat", "Telecom USSD parse internet match: ${match.value}")
            val amountText = match.groupValues[1].replace(",", "")
            val unit = match.groupValues[2]
            val expiryDate = match.groupValues[3]
            val amountMb = if (unit.equals("GB", ignoreCase = true)) {
                amountText.toDoubleOrNull()?.times(1024) ?: 0.0
            } else {
                amountText.toDoubleOrNull() ?: 0.0
            }
            val expiryTimestamp = parseExpiryTimestamp("$expiryDate 00:00:00")
            packages.add(
                BalancePackage(
                    packageType = PackageType.INTERNET,
                    packageName = "Telecom Internet",
                    totalAmount = amountMb,
                    remainingAmount = amountMb,
                    unit = "MB",
                    source = "Ethio Telecom",
                    validityDays = calculateDaysUntil(expiryTimestamp),
                    expiryDate = expiryDate,
                    expiryTimestamp = expiryTimestamp,
                    language = "en"
                )
            )
        }

        telecomVoicePattern.findAll(smsBody).forEach { match ->
            Log.d("EthioStat", "Telecom USSD parse voice match: ${match.value}")
            val minutesText = match.groupValues[1].replace(",", "")
            val expiryDate = match.groupValues[2]
            val minutes = minutesText.toDoubleOrNull() ?: 0.0
            val expiryTimestamp = parseExpiryTimestamp("$expiryDate 00:00:00")
            packages.add(
                BalancePackage(
                    packageType = PackageType.VOICE,
                    packageName = "Telecom Voice",
                    totalAmount = minutes,
                    remainingAmount = minutes,
                    unit = "minutes",
                    source = "Ethio Telecom",
                    validityDays = calculateDaysUntil(expiryTimestamp),
                    expiryDate = expiryDate,
                    expiryTimestamp = expiryTimestamp,
                    language = "en"
                )
            )
        }

        if (packages.isNotEmpty()) {
            Log.d("EthioStat", "Telecom USSD parsed packages count=${packages.size}")
        }
        return packages
    }

    /**
     * Fallback parser for telecom SMS like the provided sample when primary regex misses.
     */
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
            Log.d("EthioStat", "Telecom fallback internet match: ${match.value}")
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
            Log.d("EthioStat", "Telecom fallback voice match: ${match.value}")
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

        if (packages.isNotEmpty()) {
            Log.d("EthioStat", "Telecom fallback parsed packages count=${packages.size}")
        }
        return packages
    }
    
    private fun parseDataAmount(text: String): Double {
        val pattern = Regex("""([\d.]+)\s*(MB|GB)""", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return 0.0
        
        val amount = match.groupValues[1].toDoubleOrNull() ?: 0.0
        val unit = match.groupValues[2]
        
        return if (unit.equals("GB", ignoreCase = true)) {
            amount * 1024
        } else {
            amount
        }
    }
    
    private fun extractTotalFromPackageName(packageName: String): Double {
        val pattern = Regex("""([\d.]+)\s*(GB|MB)""", RegexOption.IGNORE_CASE)
        val match = pattern.find(packageName) ?: return 0.0
        
        val amount = match.groupValues[1].toDoubleOrNull() ?: 0.0
        val unit = match.groupValues[2]
        
        return if (unit.equals("GB", ignoreCase = true)) {
            amount * 1024
        } else {
            amount
        }
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
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
    
    private fun combineVoicePackages(voicePackages: List<BalancePackage>): BalancePackage {
        if (voicePackages.isEmpty()) {
            return createZeroVoicePackage()
        }
        
        var totalMinutes = 0.0
        var totalSeconds = 0.0
        var combinedTotal = 0.0
        var latestExpiry = ""
        var latestTimestamp = 0L
        val packageNames = mutableListOf<String>()
        
        voicePackages.forEach { pkg ->
            // Parse minutes and seconds from remaining amount
            val remainingText = pkg.remainingAmount.toString()
            val voiceMatch = voiceBalancePattern.find(remainingText)
            
            if (voiceMatch != null) {
                totalMinutes += voiceMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                totalSeconds += voiceMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            } else {
                // If no specific pattern, treat as minutes
                totalMinutes += pkg.remainingAmount
            }
            
            combinedTotal += pkg.totalAmount
            
            // Collect package names for combined display
            val simplifiedName = pkg.packageName
                .replace("Monthly voice ", "")
                .replace("Min night package bonus", "Night Bonus")
                .replace(" from telebirr", "")
                .trim()
            if (simplifiedName.isNotEmpty() && !packageNames.contains(simplifiedName)) {
                packageNames.add(simplifiedName)
            }
            
            // Use the latest expiry date
            if (pkg.expiryTimestamp > latestTimestamp) {
                latestTimestamp = pkg.expiryTimestamp
                latestExpiry = pkg.expiryDate
            }
        }
        
        // Convert excess seconds to minutes
        totalMinutes += totalSeconds / 60
        val finalSeconds = (totalSeconds % 60).toInt()
        val finalMinutes = totalMinutes.toInt()
        
        val firstPackage = voicePackages.first()
        val combinedName = if (packageNames.size > 1) {
            packageNames.joinToString(" + ")
        } else {
            packageNames.firstOrNull() ?: "Voice Package"
        }
        
        Log.d("EthioStat", "Combined ${voicePackages.size} voice packages: $combinedName, total: $totalMinutes min")
        
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
    
    private fun createZeroVoicePackage(): BalancePackage {
        return BalancePackage(
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
    }
    
    private fun combineInternetPackages(internetPackages: List<BalancePackage>): BalancePackage {
        if (internetPackages.isEmpty()) {
            return createZeroInternetPackage()
        }
        
        var totalMB = 0.0
        var combinedTotal = 0.0
        var latestExpiry = ""
        var latestTimestamp = 0L
        val packageNames = mutableListOf<String>()
        
        internetPackages.forEach { pkg ->
            totalMB += pkg.remainingAmount
            combinedTotal += pkg.totalAmount
            
            // Collect package names for combined display
            val simplifiedName = pkg.packageName
                .replace("Monthly Internet Package ", "")
                .replace("Weekly Internet Package ", "Weekly ")
                .replace(" from telebirr", "")
                .trim()
            if (simplifiedName.isNotEmpty() && !packageNames.contains(simplifiedName)) {
                packageNames.add(simplifiedName)
            }
            
            // Use the latest expiry date
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
        
        Log.d("EthioStat", "Combined ${internetPackages.size} internet packages: $combinedName, total: $totalMB MB")
        
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
    
    private fun createZeroInternetPackage(): BalancePackage {
        return BalancePackage(
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
    }
    
    private fun parseAccountBalanceBirr(smsBody: String): BalancePackage? {
        val match = accountBalanceBirrPattern.find(smsBody) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        
        Log.d("EthioStat", "Parsed account balance: $amount Birr")
        
        return BalancePackage(
            packageType = PackageType.BONUS_FUND,
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
}
