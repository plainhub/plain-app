package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.logcat.DiskLogStrategy
import com.ismartcoding.plain.lib.logcat.FormatStrategy
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.logcat.LogStrategy

class DiskLogFormatStrategy(private val logStrategy: LogStrategy) : FormatStrategy {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
    ) {
        val caller = resolveCallerInfo()
        val builder = StringBuilder()
        builder.append(currentDateTimeString())
        builder.append(SEPARATOR)
        builder.append(logLevel(priority))
        builder.append(SEPARATOR)
        builder.append(tag ?: "")
        if (caller != null) {
            builder.append(SEPARATOR)
            builder.append(PREFIX)
            builder.append(currentThreadName())
            builder.append(']')
            builder.append(SEPARATOR)
            builder.append(caller)
        }
        builder.append(SEPARATOR)
        builder.append(message)
        builder.append(NEW_LINE)
        logStrategy.log(priority, tag, builder.toString())
    }

    companion object {
        internal const val LOG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
        private const val NEW_LINE = "\n"
        private const val SEPARATOR = " "
        private const val PREFIX = "│ ["

        fun logLevel(value: Int): String {
            return when (value) {
                LogCat.VERBOSE -> "VERBOSE"
                LogCat.DEBUG -> "DEBUG"
                LogCat.INFO -> "INFO"
                LogCat.WARN -> "WARN"
                LogCat.ERROR -> "ERROR"
                LogCat.ASSERT -> "ASSERT"
                else -> "UNKNOWN"
            }
        }

        fun getLogFolder(): String = LogCat.logFolder()

        fun getInstance(): DiskLogFormatStrategy = DiskLogFormatStrategy(DiskLogStrategy())
    }
}

expect fun currentDateTimeString(): String

expect fun currentThreadName(): String

expect fun resolveCallerInfo(): String?

/**
 * Absolute path to the latest log file (e.g. `<logFolder>/latest.log`).
 */
fun getLatestLogFilePath(): String = "${DiskLogFormatStrategy.getLogFolder()}/latest.log"

/**
 * Read log lines in newest-first order without loading the whole file into memory.
 * Returns at most [limit] lines starting from the [offset]-th newest line.
 * Returns an empty list if the log file does not exist or is empty.
 */
expect fun readLogLinesNewestFirst(offset: Int, limit: Int): List<String>

/**
 * Truncate the latest log file to zero length. No-op if the file does not exist.
 */
expect fun clearLatestLogFile()
