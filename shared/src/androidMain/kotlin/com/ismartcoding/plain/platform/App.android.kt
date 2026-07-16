package com.ismartcoding.plain.platform

import android.net.Uri
import android.os.Environment
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.contentResolver
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.AppLogHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.ZipHelper
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.extensions.appDir
import com.ismartcoding.plain.lib.extensions.queryOpenableFileName
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.ui.helpers.DialogHelper
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

actual fun getCacheSize(): Long = AppHelper.getCacheSize(appContext)
actual fun getLogFileSize(): Long = AppLogHelper.getFileSize(appContext)
actual fun isAppForegrounded(): Boolean = AppHelper.foregrounded()
actual fun getAppVersionName(): String = com.ismartcoding.plain.getAppVersionName()
actual fun installApk(path: String) = PackageHelper.install(appContext, File(path))
actual fun exitApp() {
    exitProcess(0)
}

actual fun clearCacheAsync() = AppHelper.clearCacheAsync(appContext)

actual fun clearLogsAsync() {
    val dir = File(com.ismartcoding.plain.platform.DiskLogFormatStrategy.getLogFolder())
    if (dir.exists()) dir.deleteRecursively()
}

actual fun exportLogsAsync() = AppLogHelper.export(appContext)

actual fun clearImageMemoryCache() {
    coil3.SingletonImageLoader.get(appContext).memoryCache?.clear()
}

actual suspend fun checkUpdateAsync(showToast: Boolean): Boolean? =
    AppHelper.checkUpdateAsync(appContext, showToast)

actual fun backup(uriStr: String) {
    coIO {
        DialogHelper.showLoading()
        try {
            val uri = Uri.parse(uriStr)
            val stream = contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Failed to open output stream")
            ZipOutputStream(stream).use { out ->
                writeBackupContent(out)
            }
            val fileName = contentResolver.queryOpenableFileName(uri)
            DialogHelper.hideLoading()
            DialogHelper.showConfirmDialog("", LocaleHelper.getStringFAsync(Res.string.exported_to, "name", fileName))
        } catch (e: Throwable) {
            LogCat.e("Backup failed: ${e.message}")
            DialogHelper.hideLoading()
            DialogHelper.showMessage(e.message ?: LocaleHelper.getStringAsync(Res.string.error))
        }
    }
}

actual fun restore(uriStr: String) {
    coIO {
        DialogHelper.showLoading()
        try {
            val uri = Uri.parse(uriStr)
            val fileName = contentResolver.queryOpenableFileName(uri)
            if (!fileName.endsWith(".zip")) {
                DialogHelper.hideLoading()
                DialogHelper.showMessage(Res.string.invalid_file)
                return@coIO
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                val destFile = File(appContext.cacheDir, "restore")
                if (destFile.exists()) {
                    destFile.deleteRecursively()
                }
                val success = ZipHelper.unzip(stream, destFile)
                if (!success) {
                    throw IllegalStateException("Failed to unzip backup file")
                }

                File(destFile.path + "/databases").let {
                    if (it.exists()) it.copyRecursively(File(appContext.dataDir.path + "/databases"), true)
                }
                File(destFile.path + "/files").let {
                    if (it.exists()) it.copyRecursively(appContext.filesDir, true)
                }
                File(destFile.path + "/external/files").let {
                    if (it.exists()) it.copyRecursively(File(appContext.appDir()), true)
                }
                destFile.deleteRecursively()
            }
            DialogHelper.hideLoading()
            DialogHelper.showConfirmDialog("", LocaleHelper.getStringAsync(Res.string.app_restored)) {
                sendEvent(RestartAppEvent())
            }
        } catch (e: Throwable) {
            LogCat.e("Restore failed: ${e.message}")
            DialogHelper.hideLoading()
            DialogHelper.showMessage(e.message ?: LocaleHelper.getStringAsync(Res.string.error))
        }
    }
}

actual fun backupToFile(fileName: String) {
    coIO {
        DialogHelper.showLoading()
        try {
            val tmpFile = File(appContext.cacheDir, fileName)
            ZipOutputStream(FileOutputStream(tmpFile)).use { out ->
                writeBackupContent(out)
            }
            val destFile = FileHelper.createPublicFile(fileName, Environment.DIRECTORY_DOWNLOADS)
            tmpFile.copyTo(destFile, overwrite = true)
            tmpFile.delete()
            DialogHelper.hideLoading()
            DialogHelper.showConfirmDialog("", LocaleHelper.getStringFAsync(Res.string.exported_to, "name", destFile.absolutePath))
        } catch (e: Throwable) {
            LogCat.e("Backup failed: ${e.message}")
            DialogHelper.hideLoading()
            DialogHelper.showMessage(e.message ?: LocaleHelper.getStringAsync(Res.string.error))
        }
    }
}

private fun writeBackupContent(out: ZipOutputStream) {
    val items = listOf(
        BackupExportItem("/", File(appContext.dataDir.path + "/databases")),
        BackupExportItem("/", appContext.filesDir),
        BackupExportItem("/external/", File(appContext.appDir())),
    )
    for (item in items) {
        appendBackupFile(out, item.dir, item.file)
    }
}

private data class BackupExportItem(val dir: String, val file: File)

private fun appendBackupFile(out: ZipOutputStream, dir: String, file: File) {
    if (file.isDirectory) {
        file.listFiles()?.forEach {
            appendBackupFile(out, dir + file.name + "/", it)
        }
        return
    }
    if (!file.isFile) return
    try {
        val entry = ZipEntry(dir + file.name)
        entry.time = file.lastModified()
        out.putNextEntry(entry)
        file.inputStream().use { input ->
            input.copyTo(out)
        }
        out.closeEntry()
    } catch (e: Exception) {
        LogCat.w("Skipping file ${file.path}: ${e.message}")
    }
}

actual fun getAppIcon(packageName: String): Any? = runCatching {
    appContext.packageManager.getApplicationIcon(packageName)
}.getOrNull()

actual fun isIgnoringBatteryOptimizations(): Boolean =
    com.ismartcoding.plain.powerManager.isIgnoringBatteryOptimizations(appContext.packageName)

actual fun openBatteryOptimizationSettings() {
    val intent = android.content.Intent().apply {
        action = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    appContext.startActivity(intent)
}
