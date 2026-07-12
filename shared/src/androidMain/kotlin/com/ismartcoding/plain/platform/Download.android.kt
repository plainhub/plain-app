package com.ismartcoding.plain.platform

import android.webkit.MimeTypeMap
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.plain.helpers.ChatFileSaveHelper
import java.io.File
import java.io.FileOutputStream

private class AndroidDownloadTempFileHandle(
    val file: File,
    private val outputStream: FileOutputStream,
) : DownloadTempFileHandle {
    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        outputStream.write(buffer, offset, length)
    }

    override fun close() {
        outputStream.close()
    }

    override fun delete() {
        if (file.exists()) file.delete()
    }
}

actual fun createDownloadTempFile(taskId: String): DownloadTempFileHandle {
    val file = File(appContext.cacheDir, "dl_${taskId}_${System.currentTimeMillis()}")
    file.parentFile?.mkdirs()
    file.createNewFile()
    return AndroidDownloadTempFileHandle(file, FileOutputStream(file))
}

actual fun getMimeTypeFromExtension(extension: String): String {
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""
}

actual suspend fun importDownloadedFile(handle: DownloadTempFileHandle, mimeType: String): String {
    val androidHandle = handle as AndroidDownloadTempFileHandle
    androidHandle.close()
    return ChatFileSaveHelper.importDownloadedFile(androidHandle.file, mimeType)
}

actual fun resolveAppFilePath(fidUri: String): String {
    return AppFileStore.resolveUri(fidUri)
}
