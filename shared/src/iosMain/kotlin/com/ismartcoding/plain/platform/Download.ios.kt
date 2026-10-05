package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.fclose
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.UniformTypeIdentifiers.UTType

@OptIn(ExperimentalForeignApi::class)
private class IosDownloadTempFileHandle(
    override val filePath: String,
) : DownloadTempFileHandle {
    private val stream = checkNotNull(fopen(filePath, "wb")) { "Unable to open download file" }
    private var closed = false

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        check(!closed) { "Download file closed" }
        require(offset >= 0 && length >= 0 && offset <= buffer.size - length)
        if (length == 0) return
        buffer.usePinned { pinned ->
            check(fwrite(pinned.addressOf(offset), 1.convert(), length.convert(), stream) == length.toULong()) {
                "Unable to write download file"
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        check(fclose(stream) == 0) { "Unable to close download file" }
    }

    override fun delete() {
        try { close() } finally {
            check(!NSFileManager.defaultManager.fileExistsAtPath(filePath) || NSFileManager.defaultManager.removeItemAtPath(filePath, null)) { "Unable to delete download file" }
        }
    }
}

actual fun createDownloadTempFile(taskId: String): DownloadTempFileHandle {
    val path = NSTemporaryDirectory() + "dl_${taskId}_${TimeHelper.nowMillis()}"
    return IosDownloadTempFileHandle(path)
}

actual fun getMimeTypeFromExtension(extension: String): String {
    val ext = extension.lowercase()
    val common = CommonMimeTypes[ext]
    if (common.isNotEmpty()) return common
    return try {
        val type = UTType.typeWithFilenameExtension(ext)
        if (type != null) {
            type.preferredMIMEType ?: ""
        } else {
            ""
        }
    } catch (e: Exception) {
        ""
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun createFileWriteHandle(filePath: String): DownloadTempFileHandle {
    val dir = filePath.substringBeforeLast('/', "")
    if (dir.isNotEmpty()) {
        NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
    }
    return IosDownloadTempFileHandle(filePath)
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun importDownloadedFile(handle: DownloadTempFileHandle, fileName: String, mimeType: String): String = withIO {
    val iosHandle = handle as? IosDownloadTempFileHandle ?: return@withIO ""
    iosHandle.close()
    val srcPath = iosHandle.filePath
    val srcName = srcPath.substringAfterLast('/')
    val destDir = appDir() + "/downloads"
    NSFileManager.defaultManager.createDirectoryAtPath(
        destDir, withIntermediateDirectories = true, attributes = null, error = null,
    )
    val destPath = "$destDir/$srcName"
    return@withIO try {
        NSFileManager.defaultManager.removeItemAtPath(destPath, null)
        if (NSFileManager.defaultManager.moveItemAtPath(srcPath, destPath, error = null)) {
            destPath
        } else {
            LogCat.e("importDownloadedFile: moveItemAtPath failed")
            ""
        }
    } catch (e: Exception) {
        LogCat.e("importDownloadedFile: ${e.message}")
        ""
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun saveTempFileToDownloads(handle: DownloadTempFileHandle, filename: String): String = withIO {
    val iosHandle = handle as? IosDownloadTempFileHandle ?: return@withIO ""
    iosHandle.close()
    val destDir = appDir() + "/downloads"
    NSFileManager.defaultManager.createDirectoryAtPath(
        destDir, withIntermediateDirectories = true, attributes = null, error = null,
    )
    val destPath = "$destDir/$filename"
    NSFileManager.defaultManager.removeItemAtPath(destPath, null)
    try {
        if (NSFileManager.defaultManager.moveItemAtPath(iosHandle.filePath, destPath, error = null)) {
            destPath
        } else {
            LogCat.e("saveTempFileToDownloads: moveItemAtPath failed")
            ""
        }
    } catch (e: Exception) {
        LogCat.e("saveTempFileToDownloads: ${e.message}")
        ""
    }
}

actual fun resolveAppFilePath(fidUri: String): String {
    if (fidUri.startsWith("fid:", ignoreCase = true)) {
        val fidSuffix = fidUri.removePrefix("fid:").removePrefix("FID:")
        val hash = fidSuffix.substringBefore(".")
        if (hash.length >= 4) {
            return "${appDir()}/${hash.substring(0, 2)}/${hash.substring(2, 4)}/$fidSuffix"
        }
        return "${appDir()}/$fidSuffix"
    }
    return fidUri
}
