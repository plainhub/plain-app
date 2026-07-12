package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.LocaleHelper
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Date.formatDateTime(): String {
    val l = LocaleHelper.currentLocale()
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale(l.language, l.country)).format(this)
}

fun Date.formatTime(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = time
    return android.text.format.DateFormat.getTimeFormat(appContext).format(c.time)
}

fun Date.formatName(): String {
    return SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.ENGLISH).format(this)
}
