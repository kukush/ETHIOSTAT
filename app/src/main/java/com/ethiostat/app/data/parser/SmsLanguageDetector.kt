package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.SmsLanguage

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
