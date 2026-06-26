package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.extensions.appDir
import com.ismartcoding.plain.lib.extensions.queryOpenableFileName
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.contentResolver
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.helpers.ZipHelper
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupRestoreViewModel : ViewModel() {
    data class ExportItem(val dir: String, val file: File)

    /**
     * Used on Android 9 and below where ACTION_CREATE_DOCUMENT is broken on many OEM devices.
     * Writes the backup zip directly to app-specific external storage (no permission required).
     */
    fun backupToFile(context: Context, fileName: String) {
        launchSafe {
            DialogHelper.showLoading()
            try {
                val tmpFile = File(context.cacheDir, fileName)
                ZipOutputStream(FileOutputStream(tmpFile)).use { out ->
                    writeBackupContent(out, context)
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

    fun backup(context: Context, uri: Uri) {
        launchSafe {
            DialogHelper.showLoading()
            try {
                val stream = contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Failed to open output stream")
                ZipOutputStream(stream).use { out ->
                    writeBackupContent(out, context)
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

    fun restore(context: Context, uri: Uri) {
        launchSafe {
            DialogHelper.showLoading()
            try {
                val fileName = contentResolver.queryOpenableFileName(uri)
                if (!fileName.endsWith(".zip")) {
                    DialogHelper.hideLoading()
                    DialogHelper.showMessage(Res.string.invalid_file)
                    return@launchSafe
                }
                contentResolver.openInputStream(uri)?.use { stream ->
                    val destFile = File(context.cacheDir, "restore")
                    if (destFile.exists()) {
                        destFile.deleteRecursively()
                    }
                    val success = ZipHelper.unzip(stream, destFile)
                    if (!success) {
                        throw IllegalStateException("Failed to unzip backup file")
                    }

                    File(destFile.path + "/databases").let {
                        if (it.exists()) it.copyRecursively(File(context.dataDir.path + "/databases"), true)
                    }
                    File(destFile.path + "/files").let {
                        if (it.exists()) it.copyRecursively(context.filesDir, true)
                    }
                    File(destFile.path + "/external/files").let {
                        if (it.exists()) it.copyRecursively(File(context.appDir()), true)
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

    private fun writeBackupContent(out: ZipOutputStream, context: Context) {
        val items = listOf(
            ExportItem("/", File(context.dataDir.path + "/databases")),
            ExportItem("/", context.filesDir),
            ExportItem("/external/", File(context.appDir())),
        )
        for (item in items) {
            appendFile(out, item.dir, item.file)
        }
    }

    private fun appendFile(out: ZipOutputStream, dir: String, file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                appendFile(out, dir + file.name + "/", it)
            }
            return
        }
        // Skip non-regular files (sockets, pipes, device nodes, etc.).
        // FileInputStream on a socket/pipe would block forever.
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
}
