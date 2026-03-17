package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

class EnglishSmsParser : SmsParser {
    
    private val multiPackagePattern = Regex(
        """from\s+([^;]+?)\s+is\s+([\d,]+\.?\d*)\s*(MB|GB|minute|second)\s+with\s+expiry\s+date\s+on\s+(\d{4}-\d{2}-\d{2})\s+at\s+(\d{2}:\d{2}:\d{2})""",
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
        val packages = mutableListOf<BalancePackage>()
        
        packages.addAll(parseMultiPackage(smsBody))
        
        parseWeeklyPackage(smsBody)?.let { packages.add(it) }
        
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
    
    private fun parseMultiPackage(smsBody: String): List<BalancePackage> {
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
}
