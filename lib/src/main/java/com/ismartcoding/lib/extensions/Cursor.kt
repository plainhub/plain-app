package com.ismartcoding.lib.extensions

import android.database.Cursor
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import kotlinx.datetime.Instant

// https://developer.android.com/training/data-storage/room/accessing-data#kotlin

// Cache the column indices so that you don't need to call getColumnIndex() each time you process a row from the query result.
fun Cursor.getColumnIndex(key: String, cache: MutableMap<String, Int>): Int {
    return cache.getOrElse(key) {
        val index = getColumnIndex(key)
        cache[key] = index
        index
    }
}

fun Cursor.getStringValue(key: String, cache: MutableMap<String, Int>): String = getString(getColumnIndex(key, cache)) ?: ""

fun Cursor.getIntValue(key: String, cache: MutableMap<String, Int>): Int = getIntOrNull(getColumnIndex(key, cache)) ?: 0

fun Cursor.getLongValue(key: String, cache: MutableMap<String, Int>): Long = getLongOrNull(getColumnIndex(key, cache)) ?: 0L

fun Cursor.getTimeValue(key: String, cache: MutableMap<String, Int>): Instant = Instant.fromEpochMilliseconds(getLongValue(key, cache))