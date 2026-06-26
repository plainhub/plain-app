package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.TimeAgoHelper
import kotlin.time.Instant
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale


fun Instant.formatTime(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    return android.text.format.DateFormat.getTimeFormat(MainApp.instance)
        .format(c.time)
}

fun Instant.formatDateTime(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    val l = LocaleHelper.currentLocale()
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale(l.language, l.country))
        .format(c.time)
}

fun Instant.timeAgo(): String {
    return TimeAgoHelper.getString(toEpochMilliseconds())
}

fun Instant.formatDate(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    val l = LocaleHelper.currentLocale()
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale(l.language, l.country))
        .format(c.time)
}