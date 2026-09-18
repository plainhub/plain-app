package com.ismartcoding.plain.ui.helpers

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MediaGroupHelperTest {

    private val utc = TimeZone.UTC

    private data class Media(val id: String, val takenAt: Instant)

    private fun groupOf(vararg epochs: Long) =
        groupMediaByDate(epochs.map { Media("m$it", Instant.fromEpochSeconds(it)) }, { it.takenAt }, utc)

    @Test
    fun itemsOnDifferentDaysSplitIntoGroups() {
        // 2026-01-01 23:00 and 2026-01-02 01:00 UTC.
        val groups = groupOf(1767308400L, 1767315600L)
        assertEquals(listOf("2026-01-02", "2026-01-01"), groups.map { it.dateKey })
    }

    @Test
    fun itemsOnSameDayMergeIntoOneGroupPreservingOrder() {
        // Both 2026-01-01, 10:00 and 22:00 UTC.
        val groups = groupOf(1767261600L, 1767304800L)
        assertEquals(1, groups.size)
        assertEquals(listOf("m1767261600", "m1767304800"), groups[0].items.map { it.id })
    }

    @Test
    fun groupsSortByDateDescending() {
        val groups = groupOf(1609459200L, 1767225600L, 1672531200L) // 2021, 2026, 2023 (all Jan 1st 00:00 UTC)
        assertEquals(listOf("2026-01-01", "2023-01-01", "2021-01-01"), groups.map { it.dateKey })
    }

    @Test
    fun dayBoundaryFollowsInjectedTimeZone() {
        // 2026-01-01 23:30 UTC is already 2026-01-02 in UTC+13.
        val instant = Instant.fromEpochSeconds(1767310200L)
        val items = listOf(Media("a", instant))
        val inUtc = groupMediaByDate(items, { it.takenAt }, TimeZone.UTC)
        val inPlus13 = groupMediaByDate(items, { it.takenAt }, TimeZone.of("UTC+13"))
        assertEquals("2026-01-01", inUtc.single().dateKey)
        assertEquals("2026-01-02", inPlus13.single().dateKey)
    }

    @Test
    fun subSecondPrecisionDoesNotChangeTheDateKey() {
        val second = Instant.fromEpochSeconds(1767300600)
        val items = listOf(
            Media("truncated", second),
            Media("withNanos", Instant.fromEpochSeconds(second.epochSeconds, 999_999_999)),
        )
        val groups = groupMediaByDate(items, { it.takenAt }, utc)
        assertEquals(1, groups.size)
    }

    @Test
    fun emptyInputProducesNoGroups() {
        assertEquals(0, groupMediaByDate(emptyList<Media>(), { it.takenAt }, utc).size)
    }
}
