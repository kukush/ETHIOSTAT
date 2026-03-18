package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.ParsedSmsData
import com.ethiostat.app.domain.model.SmsLanguage

class MultilingualSmsParser(
    private val languageDetector: SmsLanguageDetector,
    private val englishParser: EnglishSmsParser,
    private val amharicParser: AmharicSmsParser,
    private val oromoParser: OromoSmsParser
) : SmsParser {
    
    override fun parse(smsBody: String, sender: String): ParsedSmsData {
        val language = languageDetector.detectLanguage(smsBody)
        
        android.util.Log.d("EthioStat", "MultilingualSmsParser: sender=$sender, length=${smsBody.length}, language=$language")
        
        return when (language) {
            SmsLanguage.ENGLISH -> englishParser.parse(smsBody, sender)
            SmsLanguage.AMHARIC -> amharicParser.parse(smsBody, sender)
            SmsLanguage.OROMO -> oromoParser.parse(smsBody, sender)
            SmsLanguage.MIXED -> parseMixed(smsBody, sender)
            SmsLanguage.UNKNOWN -> ParsedSmsData.empty()
        }
    }
    
    override fun canParse(smsBody: String): Boolean {
        return englishParser.canParse(smsBody) || 
               amharicParser.canParse(smsBody) || 
               oromoParser.canParse(smsBody)
    }
    
    private fun parseMixed(smsBody: String, sender: String): ParsedSmsData {
        val englishResult = if (languageDetector.hasEnglishCharacters(smsBody)) {
            englishParser.parse(smsBody, sender)
        } else {
            ParsedSmsData.empty()
        }
        
        val amharicResult = if (languageDetector.hasAmharicCharacters(smsBody)) {
            amharicParser.parse(smsBody, sender)
        } else {
            ParsedSmsData.empty()
        }
        
        return englishResult.merge(amharicResult).copy(language = SmsLanguage.MIXED)
    }
}
