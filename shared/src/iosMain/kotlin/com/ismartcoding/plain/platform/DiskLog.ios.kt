@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.toByteArray
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSFileManager

actual fun currentDateTimeString(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = DiskLogFormatStrategy.LOG_DATE_FORMAT
    formatter.locale = NSLocale(localeIdentifier = "en_US_POSIX")
    return formatter.stringFromDate(NSDate())
}

actual fun readLogLinesNewestFirst(offset: Int, limit: Int): List<String> {
    if (limit <= 0) return emptyList()
    val filePath = getLatestLogFilePath()
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(filePath)) return emptyList()
    val data = fm.contentsAtPath(filePath) ?: return emptyList()
    val text = data.toByteArray().decodeToString()
    val allLines = text.lines()
    val total = allLines.size
    val start = (total - offset).coerceAtLeast(0)
    val end = (start - limit).coerceAtLeast(0)
    if (start <= end) return emptyList()
    return allLines.subList(end, start).reversed()
}

actual fun clearLatestLogFile() {
    val filePath = getLatestLogFilePath()
    val fm = NSFileManager.defaultManager
    if (fm.fileExistsAtPath(filePath)) {
        fm.createFileAtPath(filePath, null, null)
    }
}
