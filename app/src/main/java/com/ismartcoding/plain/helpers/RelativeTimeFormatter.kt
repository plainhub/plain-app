package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.StringResource

/**
 * Modern, cross-platform relative time formatter.
 *
 * RelativeTimeFormatter.format(timestamp, now, style)
 *
 * All display strings live in strings_relative_time.xml — no hardcoded text.
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
            diff < MIN      -> str(Res.string.relative_time_now)
            diff < HOUR     -> fmt((diff / MIN).coerceAtLeast(1), style, Res.string.relative_time_minutes_short, Res.string.relative_time_minutes_long)
            diff < DAY      -> fmt((diff / HOUR).coerceAtLeast(1), style, Res.string.relative_time_hours_short, Res.string.relative_time_hours_long)
            diff < WEEK     -> fmt((diff / DAY).coerceAtLeast(1), style, Res.string.relative_time_days_short, Res.string.relative_time_days_long)
            diff < 4 * WEEK -> fmt((diff / WEEK).coerceAtLeast(1), style, Res.string.relative_time_weeks_short, Res.string.relative_time_weeks_long)
            diff < YEAR     -> fmt((diff / MONTH).coerceAtLeast(1), style, Res.string.relative_time_months_short, Res.string.relative_time_months_long)
            else            -> fmt((diff / YEAR).coerceAtLeast(1), style, Res.string.relative_time_years_short, Res.string.relative_time_years_long)
        }
    }

    private fun str(resource: StringResource) = LocaleHelper.getStringSync(resource)

    private fun fmt(n: Long, style: Style, shortRes: StringResource, longRes: StringResource): String =
        LocaleHelper.getStringSync(if (style == Style.SHORT) shortRes else longRes).format(n)
}
