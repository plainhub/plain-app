package com.ismartcoding.plain.ui.page.feeds

import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.platform.formatDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** One renderable row of the grouped feed entries list. */
internal sealed interface FeedListRow {
    val key: String

    /**
     * Sticky per-day separator. Counts and [markReadIds] cover only the
     * entries currently loaded in the paging window. In the single-feed view
     * the day IS the cluster, so [collapsed]/[toggleKey] carry the collapse
     * state there; they stay default in the clustered view.
     */
    data class DayHeader(
        val dayKey: String,
        val kind: DayKind,
        val dateLabel: String,
        val count: Int,
        val unreadCount: Int,
        val markReadIds: Set<String>,
        val collapsed: Boolean = false,
        val toggleKey: String? = null,
    ) : FeedListRow {
        override val key: String = "day_$dayKey"
    }

    data class ClusterHeader(
        override val key: String,
        val clusterKey: String,
        val feed: DFeed?,
        val count: Int,
        val unreadCount: Int,
        val collapsed: Boolean,
        /** False for a one-entry cluster: shown expanded with no collapse toggle. */
        val collapsible: Boolean = true,
    ) : FeedListRow

    data class Entry(
        override val key: String,
        val entry: DFeedEntry,
    ) : FeedListRow

    /** Title-only previews (newest first) representing a collapsed cluster. */
    data class CollapsedDigest(
        override val key: String,
        val clusterKey: String,
        val entries: List<DFeedEntry>,
    ) : FeedListRow
}

internal enum class DayKind { TODAY, YESTERDAY, DATE }

/**
 * Groups a `published_at DESC` entry list into per-day buckets, each clustered
 * by feed. A cluster defaults to collapsed once it has no unread entries;
 * [expandedOverrides] (cluster key → explicit user choice) wins over the
 * default. A cluster holding a single entry is never collapsible — it renders
 * expanded with no toggle. With [clusterByFeed] false (single-feed filter
 * view) the day is the cluster: the collapse toggle moves onto the day header
 * (also dropped for a one-entry day) and no per-feed cluster headers are
 * emitted. Pure and cheap enough to run per recomposition on the paging window.
 */
internal fun buildFeedListRows(
    items: List<DFeedEntry>,
    feeds: Map<String, DFeed>,
    expandedOverrides: Map<String, Boolean>,
    timeZone: TimeZone,
    today: LocalDate,
    clusterByFeed: Boolean = true,
): List<FeedListRow> {
    if (items.isEmpty()) return emptyList()
    val yesterday = today.minus(DatePeriod(days = 1))
    val rows = mutableListOf<FeedListRow>()
    items.groupBy { it.publishedAt.toLocalDateTime(timeZone).date }.forEach { (date, dayEntries) ->
        val kind = when (date) {
            today -> DayKind.TODAY
            yesterday -> DayKind.YESTERDAY
            else -> DayKind.DATE
        }
        if (!clusterByFeed) {
            val clusterKey = "$date/${dayEntries.first().feedId}"
            val single = dayEntries.size == 1
            val collapsed = !single && (expandedOverrides[clusterKey] ?: (dayEntries.count { !it.read } == 0))
            rows += FeedListRow.DayHeader(
                dayKey = date.toString(),
                kind = kind,
                dateLabel = dayEntries.first().publishedAt.formatDate(),
                count = dayEntries.size,
                unreadCount = dayEntries.count { !it.read },
                markReadIds = dayEntries.map { it.id }.toSet(),
                collapsed = collapsed,
                toggleKey = if (single) null else clusterKey,
            )
            if (collapsed) {
                rows += FeedListRow.CollapsedDigest("$clusterKey/digest", clusterKey, dayEntries.take(2))
            } else {
                dayEntries.forEach { e -> rows += FeedListRow.Entry(e.id, e) }
            }
            return@forEach
        }
        rows += FeedListRow.DayHeader(
            dayKey = date.toString(),
            kind = kind,
            dateLabel = dayEntries.first().publishedAt.formatDate(),
            count = dayEntries.size,
            unreadCount = dayEntries.count { !it.read },
            markReadIds = dayEntries.map { it.id }.toSet(),
        )
        dayEntries.groupBy { it.feedId }.forEach { (feedId, cluster) ->
            val clusterKey = "$date/$feedId"
            val clusterUnread = cluster.count { !it.read }
            // A one-entry cluster has nothing to fold: always expanded, no toggle.
            val collapsible = cluster.size > 1
            val collapsed = collapsible && (expandedOverrides[clusterKey] ?: (clusterUnread == 0))
            rows += FeedListRow.ClusterHeader(
                clusterKey, clusterKey, feeds[feedId], cluster.size, clusterUnread, collapsed,
                collapsible = collapsible,
            )
            if (collapsed) {
                rows += FeedListRow.CollapsedDigest("$clusterKey/digest", clusterKey, cluster.take(2))
            } else {
                cluster.forEach { e ->
                    rows += FeedListRow.Entry(e.id, e)
                }
            }
        }
    }
    return rows
}
