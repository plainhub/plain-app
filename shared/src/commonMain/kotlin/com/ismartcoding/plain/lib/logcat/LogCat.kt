package com.ismartcoding.plain.lib.logcat

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@OptIn(ExperimentalObjCRefinement::class)
object LogCat {
    @HiddenFromObjC const val VERBOSE = 2
    @HiddenFromObjC const val DEBUG = 3
    @HiddenFromObjC const val INFO = 4
    @HiddenFromObjC const val WARN = 5
    @HiddenFromObjC const val ERROR = 6
    @HiddenFromObjC const val ASSERT = 7

    private const val TAG = "PlainApp"

    private val printer = LoggerPrinter()

    fun d(message: Any?, vararg args: Any?) {
        dispatch(DEBUG, message, args)
    }

    fun e(message: Any?, vararg args: Any?) {
        dispatch(ERROR, message, args)
    }

    fun i(message: Any?, vararg args: Any?) {
        dispatch(INFO, message, args)
    }

    fun w(message: Any?, vararg args: Any?) {
        dispatch(WARN, message, args)
    }

    fun v(message: Any?, vararg args: Any?) {
        dispatch(VERBOSE, message, args)
    }

    fun wtf(message: Any?, vararg args: Any?) {
        dispatch(ASSERT, message, args)
    }

    private fun dispatch(priority: Int, message: Any?, args: Array<out Any?>) {
        val formatted = format(message, args)
        platformLog(priority, TAG, formatted)
        printer.log(priority, TAG, formatted, null)
    }

    fun addLogAdapter(adapter: LogAdapter) {
        printer.addAdapter(adapter)
    }

    fun clearLogAdapters() {
        printer.clearLogAdapters()
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

internal expect fun logFolderImpl(): String
