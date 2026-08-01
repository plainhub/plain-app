package com.ismartcoding.plain.platform

/**
 * Recent application log lines captured by the platform crash handler.
 * Empty string when no logs are available (e.g. on iOS).
 */
expect fun getAppLogs(): String

/**
 * Write the crash report — device info, report body, and app logs — to a
 * cache file named `crash_report.txt` and return its absolute path so the
 * caller can navigate to a text viewer.
 *
 * Device info is assembled from the common [AppInfo] expects; the file write
 * goes through [cacheDirPath] + [writeBytesToPath]. Returns an empty string
 * on failure.
 *
 * @param report  Crash report body text (already formatted).
 */
fun writeCrashReport(report: String): String {
    val deviceInfo = buildString {
        appendLine("App version: ${getAppVersion()}")
        appendLine("OS: ${getOSVersion()}")
        appendLine("Device: ${getDeviceName()}")
        appendLine("Platform: ${getPlatformName()}")
        appendLine("Build type: ${getBuildType()}")
    }
    val bodyText = buildString {
        append(deviceInfo)
        appendLine()
        appendLine("--- Crash Report ---")
        append(report)
        val logs = getAppLogs()
        if (logs.isNotEmpty()) {
            appendLine()
            appendLine()
            appendLine("--- App Logs ---")
            append(logs)
        }
    }
    val path = cacheDirPath() + "/crash_report.txt"
    return if (writeBytesToPath(path, bodyText.encodeToByteArray())) path else ""
}
