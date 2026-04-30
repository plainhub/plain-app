package com.ismartcoding.plain.helpers

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.ZipHelper
import com.ismartcoding.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import java.io.File

object AppLogHelper {
    fun getFileSize(context: Context): Long {
        val dir = File(DiskLogFormatStrategy.getLogFolder(context))
        if (!dir.exists()) {
            return 0
        }

        var totalSize: Long = 0
        val files = dir.listFiles() ?: arrayOf()
        for (file in files) {
            totalSize += file.length()
        }
        return totalSize
    }

    fun export(context: Context) {
        coMain {
            val logFolder = DiskLogFormatStrategy.getLogFolder(context)
            val logFolderFile = File(logFolder)
            val crashReportFile = File(context.filesDir, "crash_report.txt")

            if (!logFolderFile.exists() && !crashReportFile.exists()) {
                DialogHelper.showMessage(R.string.no_logs_error)
                return@coMain
            }

            DialogHelper.showLoading()
            val zipFile = File(context.cacheDir.absolutePath + "/logs.zip")
            val sourcePaths = buildList {
                if (logFolderFile.exists()) add(logFolder)
                if (crashReportFile.exists()) add(crashReportFile.absolutePath)
            }
            val success = withIO {
                ZipHelper.zip(sourcePaths, zipFile.absolutePath)
            }
            DialogHelper.hideLoading()
            if (!success) {
                DialogHelper.showErrorMessage(LocaleHelper.getString(R.string.error))
                return@coMain
            }
            share(context, zipFile)
        }
    }

    private fun share(
        context: Context,
        file: File,
    ) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "*/*"
        val appVersion = MainApp.getAppVersion()
        intent.putExtra(Intent.EXTRA_SUBJECT, LocaleHelper.getString(R.string.share_logs) + " - PlainApp $appVersion")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.SUPPORT_EMAIL))
        intent.putExtra(Intent.EXTRA_TEXT, buildDeviceInfoText())
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, Constants.AUTHORITY, file))
        val chooserIntent = Intent.createChooser(intent, LocaleHelper.getString(R.string.share_logs))
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, ShareHelper.getExcludeComponentNames(context).toTypedArray())
        context.startActivity(chooserIntent)
    }

    fun buildDeviceInfoText(): String {
        val appVersion = MainApp.getAppVersion()
        return buildString {
            appendLine("--- Device Info ---")
            appendLine("App: PlainApp $appVersion")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Brand: ${android.os.Build.BRAND}")
            appendLine("Product: ${android.os.Build.PRODUCT}")
        }
    }
}