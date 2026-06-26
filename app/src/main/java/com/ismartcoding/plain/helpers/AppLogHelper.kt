package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.i18n.*

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.helpers.ZipHelper
import com.ismartcoding.plain.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import java.io.File
import java.io.RandomAccessFile

object AppLogHelper {
    private const val READ_BLOCK_SIZE = 65536  // 64 KB

    /**
     * Read log lines from [file] in newest-first order without loading the whole file.
     * Reads backwards in [READ_BLOCK_SIZE]-byte blocks so only [offset]+[limit] lines
     * are ever in memory at once — no OOM risk.
     */
    fun getLogLines(file: File, offset: Int, limit: Int): List<String> {
        if (!file.exists() || file.length() == 0L || limit <= 0) return emptyList()
        val needed = offset + limit
        val collected = mutableListOf<String>()

        RandomAccessFile(file, "r").use { raf ->
            var readPos = raf.length()
            var tailBytes = ByteArray(0)

            while (readPos > 0 && collected.size < needed) {
                val blockLen = minOf(READ_BLOCK_SIZE.toLong(), readPos).toInt()
                readPos -= blockLen
                raf.seek(readPos)
                val block = ByteArray(blockLen)
                raf.readFully(block)

                val combined = block + tailBytes
                var endIdx = combined.size

                for (i in combined.size - 1 downTo 0) {
                    if (combined[i] == '\n'.code.toByte()) {
                        if (i + 1 < endIdx) {
                            val line = String(combined, i + 1, endIdx - i - 1, Charsets.UTF_8).trimEnd('\r')
                            if (line.isNotEmpty()) {
                                collected.add(line)
                                if (collected.size >= needed) break
                            }
                        }
                        endIdx = i
                    }
                }

                tailBytes = if (endIdx > 0) combined.copyOfRange(0, endIdx) else ByteArray(0)
            }

            if (collected.size < needed && tailBytes.isNotEmpty()) {
                val line = String(tailBytes, Charsets.UTF_8).trimEnd('\r', '\n')
                if (line.isNotEmpty()) collected.add(line)
            }
        }

        return collected.drop(offset).take(limit)
    }

    fun getFileSize(context: Context): Long {
        val dir = File(DiskLogFormatStrategy.getLogFolder())
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
            val logFolder = DiskLogFormatStrategy.getLogFolder()
            val logFolderFile = File(logFolder)
            val crashReportFile = File(context.filesDir, "crash_report.txt")
            val crashLogFile = File(context.filesDir, "crash_log.txt")

            if (!logFolderFile.exists() && !crashReportFile.exists() && !crashLogFile.exists()) {
                DialogHelper.showMessage(Res.string.no_logs_error)
                return@coMain
            }

            DialogHelper.showLoading()
            val zipFile = File(context.cacheDir.absolutePath + "/logs.zip")
            val sourcePaths = buildList {
                if (logFolderFile.exists()) add(logFolder)
                if (crashReportFile.exists()) add(crashReportFile.absolutePath)
                if (crashLogFile.exists()) add(crashLogFile.absolutePath)
            }
            val success = withIO {
                ZipHelper.zip(sourcePaths, zipFile.absolutePath)
            }
            DialogHelper.hideLoading()
            if (!success) {
                DialogHelper.showErrorMessage(LocaleHelper.getString(Res.string.error))
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
        intent.putExtra(Intent.EXTRA_SUBJECT, LocaleHelper.getString(Res.string.share_logs) + " - PlainApp $appVersion")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.SUPPORT_EMAIL))
        intent.putExtra(Intent.EXTRA_TEXT, buildDeviceInfoText())
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, Constants.AUTHORITY, file))
        val chooserIntent = Intent.createChooser(intent, LocaleHelper.getString(Res.string.share_logs))
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