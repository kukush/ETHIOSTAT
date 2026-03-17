package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

class AmharicSmsParser : SmsParser {
    
    private val transactionPattern = Regex("""በቴሌብር\s+ስላደረጉት\s+የገንዘብ\s+ዝውውር""")
    
    private val ussdCodePattern = Regex("""ወደ\s+(\*\d+\*\d+#)\s+በመደወል""")
    
    private val prizePattern = Regex("""([\d,]+)\s+ብር""")
    
    private val expiryDatePattern = Regex("""በ(\d{2})/(\d{2})/(\d{4})\s*ነው""")
    
    private val teleCoinPattern = Regex("""የቴሌብር\s+ግብይቶ\s+(\d+)\s+ነፃ\s+የቴሌኮይን\s+አስገኝቶሎታል""")
    
    private val serviceExpiryPattern = Regex("""የአገልግሎት\s+ማብቅያ\s+ጊዜ\s+በ(\d{2}/\d{2}/\d{4})\s+ነው""")
    
    override fun parse(smsBody: String, sender: String): ParsedSmsData {
        val transaction = parseTransaction(smsBody, sender)
        val packages = parseTeleCoinReward(smsBody)
        
        return if (transaction != null || packages.isNotEmpty()) {
            ParsedSmsData.success(
                packages = packages,
                transaction = transaction,
                language = SmsLanguage.AMHARIC
            )
        } else {
            ParsedSmsData.empty()
        }
    }
    
    override fun canParse(smsBody: String): Boolean {
        return transactionPattern.containsMatchIn(smsBody) ||
                teleCoinPattern.containsMatchIn(smsBody)
    }
    
    private fun parseTransaction(smsBody: String, sender: String): Transaction? {
        if (!transactionPattern.containsMatchIn(smsBody)) {
            return null
        }
        
        val ussdCode = ussdCodePattern.find(smsBody)?.groupValues?.get(1)
        
        val prizes = prizePattern.findAll(smsBody)
            .map { it.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0 }
            .toList()
        
        val totalPrize = prizes.firstOrNull() ?: 0.0
        
        return Transaction(
            amount = totalPrize,
            type = TransactionType.INCOME,
            category = "telebirr Transaction",
            source = sender,
            description = "የቴሌብር የገንዘብ ዝውውር - $ussdCode",
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun parseTeleCoinReward(smsBody: String): List<BalancePackage> {
        val teleCoinMatch = teleCoinPattern.find(smsBody) ?: return emptyList()
        val expiryMatch = serviceExpiryPattern.find(smsBody)
        
        val coins = teleCoinMatch.groupValues[1].toIntOrNull() ?: 0
        val expiryDate = expiryMatch?.groupValues?.get(1)?.let { 
            convertAmharicDate(it) 
        } ?: ""
        
        val expiryTimestamp = parseExpiryTimestamp(expiryDate)
        
        return if (coins > 0) {
            listOf(
                BalancePackage(
                    packageType = PackageType.BONUS_FUND,
                    packageName = "TeleCoin Reward",
                    totalAmount = coins.toDouble(),
                    remainingAmount = coins.toDouble(),
                    unit = "Coins",
                    source = "telebirr",
                    validityDays = calculateDaysUntil(expiryTimestamp),
                    expiryDate = expiryDate,
                    expiryTimestamp = expiryTimestamp,
                    language = "am"
                )
            )
        } else {
            emptyList()
        }
    }
    
    private fun convertAmharicDate(amharicDate: String): String {
        val parts = amharicDate.split("/")
        if (parts.size != 3) return ""
        
        return "${parts[2]}-${parts[1]}-${parts[0]}"
    }
    
    private fun parseExpiryTimestamp(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)
        }
    }
    
    private fun calculateDaysUntil(timestamp: Long): Int {
        val diff = timestamp - System.currentTimeMillis()
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
}
