package com.ismartcoding.plain.ui.helpers

import com.ismartcoding.plain.extensions.formatDate
import java.util.Calendar
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
    val calendar = Calendar.getInstance()
    return items.groupBy { item ->
        val instant = getDate(item)
        calendar.timeInMillis = instant.epochSeconds * 1000
        String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }.map { (dateKey, groupItems) ->
        MediaDateGroup(
            dateKey = dateKey,
            dateLabel = getDate(groupItems.first()).formatDate(),
            items = groupItems
        )
    }.sortedByDescending { it.dateKey }
}
