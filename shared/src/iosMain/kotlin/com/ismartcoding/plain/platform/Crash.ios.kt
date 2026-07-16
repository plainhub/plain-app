package com.ismartcoding.plain.platform

actual fun shareCrashReport(report: String, includeAppLogs: Boolean) {
    // iOS: would require a UIActivityViewController with the report as an
    // attachment; left as a no-op for now.
}
