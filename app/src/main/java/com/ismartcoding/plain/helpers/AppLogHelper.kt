package com.ismartcoding.plain.helpers

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import org.zeroturnaround.zip.ZipUtil
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
            DialogHelper.showLoading()
            val zipFile = File(context.cacheDir.absolutePath + "/logs.zip")
            val folder = DiskLogFormatStrategy.getLogFolder(context)
            withIO {
                ZipUtil.pack(File(folder), zipFile)
            }
            DialogHelper.hideLoading()
            share(context, zipFile)
        }
    }

    private fun share(
        context: Context,
        file: File,
    ) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_SUBJECT, LocaleHelper.getString(R.string.share_logs))
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.SUPPORT_EMAIL))
        intent.putExtra(Intent.EXTRA_TEXT, "")
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, Constants.AUTHORITY, file))
        val chooserIntent = Intent.createChooser(intent, LocaleHelper.getString(R.string.share_logs))
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, ShareHelper.getExcludeComponentNames(context).toTypedArray())
        context.startActivity(chooserIntent)
    }
}