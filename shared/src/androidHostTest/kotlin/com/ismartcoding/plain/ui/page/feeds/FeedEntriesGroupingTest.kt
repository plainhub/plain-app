package com.ismartcoding.plain.ui.page.feeds

import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class FeedEntriesGroupingTest {
    private val tz = TimeZone.UTC
    private val today = LocalDate(2026, 9, 16)
    private val feedA = DFeed(id = "a", name = "Feed A")
    private val feedB = DFeed(id = "b", name = "Feed B")
    private val feeds = mapOf("a" to feedA, "b" to feedB)

    private fun entry(
        id: String,
        feedId: String,
        publishedAt: Instant,
        read: Boolean = false,
    ) = DFeedEntry(id = id, feedId = feedId, publishedAt = publishedAt, read = read)

    @Test
    fun groupsByDayThenFeed() {
        val items =
            listOf(
                entry("1", "a", Instant.parse("2026-09-16T10:00:00Z")),
                entry("2", "a", Instant.parse("2026-09-16T09:00:00Z")),
                entry("3", "b", Instant.parse("2026-09-16T08:00:00Z")),
                entry("4", "a", Instant.parse("2026-09-15T10:00:00Z")),
            )
        val rows = buildFeedListRows(items, feeds, emptyMap(), tz, today)
        assertEquals(listOf("2026-09-16", "2026-09-15"), rows.filterIsInstance<FeedListRow.DayHeader>().map { it.dayKey })
        assertEquals(listOf(DayKind.TODAY, DayKind.YESTERDAY), rows.filterIsInstance<FeedListRow.DayHeader>().map { it.kind })
        // day 1 = cluster a (header + 2 entries) then cluster b (header + 1 entry)
        assertEquals(
            listOf("ClusterHeader", "Entry", "Entry", "ClusterHeader", "Entry"),
            rows.drop(1).take(5).map { it::class.simpleName },
        )
    }

    @Test
    fun readClustersCollapseToDigest() {
        val items =
            listOf(
                entry("1", "a", Instant.parse("2026-09-16T10:00:00Z"), read = true),
                entry("2", "a", Instant.parse("2026-09-16T09:00:00Z"), read = true),
            )
        val rows = buildFeedListRows(items, feeds, emptyMap(), tz, today)
        assertEquals(listOf("ClusterHeader", "CollapsedDigest"), rows.drop(1).map { it::class.simpleName })
        val digest = rows.filterIsInstance<FeedListRow.CollapsedDigest>().first()
        assertEquals("1", digest.entry.id)
        assertEquals("2026-09-16/a/digest", digest.key)
    }

    @Test
    fun unreadClusterStaysExpandedAndOverrideWins() {
        val items =
            listOf(
                entry("1", "a", Instant.parse("2026-09-16T10:00:00Z")),
                entry("2", "a", Instant.parse("2026-09-16T09:00:00Z"), read = true),
            )
        // default: cluster with unread entries is expanded
        var rows = buildFeedListRows(items, feeds, emptyMap(), tz, today)
        assertEquals(listOf("ClusterHeader", "Entry", "Entry"), rows.drop(1).map { it::class.simpleName })
        // user explicitly collapses it (override value = collapsed)
        rows = buildFeedListRows(items, feeds, mapOf("2026-09-16/a" to true), tz, today)
        assertEquals(listOf("ClusterHeader", "CollapsedDigest"), rows.drop(1).map { it::class.simpleName })
        // all read but user explicitly keeps it expanded
        val allRead = items.map { it.copy(read = true) }
        rows = buildFeedListRows(allRead, feeds, mapOf("2026-09-16/a" to false), tz, today)
        assertEquals(listOf("ClusterHeader", "Entry", "Entry"), rows.drop(1).map { it::class.simpleName })
    }

    @Test
    fun dayHeaderCountsCoverLoadedWindow() {
        val items =
            listOf(
                entry("1", "a", Instant.parse("2026-09-16T10:00:00Z")),
                entry("2", "b", Instant.parse("2026-09-15T10:00:00Z"), read = true),
            )
        val day0 = buildFeedListRows(items, feeds, emptyMap(), tz, today).filterIsInstance<FeedListRow.DayHeader>().first()
        assertEquals(setOf("1"), day0.markReadIds)
        assertEquals(1, day0.count)
        assertEquals(1, day0.unreadCount)
    }
}
