package com.ismartcoding.plain.lib.logcat

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

actual fun currentDateTimeString(): String = DATE_FORMATTER.format(LocalDateTime.now())

actual fun currentThreadName(): String = Thread.currentThread().name

actual fun resolveCallerInfo(): String? {
    val trace = Thread.currentThread().stackTrace
    for (frame in trace) {
        val name = frame.className
        if (!isInternalFrame(name)) {
            val simpleClass = name.substringAfterLast('.')
            val method = frame.methodName
            val file = frame.fileName
            val line = frame.lineNumber
            return if (!file.isNullOrEmpty() && line > 0) {
                "$simpleClass.$method ($file:$line)"
            } else {
                "$simpleClass.$method"
            }
        }
    }
    return null
}

private fun isInternalFrame(className: String): Boolean {
    return className == "com.ismartcoding.plain.lib.logcat.DiskLogFormatStrategy" ||
        className == "com.ismartcoding.plain.lib.logcat.DiskLogAdapter" ||
        className == "com.ismartcoding.plain.lib.logcat.LoggerPrinter" ||
        className == "com.ismartcoding.plain.lib.logcat.LogCat" ||
        className == "java.lang.Thread"
}