package com.ismartcoding.plain.lib.logcat

import platform.Foundation.NSLog

internal actual fun platformLog(priority: Int, tag: String, message: String) {
    NSLog("${LogCat.logLevel(priority)} $message")
}

