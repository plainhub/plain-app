package com.ismartcoding.plain.lib.logcat

import platform.Foundation.NSLog
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

internal actual fun platformLog(priority: Int, tag: String, message: String) {
    val prefix = when (priority) {
        LogCat.VERBOSE -> "[V]"
        LogCat.DEBUG -> "[D]"
        LogCat.INFO -> "[I]"
        LogCat.WARN -> "[W]"
        LogCat.ERROR -> "[E]"
        LogCat.ASSERT -> "[WTF]"
        else -> "[?]"
    }
    NSLog("$prefix $message")
}

internal actual fun initLogCat(context: Any?) {}

internal actual fun logFolderImpl(): String {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val docs = (paths.firstOrNull() as? String) ?: ""
    return "$docs/logs"
}
