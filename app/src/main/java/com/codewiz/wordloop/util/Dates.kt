package com.codewiz.wordloop.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

fun parseIsoInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
}

fun relativeDateLabel(value: String?, locale: Locale = Locale.getDefault()): String {
    val instant = parseIsoInstant(value) ?: return ""
    val zone = ZoneId.systemDefault()
    val date = instant.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val days = ChronoUnit.DAYS.between(date, today)
    return when (days) {
        0L -> "Today"
        1L -> "Yesterday"
        -1L -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
}

fun formattedLongDate(locale: Locale = Locale.getDefault()): String {
    return LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE, MMMM d", locale),
    )
}

fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}
