package com.ismartcoding.plain.features.file

import com.ismartcoding.plain.i18n.*

import org.jetbrains.compose.resources.StringResource

enum class FileSortBy {
    // TAKEN_AT has no ASC variant on purpose: it exists for the "group by taken
    // date" photo view, which always shows newest-day first. Plain date/size/
    // name sorts are the general-purpose symmetric pair.
    DATE_ASC,
    DATE_DESC,
    SIZE_ASC,
    SIZE_DESC,
    NAME_ASC,
    NAME_DESC,
    TAKEN_AT_DESC,
    ;

    fun getTextId(): StringResource {
        return when (this) {
            NAME_ASC -> {
                Res.string.name_asc
            }
            NAME_DESC -> {
                Res.string.name_desc
            }
            DATE_ASC -> {
                Res.string.oldest_date_first
            }
            DATE_DESC -> {
                Res.string.newest_date_first
            }
            SIZE_ASC -> {
                Res.string.smallest_first
            }
            SIZE_DESC -> {
                Res.string.largest_first
            }
            TAKEN_AT_DESC -> {
                Res.string.group_by_taken_at
            }
        }
    }
}