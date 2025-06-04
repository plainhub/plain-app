package com.ismartcoding.plain.ui.models

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.queryOpenableFileName
import com.ismartcoding.lib.helpers.CoroutinesHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.contentResolver
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.commons.IOUtils
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupRestoreViewModel : ViewModel() {
    data class ExportItem(val dir: String, val file: File)

    fun backup(
        context: Context,
        uri: Uri,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            contentResolver.openOutputStream(uri)?.use { stream ->
                val out = ZipOutputStream(stream)
                try {
                    DialogHelper.showLoading()
                    val files =
                        arrayListOf(
                            ExportItem("/", File(context.dataDir.path + "/databases")),
                            ExportItem("/", context.filesDir),
                            ExportItem("/external/", context.getExternalFilesDir(null)!!),
                        )
                    for (i in files.indices) {
                        val item = files[i]
                        appendFile(out, item.dir, item.file)
                    }
                    val fileName = contentResolver.queryOpenableFileName(uri)
                    DialogHelper.hideLoading()
                    DialogHelper.showConfirmDialog("", LocaleHelper.getStringF(R.string.exported_to, "name", fileName))
                } finally {
                    IOUtils.closeQuietly(out)
                }
            }
        }
    }

    fun restore(
        context: Context,
        uri: Uri,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            val fileName = contentResolver.queryOpenableFileName(uri)
            if (!fileName.endsWith(".zip")) {
                DialogHelper.showMessage(R.string.invalid_file)
                DialogHelper.hideLoading()
                return@launch
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                val destFile = File(context.cacheDir, "restore")
                ZipUtil.unpack(stream, destFile)

                // restore database
                File(destFile.path + "/databases").let {
                    if (it.exists()) {
                        it.copyRecursively(File(context.dataDir.path + "/databases"), true)
                    }
                }

                // restore local storage
                File(destFile.path + "/files").let {
                    if (it.exists()) {
                        it.copyRecursively(context.filesDir, true)
                    }
                }

                // restore external files
                File(destFile.path + "/external/files").let {
                    if (it.exists()) {
                        it.copyRecursively(context.getExternalFilesDir(null)!!, true)
                    }
                }
                destFile.delete()
            }
            DialogHelper.hideLoading()
            DialogHelper.showConfirmDialog("", getString(R.string.app_restored)) {
                sendEvent(RestartAppEvent())
            }
        }
    }

    private suspend fun appendFile(
        out: ZipOutputStream,
        dir: String,
        file: File,
    ) {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                LogCat.e(it.path)
                appendFile(out, dir + file.name + "/", it)
            }
            return
        }
        val entry = ZipEntry(dir + file.name)
        entry.size = file.length()
        entry.time = file.lastModified()
        CoroutinesHelper.withIO {
            out.putNextEntry(entry)
        }
        val input =
            CoroutinesHelper.withIO {
                FileInputStream(file)
            }
        IOUtils.copy(input, out)
        CoroutinesHelper.withIO {
            out.closeEntry()
        }
    }
}
