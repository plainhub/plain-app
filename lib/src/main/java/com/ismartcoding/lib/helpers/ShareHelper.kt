package com.ismartcoding.lib.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ismartcoding.lib.extensions.getMediaContentUri
import com.ismartcoding.lib.extensions.getMimeType
import com.ismartcoding.lib.extensions.getMimeTypeFromUri
import kotlin.collections.ArrayList

object ShareHelper {
    fun share(
        context: Context,
        uri: Uri,
    ) {
        context.startActivity(
            Intent.createChooser(
                createFileIntent(context, uri),
                null,
            ),
        )
    }

    fun shareText(
        context: Context,
        content: String,
    ) {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(
                        Intent.EXTRA_TEXT,
                        content,
                    )
                    type = "text/plain"
                },
                null,
            ),
        )
    }

    fun sharePaths(
        context: Context,
        paths: List<String>,
    ) {
        share(context, ArrayList(paths.map { Uri.parse(it) }))
    }

    fun share(
        context: Context,
        uris: ArrayList<Uri>,
    ) {
        if (uris.size == 1) {
            share(context, uris[0])
        } else {
            shareFiles(context, uris)
        }
    }

    private fun shareFiles(
        context: Context,
        uris: ArrayList<Uri>,
    ) {
        context.startActivity(
            Intent.createChooser(
                createFilesIntent(context, uris),
                null,
            ),
        )
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
        uris: ArrayList<Uri>,
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
}
