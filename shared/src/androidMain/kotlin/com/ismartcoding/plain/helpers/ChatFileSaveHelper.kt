package com.ismartcoding.plain.helpers

import android.content.Context
import android.net.Uri
import com.ismartcoding.plain.lib.extensions.queryOpenableFileName
import java.io.File


object ChatFileSaveHelper {
    /**
     * Import a file from a content-resolver [uri] into the content-addressable
     * chat file store with two-step dedup.
     *
     * Returns a `fid:{sha256}` URI to embed in [com.ismartcoding.plain.db.DMessageFile.uri].
     */
    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        mimeType: String = "",
    ): String {
        val tempFile = File(context.cacheDir, "chat_import_${System.currentTimeMillis()}_${Thread.currentThread().id}")
        tempFile.parentFile?.mkdirs()
        try {
            FileHelper.copyFile(context, uri, tempFile.absolutePath)
            val fileName = context.contentResolver.queryOpenableFileName(uri)
            val dFile = AppFileStore.importFile(tempFile.absolutePath, fileName, mimeType, deleteSrc = true)
            return AppFileStore.toFidUri(dFile)
        } finally {
            // Guard: if importFile did not consume (due to error path), clean up
            if (tempFile.exists()) tempFile.delete()
        }
    }

    /**
     * Import an already-downloaded / on-disk [srcFile] into the content-addressable
     * chat file store.
     *
     * [srcFile] is deleted on success (move semantics via [deleteSrc]).
     * Returns a `fid:{sha256}` URI to embed in [com.ismartcoding.plain.db.DMessageFile.uri].
     */
    suspend fun importDownloadedFile(
        srcFile: File,
        fileName: String,
        mimeType: String,
    ): String {
        val dFile = AppFileStore.importFile(srcFile.absolutePath, fileName, mimeType, deleteSrc = true)
        return AppFileStore.toFidUri(dFile)
    }
}