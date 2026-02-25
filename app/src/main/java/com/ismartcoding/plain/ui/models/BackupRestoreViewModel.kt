package com.ismartcoding.plain.ui.models

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.queryOpenableFileName
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.contentResolver
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.commons.IOUtils
import java.io.File
import java.io.FileInputStream
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
        viewModelScope.launch(Dispatchers.IO) {
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
                DialogHelper.showConfirmDialog("", LocaleHelper.getStringF(R.string.exported_to, "name", destFile.absolutePath))
            } catch (e: Throwable) {
                LogCat.e("Backup failed: ${e.message}")
                DialogHelper.hideLoading()
                DialogHelper.showMessage(e.message ?: getString(R.string.error))
            }
        }
    }

    fun backup(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            try {
                val stream = contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Failed to open output stream")
                ZipOutputStream(stream).use { out ->
                    writeBackupContent(out, context)
                }
                val fileName = contentResolver.queryOpenableFileName(uri)
                DialogHelper.hideLoading()
                DialogHelper.showConfirmDialog("", LocaleHelper.getStringF(R.string.exported_to, "name", fileName))
            } catch (e: Throwable) {
                LogCat.e("Backup failed: ${e.message}")
                DialogHelper.hideLoading()
                DialogHelper.showMessage(e.message ?: getString(R.string.error))
            }
        }
    }

    fun restore(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            try {
                val fileName = contentResolver.queryOpenableFileName(uri)
                if (!fileName.endsWith(".zip")) {
                    DialogHelper.hideLoading()
                    DialogHelper.showMessage(R.string.invalid_file)
                    return@launch
                }
                contentResolver.openInputStream(uri)?.use { stream ->
                    val destFile = File(context.cacheDir, "restore")
                    ZipUtil.unpack(stream, destFile)

                    File(destFile.path + "/databases").let {
                        if (it.exists()) it.copyRecursively(File(context.dataDir.path + "/databases"), true)
                    }
                    File(destFile.path + "/files").let {
                        if (it.exists()) it.copyRecursively(context.filesDir, true)
                    }
                    File(destFile.path + "/external/files").let {
                        if (it.exists()) it.copyRecursively(context.getExternalFilesDir(null)!!, true)
                    }
                    destFile.deleteRecursively()
                }
                DialogHelper.hideLoading()
                DialogHelper.showConfirmDialog("", getString(R.string.app_restored)) {
                    sendEvent(RestartAppEvent())
                }
            } catch (e: Throwable) {
                LogCat.e("Restore failed: ${e.message}")
                DialogHelper.hideLoading()
                DialogHelper.showMessage(e.message ?: getString(R.string.error))
            }
        }
    }

    private fun writeBackupContent(out: ZipOutputStream, context: Context) {
        val items = listOf(
            ExportItem("/", File(context.dataDir.path + "/databases")),
            ExportItem("/", context.filesDir),
            ExportItem("/external/", context.getExternalFilesDir(null)!!),
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
            FileInputStream(file).use { input ->
                IOUtils.copy(input, out)
            }
            out.closeEntry()
        } catch (e: Exception) {
            LogCat.w("Skipping file ${file.path}: ${e.message}")
        }
    }
}
