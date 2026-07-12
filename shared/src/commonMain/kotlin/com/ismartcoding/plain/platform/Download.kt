package com.ismartcoding.plain.platform

interface DownloadTempFileHandle {
    fun write(buffer: ByteArray, offset: Int, length: Int)
    fun close()
    fun delete()
}

expect fun createDownloadTempFile(taskId: String): DownloadTempFileHandle

expect fun getMimeTypeFromExtension(extension: String): String

expect suspend fun importDownloadedFile(handle: DownloadTempFileHandle, mimeType: String): String

expect fun resolveAppFilePath(fidUri: String): String

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
