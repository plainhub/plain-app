@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.helpers.RelativeTimeFormatter.Style
import org.jetbrains.compose.resources.StringResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RelativeTimeFormatterTest {

    private val now = 1_700_000_000_000L

    private fun res(name: String) =
        StringResource("string:$name", name, emptySet())

    @Test fun `current timestamp maps to now`() {
        val f = RelativeTimeFormatter.select(now, now, Style.SHORT)
        assertEquals(res("relative_time_now"), f.resource)
        assertNull(f.arg)
    }

    @Test fun `30 seconds ago still maps to now`() {
        val f = RelativeTimeFormatter.select(now - 30_000L, now, Style.SHORT)
        assertEquals(res("relative_time_now"), f.resource)
    }

    @Test fun `5 minutes ago renders minutes short with arg 5`() {
        val f = RelativeTimeFormatter.select(now - 5 * MIN, now, Style.SHORT)
        assertEquals(res("relative_time_minutes_short"), f.resource)
        assertEquals(5L, f.arg)
    }

    @Test fun `1 second past minute boundary floors to 1 not 0`() {
        val f = RelativeTimeFormatter.select(now - (MIN + 1), now, Style.SHORT)
        assertEquals(res("relative_time_minutes_short"), f.resource)
        assertEquals(1L, f.arg)
    }

    @Test fun `just under 1 hour stays in minutes bucket`() {
        val f = RelativeTimeFormatter.select(now - (HOUR - 1), now, Style.SHORT)
        assertEquals(res("relative_time_minutes_short"), f.resource)
    }

    @Test fun `2 hours renders hours short with arg 2`() {
        val f = RelativeTimeFormatter.select(now - 2 * HOUR, now, Style.SHORT)
        assertEquals(res("relative_time_hours_short"), f.resource)
        assertEquals(2L, f.arg)
    }

    @Test fun `3 days renders days short`() {
        val f = RelativeTimeFormatter.select(now - 3 * DAY, now, Style.SHORT)
        assertEquals(res("relative_time_days_short"), f.resource)
        assertEquals(3L, f.arg)
    }

    @Test fun `3 weeks renders weeks short`() {
        val f = RelativeTimeFormatter.select(now - 3 * WEEK, now, Style.SHORT)
        assertEquals(res("relative_time_weeks_short"), f.resource)
        assertEquals(3L, f.arg)
    }

    @Test fun `6 months renders months short`() {
        val f = RelativeTimeFormatter.select(now - 6 * MONTH, now, Style.SHORT)
        assertEquals(res("relative_time_months_short"), f.resource)
        assertEquals(6L, f.arg)
    }

    @Test fun `5 years renders years short`() {
        val f = RelativeTimeFormatter.select(now - 5 * YEAR, now, Style.SHORT)
        assertEquals(res("relative_time_years_short"), f.resource)
        assertEquals(5L, f.arg)
    }

    @Test fun `style LONG picks the long form`() {
        val f = RelativeTimeFormatter.select(now - 3 * DAY, now, Style.LONG)
        assertEquals(res("relative_time_days_long"), f.resource)
        assertEquals(3L, f.arg)
    }

    @Test fun `future timestamp clamps to now branch`() {
        val f = RelativeTimeFormatter.select(now + MIN, now, Style.SHORT)
        assertEquals(res("relative_time_now"), f.resource)
    }

    @Test fun `four weeks minus one day stays in weeks bucket not months`() {
        val f = RelativeTimeFormatter.select(now - (4 * WEEK - DAY), now, Style.SHORT)
        assertEquals(res("relative_time_weeks_short"), f.resource)
    }

    @Test fun `one year crosses into years bucket`() {
        val f = RelativeTimeFormatter.select(now - YEAR, now, Style.SHORT)
        assertEquals(res("relative_time_years_short"), f.resource)
    }

    companion object {
        private const val MIN   = 60_000L
        private const val HOUR  = 60 * MIN
        private const val DAY   = 24 * HOUR
        private const val WEEK  = 7 * DAY
        private const val MONTH = 30 * DAY
        private const val YEAR  = 365 * DAY
    }
}
