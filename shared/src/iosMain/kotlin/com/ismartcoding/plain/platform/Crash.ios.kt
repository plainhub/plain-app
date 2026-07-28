package com.ismartcoding.plain.platform

actual fun writeCrashReport(report: String): String {
    // iOS: not yet implemented; would require writing to a temp file.
    return ""
}
