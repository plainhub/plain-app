package com.ismartcoding.plain.features.files

import android.provider.MediaStore
import com.ismartcoding.plain.models.MediaSortBy

fun MediaSortBy.toSortBy(): SortBy = providerSort(name)
fun FileSortBy.toFileSortBy(): SortBy = providerSort(name)

private fun providerSort(name: String): SortBy {
    val column = when (name) {
        "UPDATED_AT_ASC", "UPDATED_AT_DESC" -> MediaStore.MediaColumns.DATE_MODIFIED
        "SIZE_ASC", "SIZE_DESC" -> MediaStore.MediaColumns.SIZE
        "NAME_ASC", "NAME_DESC" -> MediaStore.MediaColumns.DISPLAY_NAME
        "TITLE_ASC", "TITLE_DESC" -> MediaStore.MediaColumns.TITLE
        "TAKEN_AT_DESC" -> "CASE WHEN ${MediaStore.Images.Media.DATE_TAKEN} > 0 THEN ${MediaStore.Images.Media.DATE_TAKEN} ELSE ${MediaStore.MediaColumns.DATE_ADDED} * 1000 END"
        else -> error("Unsupported sort: $name")
    }
    return SortBy(column, if (name.endsWith("_ASC")) SortDirection.ASC else SortDirection.DESC)
}
