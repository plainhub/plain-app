package com.ismartcoding.plain.ui.app

import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.contentResolver
import com.ismartcoding.plain.features.ExportFileEvent
import com.ismartcoding.plain.features.ExportFileResultEvent
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.data.enums.ExportFileType
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.data.enums.PickFileType
import com.ismartcoding.plain.databinding.DialogBackupRestoreBinding
import com.ismartcoding.plain.extensions.formatName
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.commons.IOUtils
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class BackupRestoreDialog : BaseBottomSheetDialog<DialogBackupRestoreBinding>() {
    data class ExportItem(val dir: String, val file: File)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backup.setSafeClick {
            sendEvent(ExportFileEvent(ExportFileType.BACKUP, "backup_" + Date().formatName() + ".zip"))
        }

        binding.restore.setSafeClick {
            sendEvent(PickFileEvent(PickFileTag.RESTORE, PickFileType.FILE, multiple = false))
        }

        receiveEvent<PickFileResultEvent> { event ->
            if (event.tag != PickFileTag.RESTORE) {
                return@receiveEvent
            }
            val context = requireContext()
            val uri = event.uris.first()
            DialogHelper.showLoading()
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val fileName = cursor.getStringValue(OpenableColumns.DISPLAY_NAME)
                    if (!fileName.endsWith(".zip")) {
                        DialogHelper.showMessage(R.string.invalid_file)
                        DialogHelper.hideLoading()
                        return@receiveEvent
                    }
                    contentResolver.openInputStream(uri)?.use { stream ->
                        withIO {
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
                        dismiss()
                        DialogHelper.showConfirmDialog(context, "", getString(R.string.app_restored)) {
                            val packageManager = context.packageManager
                            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                            val componentName = intent!!.component
                            val mainIntent = Intent.makeRestartActivityTask(componentName)
                            context.startActivity(mainIntent)
                            Runtime.getRuntime().exit(0)
                        }
                    }
                }
            }
        }

        receiveEvent<ExportFileResultEvent> { event ->
            if (event.type == ExportFileType.BACKUP) {
                val context = requireContext()
                contentResolver.openOutputStream(event.uri)?.use { stream ->
                    val out = ZipOutputStream(stream)
                    try {
                        DialogHelper.showLoading()
                        withIO {
                            val files = arrayListOf(
                                ExportItem("/", File(context.dataDir.path + "/databases")),
                                ExportItem("/", context.filesDir),
                                ExportItem("/external/", context.getExternalFilesDir(null)!!)
                            )
                            for (i in files.indices) {
                                val item = files[i]
                                appendFile(out, item.dir, item.file)
                            }
                        }
                        contentResolver.query(event.uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val fileName = cursor.getStringValue(OpenableColumns.DISPLAY_NAME)
                                DialogHelper.hideLoading()
                                dismiss()
                                DialogHelper.showConfirmDialog(requireContext(), "", LocaleHelper.getStringF(R.string.exported_to, "name", fileName))
                            }
                        }
                    } finally {
                        IOUtils.closeQuietly(out)
                    }
                }

            }
        }
    }

    private suspend fun appendFile(out: ZipOutputStream, dir: String, file: File) {
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
        withContext(Dispatchers.IO) {
            out.putNextEntry(entry)
        }
        val input = withContext(Dispatchers.IO) {
            FileInputStream(file)
        }
        IOUtils.copy(input, out)
        withContext(Dispatchers.IO) {
            out.closeEntry()
        }
    }
}