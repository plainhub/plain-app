package com.ismartcoding.lib.extensions

import android.content.ContentResolver
import android.os.Bundle
import android.os.Parcelable
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.data.enums.SortDirection
import com.ismartcoding.lib.isTPlus

fun Bundle.sort(sortBy: SortBy) {
    putStringArray(
        ContentResolver.QUERY_ARG_SORT_COLUMNS,
        arrayOf(sortBy.field),
    )
    putInt(
        ContentResolver.QUERY_ARG_SORT_DIRECTION,
        if (sortBy.direction == SortDirection.ASC) ContentResolver.QUERY_SORT_DIRECTION_ASCENDING else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
    )
}

fun Bundle.where(
    selection: String,
    args: List<String>,
) {
    putString(
        ContentResolver.QUERY_ARG_SQL_SELECTION,
        selection,
    )
    putStringArray(
        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
        args.toTypedArray(),
    )
}

fun Bundle.paging(
    offset: Int,
    limit: Int,
) {
    if (offset > 0) {
        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
    }
    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? =
    when {
        isTPlus() -> getParcelable(key, T::class.java)
        else ->
            @Suppress("DEPRECATION")
            getParcelable(key)
                as? T
    }
