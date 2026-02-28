package com.ismartcoding.plain.features.file

import android.provider.MediaStore
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.data.enums.SortDirection
import com.ismartcoding.plain.R

enum class FileSortBy {
    DATE_ASC,
    DATE_DESC,
    SIZE_ASC,
    SIZE_DESC,
    NAME_ASC,
    NAME_DESC,
    TAKEN_AT_DESC,
    ;

    fun getTextId(): Int {
        return when (this) {
            NAME_ASC -> {
                R.string.name_asc
            }
            NAME_DESC -> {
                R.string.name_desc
            }
            DATE_ASC -> {
                R.string.oldest_date_first
            }
            DATE_DESC -> {
                R.string.newest_date_first
            }
            SIZE_ASC -> {
                R.string.smallest_first
            }
            SIZE_DESC -> {
                R.string.largest_first
            }
            TAKEN_AT_DESC -> {
                R.string.group_by_taken_at
            }
        }
    }

    fun toSortBy(): SortBy {
        return when (this) {
            NAME_ASC -> {
                SortBy(MediaStore.MediaColumns.TITLE, SortDirection.ASC)
            }
            NAME_DESC -> {
                SortBy(MediaStore.MediaColumns.TITLE, SortDirection.DESC)
            }
            DATE_ASC -> {
                SortBy(MediaStore.MediaColumns.DATE_MODIFIED, SortDirection.ASC)
            }
            DATE_DESC -> {
                SortBy(MediaStore.MediaColumns.DATE_MODIFIED, SortDirection.DESC)
            }
            SIZE_ASC -> {
                SortBy(MediaStore.MediaColumns.SIZE, SortDirection.ASC)
            }
            SIZE_DESC -> {
                SortBy(MediaStore.MediaColumns.SIZE, SortDirection.DESC)
            }
            TAKEN_AT_DESC -> {
                SortBy("CASE WHEN ${MediaStore.Images.Media.DATE_TAKEN} > 0 THEN ${MediaStore.Images.Media.DATE_TAKEN} ELSE ${MediaStore.MediaColumns.DATE_ADDED} * 1000 END", SortDirection.DESC)
            }
        }
    }

    fun toFileSortBy(): SortBy {
        return when (this) {
            NAME_ASC -> {
                SortBy(MediaStore.MediaColumns.DISPLAY_NAME, SortDirection.ASC)
            }
            NAME_DESC -> {
                SortBy(MediaStore.MediaColumns.DISPLAY_NAME, SortDirection.DESC)
            }
            DATE_ASC -> {
                SortBy(MediaStore.MediaColumns.DATE_MODIFIED, SortDirection.ASC)
            }
            DATE_DESC -> {
                SortBy(MediaStore.MediaColumns.DATE_MODIFIED, SortDirection.DESC)
            }
            SIZE_ASC -> {
                SortBy(MediaStore.MediaColumns.SIZE, SortDirection.ASC)
            }
            SIZE_DESC -> {
                SortBy(MediaStore.MediaColumns.SIZE, SortDirection.DESC)
            }
            TAKEN_AT_DESC -> {
                SortBy(MediaStore.MediaColumns.DATE_MODIFIED, SortDirection.DESC)
            }
        }
    }
}
