package com.ismartcoding.lib.logcat

import android.content.Context
import android.os.HandlerThread
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DiskLogFormatStrategy(private val logStrategy: LogStrategy) : FormatStrategy {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
    ) {
        val builder = StringBuilder()
        builder.append(dateFormat.format(LocalDateTime.now()))

        builder.append(SEPARATOR)
        builder.append(logLevel(priority))

        builder.append(SEPARATOR)
        builder.append(tag ?: "")

        builder.append(SEPARATOR)
        builder.append(message)

        builder.append(NEW_LINE)

        logStrategy.log(priority, tag, builder.toString())
    }

    companion object {
        private val NEW_LINE = System.getProperty("line.separator")
        private const val SEPARATOR = " "
        private var instance: DiskLogFormatStrategy? = null
        private const val MAX_BYTES = 25 * 1024 * 1024

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

        fun getLogFolder(context: Context): String {
            return context.filesDir.absolutePath + "/logs"
        }

        fun getInstance(context: Context): DiskLogFormatStrategy {
            if (instance == null) {
                val folder = getLogFolder(context)
                val ht = HandlerThread("AndroidFileLogger.$folder")
                ht.start()
                val handler = DiskLogStrategy.WriteHandler(ht.looper, folder, MAX_BYTES)
                instance = DiskLogFormatStrategy(DiskLogStrategy(handler))
            }

            return instance!!
        }
    }
}
