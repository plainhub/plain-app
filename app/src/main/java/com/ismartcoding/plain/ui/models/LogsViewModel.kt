package com.ismartcoding.plain.ui.models

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch
import org.zeroturnaround.zip.ZipUtil
import java.io.File

class LogsViewModel : ViewModel() {
    fun export(context: Context) {
        viewModelScope.launch {
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

    private fun share(
        context: Context,
        file: File,
    ) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_logs))
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.SUPPORT_EMAIL))
        intent.putExtra(Intent.EXTRA_TEXT, "")
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, Constants.AUTHORITY, file))
        context.startActivity(Intent.createChooser(intent, getString(R.string.share_logs)))
    }
}
