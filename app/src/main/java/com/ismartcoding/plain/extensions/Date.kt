package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.features.locale.LocaleHelper
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Date.formatDateTime(): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, LocaleHelper.currentLocale()).format(this)
}

fun Date.formatTime(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = time
    return android.text.format.DateFormat.getTimeFormat(MainApp.instance).format(c.time)
}

fun Date.formatName(): String {
    return SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.ENGLISH).format(this)
}
