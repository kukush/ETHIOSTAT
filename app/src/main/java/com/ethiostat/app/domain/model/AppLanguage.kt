package com.ethiostat.app.domain.model

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    AMHARIC("am", "አማርኛ"),
    OROMIFFA("om", "Afaan Oromoo"),
    TIGRINYA("ti", "ትግርኛ");
    
    companion object {
        fun fromCode(code: String): AppLanguage {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}
