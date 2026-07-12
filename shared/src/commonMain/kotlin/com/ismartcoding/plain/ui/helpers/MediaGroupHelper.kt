package com.ismartcoding.plain.ui.helpers

import com.ismartcoding.plain.platform.formatDate
import kotlinx.datetime.Instant as KxInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class MediaDateGroup<T>(
    val dateKey: String,
    val dateLabel: String,
    val items: List<T>
)

fun <T> groupMediaByDate(
    items: List<T>,
    getDate: (T) -> Instant
): List<MediaDateGroup<T>> {
    return items.groupBy { item ->
        val instant = getDate(item)
        val localDateTime = KxInstant.fromEpochSeconds(instant.epochSeconds)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.year.toString().padStart(4, '0')}-${localDateTime.monthNumber.toString().padStart(2, '0')}-${localDateTime.dayOfMonth.toString().padStart(2, '0')}"
    }.map { (dateKey, groupItems) ->
        MediaDateGroup(
            dateKey = dateKey,
            dateLabel = getDate(groupItems.first()).formatDate(),
            items = groupItems
        )
    }.sortedByDescending { it.dateKey }
}
