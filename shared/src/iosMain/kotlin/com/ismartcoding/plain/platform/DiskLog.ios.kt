package com.ismartcoding.plain.platform

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSThread

actual fun currentDateTimeString(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = DiskLogFormatStrategy.LOG_DATE_FORMAT
    formatter.locale = NSLocale(localeIdentifier = "en_US_POSIX")
    return formatter.stringFromDate(NSDate())
}

actual fun currentThreadName(): String = NSThread.currentThread().name ?: "main"

actual fun resolveCallerInfo(): String? = null

actual fun readLogLinesNewestFirst(offset: Int, limit: Int): List<String> = emptyList()

actual fun clearLatestLogFile() = Unit
