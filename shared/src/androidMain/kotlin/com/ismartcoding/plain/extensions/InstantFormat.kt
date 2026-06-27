package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.locale.LocaleHelper
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Instant

fun Instant.formatTime(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    return android.text.format.DateFormat.getTimeFormat(appContext)
        .format(c.time)
}

fun Instant.formatDateTime(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    val l = LocaleHelper.currentLocale()
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale(l.language, l.country))
        .format(c.time)
}

fun Instant.formatDate(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    val l = LocaleHelper.currentLocale()
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale(l.language, l.country))
        .format(c.time)
}