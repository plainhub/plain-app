package com.ismartcoding.lib.extensions

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import kotlinx.datetime.Instant

@SuppressLint("Range")
fun Cursor.getStringValue(key: String): String = getString(getColumnIndex(key)) ?: ""

@SuppressLint("Range")
fun Cursor.getStringValueOrNull(key: String): String? = getString(getColumnIndex(key))

@SuppressLint("Range")
fun Cursor.getIntValue(key: String): Int = getIntOrNull(getColumnIndex(key)) ?: 0

fun Cursor.getIntValueOrNull(key: String): Int? = getIntOrNull(getColumnIndex(key))

@SuppressLint("Range")
fun Cursor.getLongValue(key: String): Long = getLongOrNull(getColumnIndex(key)) ?: 0L

fun Cursor.getLongValueOrNull(key: String): Long? = getLongOrNull(getColumnIndex(key))

fun Cursor.getTimeValue(key: String): Instant = Instant.fromEpochMilliseconds(getLongValue(key))