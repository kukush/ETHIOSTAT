package com.ethiostat.app.domain.model

data class ParsedSmsData(
    val packages: List<BalancePackage> = emptyList(),
    val transaction: Transaction? = null,
    val language: SmsLanguage = SmsLanguage.UNKNOWN,
    val isParsed: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun empty() = ParsedSmsData(
            packages = emptyList(),
            transaction = null,
            language = SmsLanguage.UNKNOWN,
            isParsed = false
        )
        
        fun success(
            packages: List<BalancePackage> = emptyList(),
            transaction: Transaction? = null,
            language: SmsLanguage
        ) = ParsedSmsData(
            packages = packages,
            transaction = transaction,
            language = language,
            isParsed = true
        )
        
        fun error(message: String, language: SmsLanguage = SmsLanguage.UNKNOWN) = ParsedSmsData(
            errorMessage = message,
            language = language,
            isParsed = false
        )
    }
    
    fun merge(other: ParsedSmsData): ParsedSmsData {
        return ParsedSmsData(
            packages = this.packages + other.packages,
            transaction = this.transaction ?: other.transaction,
            language = if (this.language != SmsLanguage.UNKNOWN) this.language else other.language,
            isParsed = this.isParsed || other.isParsed,
            errorMessage = this.errorMessage ?: other.errorMessage
        )
    }
}
