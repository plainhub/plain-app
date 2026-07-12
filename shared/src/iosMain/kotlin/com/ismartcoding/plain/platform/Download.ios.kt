package com.ismartcoding.plain.platform

private class IosDownloadTempFileHandle : DownloadTempFileHandle {
    val bytes = mutableListOf<Byte>()

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        for (i in offset until offset + length) {
            bytes.add(buffer[i])
        }
    }

    override fun close() {}

    override fun delete() {
        bytes.clear()
    }
}

actual fun createDownloadTempFile(taskId: String): DownloadTempFileHandle {
    return IosDownloadTempFileHandle()
}

actual fun getMimeTypeFromExtension(extension: String): String {
    return CommonMimeTypes[extension]
}

actual suspend fun importDownloadedFile(handle: DownloadTempFileHandle, mimeType: String): String {
    handle.close()
    return ""
}

actual fun resolveAppFilePath(fidUri: String): String {
    return ""
}
