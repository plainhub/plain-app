package com.ismartcoding.plain.lib.logcat

import android.util.Log
import com.ismartcoding.plain.appContext

internal actual fun platformLog(priority: Int, tag: String, message: String) {
    try {
        when (priority) {
            LogCat.VERBOSE -> Log.v(tag, message)
            LogCat.DEBUG -> Log.d(tag, message)
            LogCat.INFO -> Log.i(tag, message)
            LogCat.WARN -> Log.w(tag, message)
            LogCat.ERROR -> Log.e(tag, message)
            LogCat.ASSERT -> Log.wtf(tag, message)
        }
    } catch (_: RuntimeException) {
        // Ignore logging failures (e.g. android.util.Log not mocked in unit tests)
    }
}

internal actual fun logFolderImpl(): String = appContext.filesDir.absolutePath + "/logs"
