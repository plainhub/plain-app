package com.ismartcoding.plain.ui.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogLogsBinding
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setKeyText
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.extensions.setValueText
import com.ismartcoding.plain.ui.helpers.DialogHelper
import org.zeroturnaround.zip.ZipUtil
import java.io.File


class LogsDialog : BaseBottomSheetDialog<DialogLogsBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
    }

    private fun updateUI() {
        val context = requireContext()
        val fileSize = getFileSize(context)

        binding.fileSize.setKeyText(R.string.file_size)
        binding.fileSize.setValueText(FormatHelper.formatBytes(fileSize))
        binding.share.setSafeClick {
            if (fileSize == 0L) {
                DialogHelper.showMessage(getString(R.string.no_logs_error))
                return@setSafeClick
            }

            coMain {
                DialogHelper.showLoading()
                val zipFile = File(context.filesDir.absolutePath + "/logs.zip")
                val folder = DiskLogFormatStrategy.getLogFolder(context)
                withIO {
                    ZipUtil.pack(File(folder), zipFile)
                }
                DialogHelper.hideLoading()
                share(context, zipFile)
            }

        }

        binding.clear.setSafeClick {
            DialogHelper.confirmToAction(context, R.string.confirm_to_clear_logs) {
                val dir = File(DiskLogFormatStrategy.getLogFolder(context))
                if (dir.exists()) {
                    dir.deleteRecursively()
                    updateUI()
                }
            }
        }
    }

    private fun share(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_logs))
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.SUPPORT_EMAIL))
        intent.putExtra(Intent.EXTRA_TEXT, "")
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, Constants.AUTHORITY, file))
        context.startActivity(Intent.createChooser(intent, getString(R.string.share_logs)))
    }

    private fun getFileSize(context: Context): Long {
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
}