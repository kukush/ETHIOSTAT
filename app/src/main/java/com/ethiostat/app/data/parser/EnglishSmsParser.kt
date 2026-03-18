package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

class EnglishSmsParser : SmsParser {
    
    private val multiPackagePattern = Regex(
        """from\s+([^;]+?)\s+is\s+([\d,]+\.?\d*)\s*(MB|GB|minute|second)(?:\s+and\s+\d+\s+second)?\s+with\s+expiry\s+date\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2})""",
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
    
    private val incomeKeywords = listOf("received", "credited", "deposited")
    private val expenseKeywords = listOf("Monthly Internet Package", "Voice Min", "from telebirr", "purchased")
    
    override fun parse(smsBody: String, sender: String): ParsedSmsData {
        val allRaw = mutableListOf<BalancePackage>()
        
        allRaw.addAll(parseMultiPackageRaw(smsBody))
        parseWeeklyPackage(smsBody)?.let { allRaw.add(it) }
        
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
        
        val transaction = parseTransaction(smsBody, sender)
        
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
                bonusFundsPattern.containsMatchIn(smsBody)
    }
    
    private fun parseMultiPackageRaw(smsBody: String): List<BalancePackage> {
        val packages = mutableListOf<BalancePackage>()
        val segments = smsBody.split(";")
        
        for (segment in segments) {
            multiPackagePattern.findAll(segment).forEach { match ->
                val packageDesc = match.groupValues[1]
                val amount = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
                val unit = match.groupValues[3]
                val expiryDate = match.groupValues[4]
                val expiryTime = match.groupValues[5]
                
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
                    amount
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
    
    private fun combineVoicePackages(voicePackages: List<BalancePackage>): BalancePackage {
        if (voicePackages.isEmpty()) {
            return createZeroVoicePackage()
        }
        
        var totalMinutes = 0.0
        var totalSeconds = 0.0
        var combinedTotal = 0.0
        var latestExpiry = ""
        var latestTimestamp = 0L
        
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
        
        return BalancePackage(
            packageType = PackageType.VOICE,
            packageName = "Combined Voice Packages",
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
        
        internetPackages.forEach { pkg ->
            totalMB += pkg.remainingAmount
            combinedTotal += pkg.totalAmount
            
            // Use the latest expiry date
            if (pkg.expiryTimestamp > latestTimestamp) {
                latestTimestamp = pkg.expiryTimestamp
                latestExpiry = pkg.expiryDate
            }
        }
        
        val firstPackage = internetPackages.first()
        
        return BalancePackage(
            packageType = PackageType.INTERNET,
            packageName = "Combined Internet Packages",
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
}
