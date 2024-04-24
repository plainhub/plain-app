package com.ismartcoding.plain.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ismartcoding.lib.extensions.getMediaContentUri
import com.ismartcoding.lib.extensions.getMimeType
import com.ismartcoding.lib.extensions.getMimeTypeFromUri
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.MainActivity
import java.io.File

object ShareHelper {
    fun share(
        context: Context,
        uri: Uri,
    ) {
        if (uri.scheme == "file") {
            shareFile(context, File(uri.path!!))
            return
        }
        val shareIntent = createFileIntent(context, uri)
        val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share))
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, getExcludeComponentNames(context).toTypedArray())
        context.startActivity(chooserIntent)
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
        share(context, paths.map { Uri.parse(it) })
    }

    fun share(
        context: Context,
        uris: List<Uri>,
    ) {
        if (uris.size == 1) {
            share(context, uris[0])
        } else {
            shareFiles(context, uris)
        }
    }

    private fun shareFiles(
        context: Context,
        uris: List<Uri>,
    ) {
        val shareIntent = createFilesIntent(context, uris)
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
            val path = uri.toString()
            var newUri = uri
            if (!path.startsWith("content://")) {
                context.getMediaContentUri(path)?.let {
                    newUri = it
                }
            }
            putExtra(
                Intent.EXTRA_STREAM,
                newUri,
            )
            var mimeType = path.getMimeType()
            if (mimeType.isEmpty()) {
                mimeType = context.getMimeTypeFromUri(uri)
            }
            type = mimeType
        }
    }

    private fun createFilesIntent(
        context: Context,
        uris: List<Uri>,
    ): Intent {
        return Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            val newUris = mutableListOf<Uri>()
            val paths = mutableListOf<String>()
            uris.forEach { uri ->
                val path = uri.toString()
                var newUri = uri
                if (!path.startsWith("content://")) {
                    context.getMediaContentUri(path)?.let {
                        newUri = it
                    }
                }
                newUris.add(newUri)
                paths.add(path)
            }
            type = paths.getMimeType()
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(newUris))
        }
    }

    fun shareFile(
        context: Context,
        file: File,
    ) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, Constants.AUTHORITY, file))
        val chooserIntent = Intent.createChooser(intent, getString(R.string.share))
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, getExcludeComponentNames(context).toTypedArray())
        context.startActivity(chooserIntent)
    }
}
