package com.ismartcoding.plain.lib.logcat

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