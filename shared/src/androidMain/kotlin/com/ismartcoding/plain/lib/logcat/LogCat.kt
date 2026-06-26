package com.ismartcoding.plain.lib.logcat

import android.content.Context
import android.util.Log

private var appContext: Context? = null

actual object LogCat {
    actual val VERBOSE = 2
    actual val DEBUG = 3
    actual val INFO = 4
    actual val WARN = 5
    actual val ERROR = 6
    actual val ASSERT = 7

    private const val TAG = "PlainApp"

    actual fun d(message: Any?, vararg args: Any?) {
        Log.d(TAG, format(message, args))
    }

    actual fun e(message: Any?, vararg args: Any?) {
        Log.e(TAG, format(message, args))
    }

    actual fun i(message: Any?, vararg args: Any?) {
        Log.i(TAG, format(message, args))
    }

    actual fun w(message: Any?, vararg args: Any?) {
        Log.w(TAG, format(message, args))
    }

    actual fun v(message: Any?, vararg args: Any?) {
        Log.v(TAG, format(message, args))
    }

    actual fun wtf(message: Any?, vararg args: Any?) {
        Log.wtf(TAG, format(message, args))
    }

    actual fun addLogAdapter(adapter: LogAdapter) {
        printer.addAdapter(adapter)
    }

    actual fun clearLogAdapters() {
        printer.clearLogAdapters()
    }

    actual fun init(context: Any?) {
        appContext = context as? Context
    }

    actual fun logFolder(): String = appContext?.let { it.filesDir.absolutePath + "/logs" } ?: ""

    private val printer = LoggerPrinter()

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