package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.writeToFile
import platform.UniformTypeIdentifiers.UTType

@OptIn(ExperimentalForeignApi::class)
private class IosDownloadTempFileHandle(
    val filePath: String,
) : DownloadTempFileHandle {
    private val chunks = mutableListOf<ByteArray>()

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        chunks.add(buffer.copyOfRange(offset, offset + length))
    }

    override fun close() {
        val size = chunks.sumOf { it.size }
        val merged = ByteArray(size)
        var pos = 0
        for (chunk in chunks) {
            chunk.copyInto(merged, pos)
            pos += chunk.size
        }
        merged.toNSData().writeToFile(filePath, atomically = true)
    }

    override fun delete() {
        NSFileManager.defaultManager.removeItemAtPath(filePath, null)
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
actual suspend fun importDownloadedFile(handle: DownloadTempFileHandle, mimeType: String): String = withIO {
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
