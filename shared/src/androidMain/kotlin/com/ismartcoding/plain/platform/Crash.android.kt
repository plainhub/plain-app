package com.ismartcoding.plain.platform

import com.ismartcoding.plain.CrashHandler
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.AppLogHelper
import java.io.File

actual fun writeCrashReport(report: String): String {
    val deviceInfo = AppLogHelper.buildDeviceInfoText()
    val bodyText = buildString {
        append(deviceInfo)
        appendLine()
        appendLine("--- Crash Report ---")
        append(report)
        val logs = CrashHandler.getAppLogs(appContext)
        if (logs.isNotEmpty()) {
            appendLine()
            appendLine()
            appendLine("--- App Logs ---")
            append(logs)
        }
    }

    val crashFile = File(appContext.cacheDir, "crash_report.txt")
    try {
        crashFile.writeText(bodyText)
    } catch (_: Exception) {
        return ""
    }
    return crashFile.absolutePath
}
