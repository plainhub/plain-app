package com.ismartcoding.plain.platform

interface DownloadTempFileHandle {
    fun write(buffer: ByteArray, offset: Int, length: Int)
    fun close()
    fun delete()

    /** Absolute path of the temp file when the platform exposes one; empty otherwise. */
    val filePath: String
        get() = ""
}

expect fun createDownloadTempFile(taskId: String): DownloadTempFileHandle

expect fun getMimeTypeFromExtension(extension: String): String

expect suspend fun importDownloadedFile(handle: DownloadTempFileHandle, fileName: String, mimeType: String): String

/**
 * Moves a downloaded temp file ([handle]) to the system Downloads directory
 * with the given [filename]. The temp file is deleted on success.
 * Returns the absolute path of the saved file, or empty string on failure.
 *
 * Unlike [importDownloadedFile] (which imports to the app's internal
 * content-addressable store), this saves directly to the public Downloads
 * directory so the user can find the file in their file manager.
 */
expect suspend fun saveTempFileToDownloads(handle: DownloadTempFileHandle, filename: String): String

expect fun resolveAppFilePath(fidUri: String): String

/**
 * Streaming write handle for an arbitrary real filesystem path. Parent
 * directories are created as needed; close() completes the file.
 */
expect fun createFileWriteHandle(filePath: String): DownloadTempFileHandle

internal object CommonMimeTypes {
    operator fun get(extension: String): String = when (extension.lowercase()) {
        "txt" -> "text/plain"
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "mp4" -> "video/mp4"
        "mp3" -> "audio/mpeg"
        "zip" -> "application/zip"
        else -> ""
    }
}
