package com.ismartcoding.plain.ui.files

import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.StringResource

fun FileSortBy.getTextId(): StringResource = sortTextId(name)

fun com.ismartcoding.plain.models.MediaSortBy.getTextId(): StringResource = sortTextId(name)

private fun sortTextId(name: String): StringResource {
        return when (name) {
            "TITLE_ASC" -> Res.string.title_asc
            "TITLE_DESC" -> Res.string.title_desc
            "NAME_ASC" -> {
                Res.string.name_asc
            }
            "NAME_DESC" -> {
                Res.string.name_desc
            }
            "UPDATED_AT_ASC" -> {
                Res.string.oldest_date_first
            }
            "UPDATED_AT_DESC" -> {
                Res.string.newest_date_first
            }
            "SIZE_ASC" -> {
                Res.string.smallest_first
            }
            "SIZE_DESC" -> {
                Res.string.largest_first
            }
            "TAKEN_AT_DESC" -> {
                Res.string.group_by_taken_at
            }
            else -> error("Unsupported sort: $name")
        }
    }
