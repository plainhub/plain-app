package com.ismartcoding.plain.helpers

import android.content.Context
import android.net.Uri
import java.io.File

object ChatFileSaveHelper {
    /**
     * Import a file from a content-resolver [uri] into the content-addressable
     * chat file store with two-step dedup.
     *
     * Returns a `fid:{sha256}` URI to embed in [com.ismartcoding.plain.db.DMessageFile.uri].
     */
    fun importFromUri(
        context: Context,
        uri: Uri,
        mimeType: String = "",
    ): String {
        val tempFile = File(context.cacheDir, "chat_import_${System.currentTimeMillis()}_${Thread.currentThread().id}")
        tempFile.parentFile?.mkdirs()
        try {
            FileHelper.copyFile(context, uri, tempFile.absolutePath)
            val dFile = AppFileStore.importFile(context, tempFile, mimeType, deleteSrc = true)
            return AppFileStore.toFidUri(dFile.id)
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
    fun importDownloadedFile(
        context: Context,
        srcFile: File,
        mimeType: String = "",
    ): String {
        val dFile = AppFileStore.importFile(context, srcFile, mimeType, deleteSrc = true)
        return AppFileStore.toFidUri(dFile.id)
    }
}