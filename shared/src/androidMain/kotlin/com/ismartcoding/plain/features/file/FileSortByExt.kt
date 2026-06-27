package com.ismartcoding.plain.features.file

import android.provider.MediaStore
import com.ismartcoding.plain.data.SortBy
import com.ismartcoding.plain.data.SortDirection

fun FileSortBy.toSortBy(): SortBy {
    return when (this) {
        FileSortBy.NAME_ASC -> {
            SortBy(MediaStore.MediaColumns.TITLE, SortDirection.ASC)
        }
        FileSortBy.NAME_DESC -> {
            SortBy(MediaStore.MediaColumns.TITLE, SortDirection.DESC)
        }
        FileSortBy.DATE_ASC -> {
            SortBy(MediaStore.MediaColumns.DATE_MODIFIED, SortDirection.ASC)
        }
        FileSortBy.DATE_DESC -> {
            SortBy(MediaStore.MediaColumns.DATE_MODIFIED, SortDirection.DESC)
        }
        FileSortBy.SIZE_ASC -> {
            SortBy(MediaStore.MediaColumns.SIZE, SortDirection.ASC)
        }
        FileSortBy.SIZE_DESC -> {
            SortBy(MediaStore.MediaColumns.SIZE, SortDirection.DESC)
        }
        FileSortBy.TAKEN_AT_DESC -> {
            SortBy("CASE WHEN ${MediaStore.Images.Media.DATE_TAKEN} > 0 THEN ${MediaStore.Images.Media.DATE_TAKEN} ELSE ${MediaStore.MediaColumns.DATE_ADDED} * 1000 END", SortDirection.DESC)
        }
    }
}

fun FileSortBy.toFileSortBy(): SortBy {
    return when (this) {
        FileSortBy.NAME_ASC -> {
            SortBy(MediaStore.MediaColumns.DISPLAY_NAME, SortDirection.ASC)
        }
        FileSortBy.NAME_DESC -> {
            SortBy(MediaStore.MediaColumns.DISPLAY_NAME, SortDirection.DESC)
        }
        FileSortBy.DATE_ASC -> {
            SortBy(MediaStore.MediaColumns.DATE_MODIFIED, SortDirection.ASC)
        }
        FileSortBy.DATE_DESC -> {
            SortBy(MediaStore.MediaColumns.DATE_MODIFIED, SortDirection.DESC)
        }
        FileSortBy.SIZE_ASC -> {
            SortBy(MediaStore.MediaColumns.SIZE, SortDirection.ASC)
        }
        FileSortBy.SIZE_DESC -> {
            SortBy(MediaStore.MediaColumns.SIZE, SortDirection.DESC)
        }
        FileSortBy.TAKEN_AT_DESC -> {
            SortBy(MediaStore.MediaColumns.DATE_MODIFIED, SortDirection.DESC)
        }
    }
}