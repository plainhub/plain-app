package com.ismartcoding.plain.features.file

import android.provider.MediaStore
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.data.enums.SortDirection

enum class FileSortBy {
    DATE_ASC,
    DATE_DESC,
    SIZE_ASC,
    SIZE_DESC,
    NAME_ASC,
    NAME_DESC,
    ;

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
        }
    }
}
