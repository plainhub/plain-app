package com.ismartcoding.plain.lib.extensions

import android.content.ContentResolver
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableString
import com.ismartcoding.plain.data.SortBy
import com.ismartcoding.plain.platform.isTPlus

fun Bundle.sort(sortBy: SortBy) {
    // Always use QUERY_ARG_SQL_SORT_ORDER (raw SQL) instead of the structured
    // QUERY_ARG_SORT_COLUMNS + QUERY_ARG_SORT_DIRECTION. The structured form is
    // validated/rejected by some MediaProvider implementations, while count()
    // (which sends no sort) succeeds — proving the sort arg is the failure
    // point. The raw SQL form is the same approach pre-R uses and is proven.
    putString(
        ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
        "${sortBy.field} ${sortBy.direction}",
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

fun Bundle.getString2(key: String): String {
    return when (val extra = get(key)) {
        null -> {
            ""
        }
        is String -> {
            extra
        }

        is SpannableString -> {
            extra.toString()
        }

        else -> {
            ""
        }
    }
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? =
    when {
        isTPlus() -> getParcelable(key, T::class.java)
        else ->
            @Suppress("DEPRECATION")
            getParcelable(key)
                as? T
    }
