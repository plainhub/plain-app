package com.ismartcoding.plain.features.file

import com.ismartcoding.plain.i18n.*

import org.jetbrains.compose.resources.StringResource

enum class FileSortBy {
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