package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.HttpLogSink
import com.ismartcoding.plain.api.httpLogSink
import com.ismartcoding.plain.lib.logcat.DiskLogAdapter
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
        val builder = StringBuilder()
        builder.append(currentDateTimeString())
        builder.append(SEPARATOR)
        builder.append(logLevel(priority))
        builder.append(SEPARATOR)
        builder.append(message)
        builder.append(NEW_LINE)
        logStrategy.log(priority, tag, builder.toString())
    }

    companion object {
        internal const val LOG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
        private const val NEW_LINE = "\n"
        private const val SEPARATOR = " "

        fun logLevel(value: Int): String {
            return when (value) {
                LogCat.VERBOSE -> "[V]"
                LogCat.DEBUG -> "[D]"
                LogCat.INFO -> "[I]"
                LogCat.WARN -> "[W]"
                LogCat.ERROR -> "[E]"
                LogCat.ASSERT -> "[WTF]"
                else -> "[?]"
            }
        }

        fun getLogFolder(): String = LogCat.logFolder()

        fun getInstance(): DiskLogFormatStrategy = DiskLogFormatStrategy(DiskLogStrategy())
    }
}

expect fun currentDateTimeString(): String

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

/**
 * Initialize disk logging and HTTP request logging.
 * - Debug builds: all log levels (VERBOSE+) are written to disk
 * - Release builds: only WARN+ are written to disk
 * Idempotent — safe to call multiple times (LogCat.addLogAdapter just appends).
 */
fun initDiskLogging() {
    LogCat.addLogAdapter(
        DiskLogAdapter(
            DiskLogFormatStrategy.getInstance(),
            minPriority = if (isDebugBuild()) LogCat.VERBOSE else LogCat.WARN,
        ),
    )
    httpLogSink = HttpLogSink { LogCat.v(it) }
    // Self-test: guarantee at least one line is written so the log file is
    // never empty. A WARN always passes the adapter's minPriority filter.
    LogCat.w("DiskLog: initialized (debug=${isDebugBuild()})")
}
