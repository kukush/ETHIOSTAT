package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.SmsLanguage

class SmsLanguageDetector {
    
    fun detectLanguage(text: String): SmsLanguage {
        val amharicChars = text.count { it in '\u1200'..'\u137F' }
        val latinChars = text.count { it in 'A'..'Z' || it in 'a'..'z' }
        val totalChars = text.length
        
        // Check for strong English indicators (telecom USSD patterns)
        val englishTelecomPatterns = listOf(
            "Dear Customer", "remaining amount", "expiry date", "Monthly Internet Package",
            "Monthly voice", "with expiry date on", "to be expired after"
        )
        val hasEnglishTelecomPattern = englishTelecomPatterns.any { text.contains(it, ignoreCase = true) }
        
        // Check for Oromo keywords (excluding 'telebirr' and 'birr' which appear in English SMS)
        val oromoKeywords = listOf(
            "hanqina", "balansi", "argatte", "fudhatte", "ergite", "kaffale",
            "dabarsuu", "kaffaltii", "kuusuu", "baasuu",
            "herrega", "akawuntii", "daldalaa", "afaan oromoo"
        )
        val oromoKeywordCount = oromoKeywords.count { text.lowercase().contains(it) }
        
        return when {
            hasEnglishTelecomPattern -> SmsLanguage.ENGLISH  // Prioritize English telecom patterns
            oromoKeywordCount >= 2 -> SmsLanguage.OROMO
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
