package com.ismartcoding.plain.lib.logcat

object LogCat {
    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6
    const val ASSERT = 7

    private const val TAG = "PlainApp"

    private val printer = LoggerPrinter()

    fun d(message: Any?, vararg args: Any?) {
        platformLog(DEBUG, TAG, format(message, args))
    }

    fun e(message: Any?, vararg args: Any?) {
        platformLog(ERROR, TAG, format(message, args))
    }

    fun i(message: Any?, vararg args: Any?) {
        platformLog(INFO, TAG, format(message, args))
    }

    fun w(message: Any?, vararg args: Any?) {
        platformLog(WARN, TAG, format(message, args))
    }

    fun v(message: Any?, vararg args: Any?) {
        platformLog(VERBOSE, TAG, format(message, args))
    }

    fun wtf(message: Any?, vararg args: Any?) {
        platformLog(ASSERT, TAG, format(message, args))
    }

    fun addLogAdapter(adapter: LogAdapter) {
        printer.addAdapter(adapter)
    }

    fun clearLogAdapters() {
        printer.clearLogAdapters()
    }

    fun init(context: Any?) {
        initLogCat(context)
    }

    fun logFolder(): String = logFolderImpl()

    private fun format(message: Any?, args: Array<out Any?>): String {
        val msg = message?.toString() ?: "null"
        return if (args.isEmpty()) msg else buildString {
            append(msg)
            for (arg in args) {
                append(", ")
                append(arg?.toString() ?: "null")
            }
        }
    }
}

internal expect fun platformLog(priority: Int, tag: String, message: String)

internal expect fun initLogCat(context: Any?)

internal expect fun logFolderImpl(): String
