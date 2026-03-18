package com.ethiostat.app.domain.model

enum class ThemeMode(val key: String) {
    DARK("DARK"),
    LIGHT("LIGHT"),
    SYSTEM("SYSTEM");

    companion object {
        fun fromKey(key: String): ThemeMode =
            values().find { it.key == key } ?: SYSTEM
    }
}
