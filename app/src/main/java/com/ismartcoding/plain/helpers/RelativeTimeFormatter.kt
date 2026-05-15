package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R

/**
 * Modern, cross-platform relative time formatter.
 *
 * RelativeTimeFormatter.format(timestamp, now, style)
 *
 * All display strings live in strings_relative_time.xml — no hardcoded text.
 * Android's resource system automatically selects the correct locale.
 *
 * Supported locales with dedicated translations: en (default), zh-CN, zh-TW, ja.
 * All other locales fall back to the English short format (5m / 2h / 1d / …).
 */
object RelativeTimeFormatter {

    enum class Style { SHORT, LONG }

    private const val MIN   = 60_000L
    private const val HOUR  = 60 * MIN
    private const val DAY   = 24 * HOUR
    private const val WEEK  = 7 * DAY
    private const val MONTH = 30 * DAY
    private const val YEAR  = 365 * DAY

    fun format(
        timestamp: Long,
        now: Long = System.currentTimeMillis(),
        style: Style = Style.SHORT,
    ): String {
        val diff = now - timestamp
        return when {
            diff < MIN      -> str(R.string.relative_time_now)
            diff < HOUR     -> fmt((diff / MIN).coerceAtLeast(1), style, R.string.relative_time_minutes_short, R.string.relative_time_minutes_long)
            diff < DAY      -> fmt((diff / HOUR).coerceAtLeast(1), style, R.string.relative_time_hours_short, R.string.relative_time_hours_long)
            diff < WEEK     -> fmt((diff / DAY).coerceAtLeast(1), style, R.string.relative_time_days_short, R.string.relative_time_days_long)
            diff < 4 * WEEK -> fmt((diff / WEEK).coerceAtLeast(1), style, R.string.relative_time_weeks_short, R.string.relative_time_weeks_long)
            diff < YEAR     -> fmt((diff / MONTH).coerceAtLeast(1), style, R.string.relative_time_months_short, R.string.relative_time_months_long)
            else            -> fmt((diff / YEAR).coerceAtLeast(1), style, R.string.relative_time_years_short, R.string.relative_time_years_long)
        }
    }

    private fun str(resId: Int) = MainApp.instance.getString(resId)

    private fun fmt(n: Long, style: Style, shortRes: Int, longRes: Int): String =
        MainApp.instance.getString(if (style == Style.SHORT) shortRes else longRes, n)
}
