package com.ethiostat.app.data.parser

import com.ethiostat.app.domain.model.ParsedSmsData

interface SmsParser {
    fun parse(smsBody: String, sender: String): ParsedSmsData
    fun canParse(smsBody: String): Boolean
}
