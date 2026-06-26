package com.ismartcoding.plain.lib.logcat

import platform.Foundation.NSLog
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

actual object LogCat {
    actual val VERBOSE = 2
    actual val DEBUG = 3
    actual val INFO = 4
    actual val WARN = 5
    actual val ERROR = 6
    actual val ASSERT = 7

    actual fun d(message: Any?, vararg args: Any?) {
        NSLog("[D] ${format(message, args)}")
    }

    actual fun e(message: Any?, vararg args: Any?) {
        NSLog("[E] ${format(message, args)}")
    }

    actual fun i(message: Any?, vararg args: Any?) {
        NSLog("[I] ${format(message, args)}")
    }

    actual fun w(message: Any?, vararg args: Any?) {
        NSLog("[W] ${format(message, args)}")
    }

    actual fun v(message: Any?, vararg args: Any?) {
        NSLog("[V] ${format(message, args)}")
    }

    actual fun wtf(message: Any?, vararg args: Any?) {
        NSLog("[WTF] ${format(message, args)}")
    }

    actual fun addLogAdapter(adapter: LogAdapter) {
        printer.addAdapter(adapter)
    }

    actual fun clearLogAdapters() {
        printer.clearLogAdapters()
    }

    actual fun init(context: Any?) {}

    actual fun logFolder(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val docs = (paths.firstOrNull() as? String) ?: ""
        return "$docs/logs"
    }

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