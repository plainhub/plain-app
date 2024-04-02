package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.TimeAgoHelper
import kotlinx.datetime.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

fun Date.formatDateTime(): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, LocaleHelper.currentLocale()).format(this)
}

fun Date.formatTime(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = time
    return android.text.format.DateFormat.getTimeFormat(MainApp.instance).format(c.time)
}

fun Instant.formatTime(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    return android.text.format.DateFormat.getTimeFormat(MainApp.instance)
        .format(c.time)
}

fun Instant.formatDateTime(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, LocaleHelper.currentLocale())
        .format(c.time)
}

fun Instant.timeAgo(): String {
    return TimeAgoHelper.getString(toEpochMilliseconds())
}

fun Instant.formatDate(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    return DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleHelper.currentLocale())
        .format(c.time)
}

fun Date.formatName(): String {
    return SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.ENGLISH).format(this)
}
