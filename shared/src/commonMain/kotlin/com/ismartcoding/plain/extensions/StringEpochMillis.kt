package com.ismartcoding.plain.extensions

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant

fun String.parseEpochMillis(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Long? {
    val s = trim()
    if (s.isEmpty()) return null

    val digitsOnly = s.all { it.isDigit() }
    if (digitsOnly) {
        val n = s.toLongOrNull() ?: return null
        // Heuristic: 13+ digits => ms, else seconds.
        return if (s.length >= 13) n else n * 1000L
    }

    if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(s)) {
        val d = runCatching { LocalDate.parse(s) }.getOrNull() ?: return null
        return d.atStartOfDayIn(timeZone).toEpochMilliseconds()
    }

    // Local datetime without timezone, assume local TZ.
    if (Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?$").matches(s)) {
        val dt = runCatching { LocalDateTime.parse(s) }.getOrNull() ?: return null
        return dt.toInstant(timeZone).toEpochMilliseconds()
    }

    return null
}
