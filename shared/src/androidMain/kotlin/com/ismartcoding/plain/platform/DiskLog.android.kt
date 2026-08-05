package com.ismartcoding.plain.platform

import com.ismartcoding.plain.helpers.AppLogHelper
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DiskLogFormatStrategy.LOG_DATE_FORMAT)

actual fun currentDateTimeString(): String = DATE_FORMATTER.format(LocalDateTime.now())

actual fun readLogLinesNewestFirst(offset: Int, limit: Int): List<String> {
    val logFile = File(getLatestLogFilePath())
    return AppLogHelper.getLogLines(logFile, offset, limit)
}

actual fun clearLatestLogFile() {
    val logFile = File(getLatestLogFilePath())
    if (logFile.exists()) logFile.writeText("")
}
