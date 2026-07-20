package com.ismartcoding.plain.platform

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ismartcoding.plain.AppIntents
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.CrashHandler
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.AppLogHelper
import java.io.File

actual fun shareCrashReport(report: String, includeAppLogs: Boolean) {
    val appVersion = getAppVersion()
    val deviceInfo = AppLogHelper.buildDeviceInfoText()
    val bodyText = buildString {
        append(deviceInfo)
        appendLine()
        appendLine("--- Crash Report ---")
        append(report)
        if (includeAppLogs) {
            val logs = CrashHandler.getAppLogs(appContext)
            if (logs.isNotEmpty()) {
                appendLine()
                appendLine()
                appendLine("--- App Logs ---")
                append(logs)
            }
        }
    }

    // Write crash report to a temp file so it can be attached to the email
    val crashFile = File(appContext.cacheDir, "crash_report.txt")
    try {
        crashFile.writeText(bodyText)
    } catch (_: Exception) {
    }

    val attachmentUri: Uri? = try {
        FileProvider.getUriForFile(appContext, AppIntents.AUTHORITY, crashFile)
    } catch (_: Exception) {
        null
    }

    val intent = if (attachmentUri != null) {
        Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "Crash Report - PlainApp $appVersion")
            putExtra(Intent.EXTRA_TEXT, bodyText)
            putExtra(Intent.EXTRA_STREAM, attachmentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${Constants.SUPPORT_EMAIL}")
            putExtra(Intent.EXTRA_SUBJECT, "Crash Report - PlainApp $appVersion")
            putExtra(Intent.EXTRA_TEXT, bodyText)
        }
    }
    // appContext is the Application context, so FLAG_ACTIVITY_NEW_TASK is required
    // when calling startActivity() outside of an Activity context.
    val chooser = Intent.createChooser(intent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    appContext.startActivity(chooser)
}
