package com.ismartcoding.plain.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ismartcoding.lib.extensions.getMimeType
import com.ismartcoding.lib.extensions.getMimeTypeFromUri
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.MainActivity
import java.io.File

object ShareHelper {
    fun shareUri(
        context: Context,
        uri: Uri,
    ) {
        val shareIntent = createFileIntent(context, uri)
        val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share))
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, getExcludeComponentNames(context).toTypedArray())
        context.startActivity(chooserIntent)
    }

    fun shareUris(
        context: Context,
        uris: List<Uri>,
    ) {
        if (uris.size == 1) {
            shareUri(context, uris[0])
        } else {
            shareFileUris(context, uris)
        }
    }

    fun shareText(
        context: Context,
        content: String,
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(
                Intent.EXTRA_TEXT,
                content,
            )
            type = "text/plain"
        }
        val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share))
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, getExcludeComponentNames(context).toTypedArray())
        context.startActivity(chooserIntent)
    }

    fun sharePaths(
        context: Context,
        paths: Set<String>,
    ) {
        if (paths.size == 1) {
            shareFile(context, File(paths.first()))
        } else {
            shareFiles(context, paths.map { File(it) })
        }
    }

    private fun shareFileUris(
        context: Context,
        uris: List<Uri>,
    ) {
        val shareIntent = createFilesIntent(uris)
        val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share))
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, getExcludeComponentNames(context).toTypedArray())
        context.startActivity(chooserIntent)
    }

    fun getExcludeComponentNames(context: Context): List<ComponentName> {
        return listOf(ComponentName(context, MainActivity::class.java))
    }

    private fun createFileIntent(
        context: Context,
        uri: Uri,
    ): Intent {
        return Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, uri)
            type = context.getMimeTypeFromUri(uri)
        }
    }

    private fun createFilesIntent(
        uris: List<Uri>,
    ): Intent {
        return Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            type = "*/*"
        }
    }

    fun shareFile(
        context: Context,
        file: File
    ) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = file.path.getMimeType()
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, Constants.AUTHORITY, file))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooserIntent = Intent.createChooser(intent, getString(R.string.share))
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, getExcludeComponentNames(context).toTypedArray())
        context.startActivity(chooserIntent)
    }

    fun shareFiles(
        context: Context,
        files: List<File>,
    ) {
        val fileUris = arrayListOf<Uri>()

        for (file in files) {
            fileUris.add(FileProvider.getUriForFile(context, Constants.AUTHORITY, file))
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        intent.type = "*/*"
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooserIntent = Intent.createChooser(intent, getString(R.string.share))
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, getExcludeComponentNames(context).toTypedArray())
        context.startActivity(chooserIntent)
    }

    fun openPathWith(
        context: Context,
        path: String,
    ) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        val uri = FileProvider.getUriForFile(context, Constants.AUTHORITY, File(path))
        val mimeType = path.getMimeType()
        intent.setDataAndType(uri, mimeType)
        intent.putExtra("mimeType", mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooserIntent = Intent.createChooser(intent, getString(R.string.open_with))
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, getExcludeComponentNames(context).toTypedArray())
        context.startActivity(chooserIntent)
    }
}
