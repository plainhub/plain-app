package com.ismartcoding.plain.lib.logcat

import com.ismartcoding.plain.platform.DiskLogFormatStrategy
import platform.Foundation.NSLog

internal actual fun platformLog(priority: Int, tag: String, message: String) {
    NSLog("${DiskLogFormatStrategy.logLevel(priority)} $message")
}

