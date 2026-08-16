package com.codewiz.wordloop.util

import java.util.Locale

enum class AppUiLanguage(
    val code: String,
    val endonym: String,
    val flag: String,
) {
    ENGLISH("en", "English", "🇺🇸"),
    GERMAN("de", "Deutsch", "🇩🇪"),
    SPANISH("es", "Español", "🇪🇸"),
    PORTUGUESE_BRAZIL("pt-BR", "Português (Brasil)", "🇧🇷"),
    FRENCH("fr", "Français", "🇫🇷"),
    NORWEGIAN("nb", "Norsk", "🇳🇴"),
    DUTCH("nl", "Nederlands", "🇳🇱"),
    ITALIAN("it", "Italiano", "🇮🇹"),
    RUSSIAN("ru", "Русский", "🇷🇺"),
    POLISH("pl", "Polski", "🇵🇱"),
    TURKISH("tr", "Türkçe", "🇹🇷"),
    ;

    val pickerLabel: String get() = "$flag $endonym"

    val locale: Locale
        get() = if (code.contains("-")) {
            val parts = code.split("-")
            Locale(parts[0], parts[1])
        } else {
            Locale(code)
        }

    companion object {
        fun from(code: String?): AppUiLanguage? {
            if (code.isNullOrBlank()) return null
            val trimmed = code.trim()
            entries.firstOrNull { it.code.equals(trimmed, ignoreCase = true) }?.let { return it }
            val base = trimmed.lowercase().substringBefore("-")
            return entries.firstOrNull { it.code.lowercase().substringBefore("-") == base }
        }

        fun fromSystemPreferred(): AppUiLanguage {
            val preferred = Locale.getDefault().toLanguageTag()
            return from(preferred) ?: ENGLISH
        }
    }
}
