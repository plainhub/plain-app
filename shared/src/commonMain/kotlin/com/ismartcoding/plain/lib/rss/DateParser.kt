package com.ismartcoding.plain.lib.rss

/**
 * Pure-Kotlin date parser for RSS/Atom date formats.
 * Supports RFC822, W3C date-time, and common additional formats.
 * Returns epoch milliseconds (Long?) instead of java.util.Date.
 */
object DateParser {

    private val MONTHS = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
        "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
        "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
    )

    private val TIMEZONES = mapOf(
        "ut" to 0, "gmt" to 0, "z" to 0, "utc" to 0,
        "est" to -5, "edt" to -4,
        "cst" to -6, "cdt" to -5,
        "mst" to -7, "mdt" to -6,
        "pst" to -8, "pdt" to -7,
    )

    /**
     * Parses a date string in W3C date-time or RFC822 format.
     * @return epoch milliseconds, or null if parsing fails
     */
    fun parseDate(sDate: String): Long? {
        // Try additional masks first
        parseAdditional(sDate)?.let { return it }

        // Try W3C date-time
        parseW3CDateTime(sDate)?.let { return it }

        // Try RFC822
        parseRFC822(sDate)?.let { return it }

        return null
    }

    private fun parseAdditional(input: String): Long? {
        val trimmed = input.trim()
        // "yyyy-MM-dd HH:mm:ss Z" or "yyyy-MM-dd HH:mm:ss"
        val match = Regex("""^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})(?:\s*([+-]\d{4}|[A-Za-z]+))?$""").find(trimmed)
        if (match != null) {
            val (year, month, day, hour, minute, second, tz) = match.destructured
            val tzOffset = parseTimezoneOffset(tz)
            return toEpochMillis(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt(), second.toInt(), tzOffset)
        }
        return null
    }

    fun parseW3CDateTime(input: String): Long? {
        var sDate = input.trim()
        val tIndex = sDate.indexOf("T", ignoreCase = true)
        if (tIndex > -1) {
            if (sDate.endsWith("Z", ignoreCase = true)) {
                sDate = sDate.substring(0, sDate.length - 1) + "+00:00"
            }
            // Find timezone displacement after T
            var tzdIndex = sDate.indexOf("+", tIndex)
            if (tzdIndex == -1) {
                tzdIndex = sDate.indexOf("-", tIndex)
            }
            if (tzdIndex > -1) {
                var pre = sDate.substring(0, tzdIndex)
                val secFraction = pre.indexOf(",")
                if (secFraction > -1) {
                    pre = pre.substring(0, secFraction)
                }
                val post = sDate.substring(tzdIndex)
                sDate = pre + post
            }
        } else {
            sDate += "T00:00+00:00"
        }

        // Patterns: yyyy-MM-dd'T'HH:mm:ss(.SSS)?(Z|+hh:mm)?
        // Pattern with milliseconds
        val matchWithMillis = Regex("""^(\d{4})-(\d{2})-(\d{2})[Tt](\d{2}):(\d{2}):(\d{2})\.(\d{3})([+-]\d{2}:?\d{2})?$""").find(sDate)
        if (matchWithMillis != null) {
            val g = matchWithMillis.groupValues
            return toEpochMillis(g[1].toInt(), g[2].toInt(), g[3].toInt(), g[4].toInt(), g[5].toInt(), g[6].toInt(), parseNumericTimezone(g[8].ifEmpty { "+00:00" }))
        }

        // Pattern with seconds
        val matchWithSeconds = Regex("""^(\d{4})-(\d{2})-(\d{2})[Tt](\d{2}):(\d{2}):(\d{2})([+-]\d{2}:?\d{2})?$""").find(sDate)
        if (matchWithSeconds != null) {
            val g = matchWithSeconds.groupValues
            return toEpochMillis(g[1].toInt(), g[2].toInt(), g[3].toInt(), g[4].toInt(), g[5].toInt(), g[6].toInt(), parseNumericTimezone(g[7].ifEmpty { "+00:00" }))
        }

        // Pattern without seconds
        val matchNoSeconds = Regex("""^(\d{4})-(\d{2})-(\d{2})[Tt](\d{2}):(\d{2})([+-]\d{2}:?\d{2})?$""").find(sDate)
        if (matchNoSeconds != null) {
            val g = matchNoSeconds.groupValues
            return toEpochMillis(g[1].toInt(), g[2].toInt(), g[3].toInt(), g[4].toInt(), g[5].toInt(), 0, parseNumericTimezone(g[6].ifEmpty { "+00:00" }))
        }

        // yyyy-MM-dd, yyyy-MM, yyyy
        val dateOnly = Regex("""^(\d{4})(?:-(\d{2}))?(?:-(\d{2}))?$""").find(sDate)
        if (dateOnly != null) {
            val year = dateOnly.groupValues[1].toInt()
            val month = if (dateOnly.groupValues[2].isNotEmpty()) dateOnly.groupValues[2].toInt() else 1
            val day = if (dateOnly.groupValues[3].isNotEmpty()) dateOnly.groupValues[3].toInt() else 1
            return toEpochMillis(year, month, day, 0, 0, 0, 0)
        }

        return null
    }

    fun parseRFC822(input: String): Long? {
        val converted = convertUnsupportedTimeZones(input.trim())
        // Patterns: "EEE, dd MMM yy HH:mm:ss z" etc.
        // Also "dd MMM yy HH:mm:ss z" without day of week
        val match = Regex("""^(?:[A-Za-z]{3},\s*)?(\d{1,2})\s+([A-Za-z]{3})\s+(\d{2,4})\s+(\d{2}):(\d{2})(?::(\d{2}))?\s*([A-Za-z]+|[+-]\d{4})?$""").find(converted)
        if (match != null) {
            val day = match.groupValues[1].toInt()
            val month = MONTHS[match.groupValues[2].lowercase()] ?: return null
            val yearStr = match.groupValues[3]
            var year = yearStr.toInt()
            if (year < 100) {
                year += if (year < 50) 2000 else 1900
            }
            val hour = match.groupValues[4].toInt()
            val minute = match.groupValues[5].toInt()
            val second = if (match.groupValues[6].isNotEmpty()) match.groupValues[6].toInt() else 0
            val tzStr = if (match.groupValues[7].isNotEmpty()) match.groupValues[7] else "GMT"
            val tzOffset = parseTimezoneOffset(tzStr)
            return toEpochMillis(year, month, day, hour, minute, second, tzOffset)
        }
        return null
    }

    private fun convertUnsupportedTimeZones(input: String): String {
        val parts = input.split(" ")
        for (tz in listOf("UT", "Z")) {
            if (parts.contains(tz)) {
                val lastIdx = input.lastIndexOf(tz)
                return input.substring(0, lastIdx) + "UTC" + input.substring(lastIdx + tz.length)
            }
        }
        return input
    }

    private fun parseTimezoneOffset(tz: String?): Int {
        if (tz.isNullOrBlank()) return 0
        val lower = tz.lowercase()
        // Numeric timezone: +HHMM or -HHMM
        if (lower.matches(Regex("[+-]\\d{4}"))) {
            return parseNumericTimezone(lower)
        }
        // Named timezone
        TIMEZONES[lower]?.let { return it * 3600 }
        return 0
    }

    private fun parseNumericTimezone(tz: String): Int {
        // +HHMM, -HHMM, +HH:MM, -HH:MM
        val cleaned = tz.replace(":", "")
        if (cleaned.length < 5) return 0
        val sign = if (cleaned[0] == '-') -1 else 1
        val hours = cleaned.substring(1, 3).toIntOrNull() ?: return 0
        val minutes = cleaned.substring(3, 5).toIntOrNull() ?: return 0
        return sign * (hours * 3600 + minutes * 60)
    }

    /**
     * Converts date/time components to epoch milliseconds.
     * Uses the proleptic Gregorian calendar algorithm.
     */
    private fun toEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, tzOffsetSeconds: Int): Long {
        // Days from 1970-01-01 to year-month-day (proleptic Gregorian)
        val days = daysFromEpoch(year, month, day)
        val totalSeconds = (days * 24L * 3600) + (hour * 3600L) + (minute * 60L) + second - tzOffsetSeconds
        return totalSeconds * 1000
    }

    private fun daysFromEpoch(year: Int, month: Int, day: Int): Long {
        // Howard Hinnant's algorithm
        val y = if (month <= 2) year - 1 else year
        val era = if (y >= 0) y / 400 else (y - 399) / 400
        val yoe = y - era * 400
        val m = month
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468
    }
}
