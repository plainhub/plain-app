package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.LocaleHelper
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.time.Instant

actual fun Instant.formatTime(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    return android.text.format.DateFormat.getTimeFormat(appContext)
        .format(c.time)
}

actual fun Instant.formatDateTime(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    val l = LocaleHelper.currentLocale()
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, java.util.Locale(l.language, l.country))
        .format(c.time)
}

actual fun Instant.formatDate(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    val l = LocaleHelper.currentLocale()
    return DateFormat.getDateInstance(DateFormat.MEDIUM, java.util.Locale(l.language, l.country))
        .format(c.time)
}

actual fun Instant.formatName(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = epochSeconds * 1000
    return SimpleDateFormat(InstantFormats.FILE_NAME_DATE_FORMAT, java.util.Locale.ENGLISH).format(c.time)
}

fun java.util.Date.formatName(): String {
    return SimpleDateFormat(InstantFormats.FILE_NAME_DATE_FORMAT, java.util.Locale.ENGLISH).format(this)
}
