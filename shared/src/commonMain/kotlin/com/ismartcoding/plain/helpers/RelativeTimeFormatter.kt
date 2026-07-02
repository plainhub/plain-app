package com.ismartcoding.plain.helpers

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

object RelativeTimeFormatter {

    enum class Style { SHORT, LONG }

    private const val MIN   = 60_000L
    private const val HOUR  = 60 * MIN
    private const val DAY   = 24 * HOUR
    private const val WEEK  = 7 * DAY
    private const val MONTH = 30 * DAY
    private const val YEAR  = 365 * DAY

    data class Formatted(val resource: StringResource, val arg: Long?, val style: Style)

    fun select(timestamp: Long, now: Long, style: Style): Formatted {
        val diff = now - timestamp
        fun s(short: StringResource, long: StringResource) =
            if (style == Style.SHORT) short else long
        return when {
            diff < MIN      -> Formatted(Res.string.relative_time_now, null, style)
            diff < HOUR     -> Formatted(s(Res.string.relative_time_minutes_short, Res.string.relative_time_minutes_long), (diff / MIN).coerceAtLeast(1), style)
            diff < DAY      -> Formatted(s(Res.string.relative_time_hours_short, Res.string.relative_time_hours_long), (diff / HOUR).coerceAtLeast(1), style)
            diff < WEEK     -> Formatted(s(Res.string.relative_time_days_short, Res.string.relative_time_days_long), (diff / DAY).coerceAtLeast(1), style)
            diff < 4 * WEEK -> Formatted(s(Res.string.relative_time_weeks_short, Res.string.relative_time_weeks_long), (diff / WEEK).coerceAtLeast(1), style)
            diff < YEAR     -> Formatted(s(Res.string.relative_time_months_short, Res.string.relative_time_months_long), (diff / MONTH).coerceAtLeast(1), style)
            else            -> Formatted(s(Res.string.relative_time_years_short, Res.string.relative_time_years_long), (diff / YEAR).coerceAtLeast(1), style)
        }
    }

    @Composable
    fun format(
        timestamp: Long,
        now: Long = TimeHelper.nowMillis(),
        style: Style = Style.SHORT,
    ): String {
        val f = select(timestamp, now, style)
        return when {
            f.arg == null      -> stringResource(f.resource)
            style == Style.SHORT -> "${f.arg}${stringResource(f.resource)}"
            else               -> stringResource(f.resource, f.arg)
        }
    }
}
