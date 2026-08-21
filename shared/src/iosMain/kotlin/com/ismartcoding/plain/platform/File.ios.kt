@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.plain.helpers.FileHashHelper
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.extensions.isAnimatedImageOrSvgHeader
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.isHeifHeader
import com.ismartcoding.plain.lib.toNSData
import com.ismartcoding.plain.thumbnail.DecodePolicy
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.httpserver.http.StreamSink
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSLog
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
actual fun deleteFileAt(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun fileSize(path: String): Long =
    (NSFileManager.defaultManager.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? Long) ?: 0L

@OptIn(ExperimentalForeignApi::class)
actual fun copyFile(srcPath: String, destPath: String): Boolean {
    val mgr = NSFileManager.defaultManager
    if (mgr.fileExistsAtPath(destPath)) {
        mgr.removeItemAtPath(destPath, null)
    }
    return mgr.copyItemAtPath(srcPath, destPath, error = null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun moveFile(fromPath: String, toPath: String): Boolean =
    NSFileManager.defaultManager.moveItemAtPath(fromPath, toPath, error = null)

@OptIn(ExperimentalForeignApi::class)
actual suspend fun sha256File(path: String): String =
    NSFileManager.defaultManager.contentsAtPath(path)?.toByteArray()?.let { FileHashHelper.strongHash(it) } ?: ""

@OptIn(ExperimentalForeignApi::class)
actual suspend fun sha256FileEdges(path: String, size: Long): String =
    NSFileManager.defaultManager.contentsAtPath(path)?.toByteArray()?.let { FileHashHelper.weakHash(it) } ?: ""

@OptIn(ExperimentalForeignApi::class)
actual fun writeBytesToPath(path: String, bytes: ByteArray): Boolean {
    return try {
        bytes.toNSData().writeToFile(path, atomically = true)
    } catch (_: Exception) {
        false
    }
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
actual fun createLongTextFile(text: String): DMessageContent {
    val timestamp = TimeHelper.now().toEpochMilliseconds()
    val fileName = "message-$timestamp.txt"
    val dir = appDir() + "/Documents"
    NSFileManager.defaultManager.createDirectoryAtPath(
        dir, withIntermediateDirectories = true, attributes = null, error = null,
    )
    val file = "$dir/$fileName"
    NSString.create(string = text)?.writeToFile(file, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    val size = NSFileManager.defaultManager.contentsAtPath(file)?.length?.toInt()?.toLong() ?: 0L
    return buildLongTextMessage(file, fileName, text, size)
}

@OptIn(ExperimentalForeignApi::class)
actual fun saveFileToDownloads(path: String, fileName: String): String {
    val destDir = appDir() + "/Downloads"
    NSFileManager.defaultManager.createDirectoryAtPath(
        destDir, withIntermediateDirectories = true, attributes = null, error = null,
    )
    val destPath = "$destDir/$fileName"
    return try {
        if (NSFileManager.defaultManager.fileExistsAtPath(destPath)) {
            NSFileManager.defaultManager.removeItemAtPath(destPath, null)
        }
        if (NSFileManager.defaultManager.copyItemAtPath(path, destPath, error = null)) {
            destPath
        } else {
            ""
        }
    } catch (e: Exception) {
        LogCat.e("saveFileToDownloads: ${e.message}")
        ""
    }
}

actual fun fileToUriString(path: String): String = path

actual fun getFileIconPath(extension: String): String = ""

@OptIn(ExperimentalForeignApi::class)
actual fun fileExists(path: String): Boolean =
    NSFileManager.defaultManager.fileExistsAtPath(path)

@OptIn(ExperimentalForeignApi::class)
actual suspend fun copyPickedFileToAppStorage(uriStr: String, destRelativePath: String): String? = withIO {
    val srcPath = uriStr
    if (!NSFileManager.defaultManager.fileExistsAtPath(srcPath)) return@withIO null
    val destPath = appDir() + "/" + destRelativePath
    val parent = destPath.substringBeforeLast('/', "")
    if (parent.isNotEmpty()) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            parent, withIntermediateDirectories = true, attributes = null, error = null,
        )
    }
    val displayName = srcPath.substringAfterLast('/')
    try {
        if (NSFileManager.defaultManager.copyItemAtPath(srcPath, destPath, error = null)) {
            displayName
        } else {
            null
        }
    } catch (e: Exception) {
        LogCat.e("copyPickedFileToAppStorage: ${e.message}")
        null
    }
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
actual fun writeFileText(path: String, content: String, overwrite: Boolean): DFile {
    if (!overwrite && NSFileManager.defaultManager.fileExistsAtPath(path)) {
        throw com.ismartcoding.plain.lib.kgraphql.GraphQLError("File already exists")
    }
    val parent = path.substringBeforeLast('/', "")
    if (parent.isNotEmpty()) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            parent, withIntermediateDirectories = true, attributes = null, error = null,
        )
    }
    NSString.create(string = content)?.writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    val size = NSFileManager.defaultManager.contentsAtPath(path)?.length?.toInt()?.toLong() ?: 0L
    return buildTextFile(path, size, TimeHelper.nowMillis())
}

actual fun getUploadTmpDirPath(): String =
    appDir() + "/upload_tmp"

actual fun getUploadCacheMergeDirPath(): String =
    appDir() + "/upload_merge"

@OptIn(ExperimentalForeignApi::class)
actual fun listFilesInDir(path: String): List<String> =
    NSFileManager.defaultManager.contentsOfDirectoryAtPath(path, null)
        ?.filterIsInstance<String>()
        ?: emptyList()

@OptIn(ExperimentalForeignApi::class)
actual fun deleteDirRecursively(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun streamFileTo(path: String, sink: StreamSink): Boolean = withIO {
    try {
        val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return@withIO false
        val bytes = data.toByteArray()
        val bufferSize = 64 * 1024
        var offset = 0
        while (offset < bytes.size) {
            val len = minOf(bufferSize, bytes.size - offset)
            sink.write(bytes, offset, len)
            offset += len
        }
        true
    } catch (e: Exception) {
        LogCat.e("streamFileTo: ${e.message}")
        false
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun readFileRange(path: String, offset: Long, length: Int): ByteArray? = withIO {
    try {
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return@withIO null
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null) ?: return@withIO null
        val fileLen = (attrs[NSFileSize] as? Long) ?: return@withIO null
        if (offset >= fileLen) return@withIO ByteArray(0)
        val readLen = minOf(length.toLong(), fileLen - offset).toInt()
        if (readLen <= 0) return@withIO ByteArray(0)
        val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return@withIO null
        val bytes = data.toByteArray()
        bytes.copyOfRange(offset.toInt(), offset.toInt() + readLen)
    } catch (e: Exception) {
        LogCat.e("readFileRange: ${e.message}")
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun createFileSink(path: String): StreamSink = withIO {
    val parent = path.substringBeforeLast('/', "")
    if (parent.isNotEmpty()) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            parent, withIntermediateDirectories = true, attributes = null, error = null,
        )
    }
    val destPath = path
    object : StreamSink {
        private val chunks = mutableListOf<ByteArray>()

        override suspend fun write(bytes: ByteArray) = withIO {
            if (bytes.isNotEmpty()) {
                chunks.add(bytes)
            }
        }

        override suspend fun write(bytes: ByteArray, offset: Int, length: Int) = withIO {
            if (length > 0) {
                chunks.add(bytes.copyOfRange(offset, offset + length))
            }
        }

        override suspend fun flush() {
            withIO {
                writeToDisk()
            }
        }

        override suspend fun close() {
            withIO {
                writeToDisk()
            }
        }

        private fun writeToDisk() {
            val size = chunks.sumOf { it.size }
            val merged = ByteArray(size)
            var pos = 0
            for (chunk in chunks) {
                chunk.copyInto(merged, pos)
                pos += chunk.size
            }
            merged.toNSData().writeToFile(destPath, atomically = true)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun renameFileAtomic(from: String, to: String): Boolean = withIO {
    val mgr = NSFileManager.defaultManager
    if (!mgr.fileExistsAtPath(from)) return@withIO false
    if (mgr.fileExistsAtPath(to)) {
        mgr.removeItemAtPath(to, null)
    }
    val parent = to.substringBeforeLast('/', "")
    if (parent.isNotEmpty()) {
        mgr.createDirectoryAtPath(
            parent, withIntermediateDirectories = true, attributes = null, error = null,
        )
    }
    try {
        mgr.moveItemAtPath(from, to, error = null)
    } catch (e: Exception) {
        LogCat.e("renameFileAtomic: ${e.message}")
        false
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun ensureParentDir(path: String) {
    withIO {
        val parent = path.substringBeforeLast('/', "")
        if (parent.isNotEmpty()) {
            NSFileManager.defaultManager.createDirectoryAtPath(
                parent, withIntermediateDirectories = true, attributes = null, error = null,
            )
        }
    }
}

actual suspend fun createTempFilePath(prefix: String): String = withIO {
    NSTemporaryDirectory() + prefix + "_" + TimeHelper.nowMillis()
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun importAppFile(tempFilePath: String, contentType: String, deleteSrc: Boolean): String? = withIO {
    val dFile = AppFileStore.importFile(tempFilePath, contentType, deleteSrc)
    dFile.realPath.substringAfterLast('/')
}

actual suspend fun streamContentUri(uri: String, sink: StreamSink): String? = null

actual suspend fun convert3gpToMp4(uri: String): ByteArray? = null

actual suspend fun getPackageIconBytes(packageName: String): ByteArray? = null

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun decodeImageFileToPng(path: String): ByteArray? = withIO {
    try {
        val mgr = NSFileManager.defaultManager
        if (!mgr.fileExistsAtPath(path)) return@withIO null
        val data = mgr.contentsAtPath(path) ?: return@withIO null
        val bytes = data.toByteArray()
        if (bytes.size < 12) return@withIO null
        if (!isHeifHeader(bytes)) return@withIO null
        val image = UIImage(data = data) ?: return@withIO null
        // Cap the decoded edge like Android so huge HEIF photos don't blow up the
        // render buffer; 4096 keeps 12 MP photos pixel-exact.
        val iw = image.size.useContents { width }
        val ih = image.size.useContents { height }
        if (iw <= 0.0 || ih <= 0.0) return@withIO null
        val cap = DecodePolicy.MAX_FULL_VIEW_EDGE.toDouble()
        val scale = minOf(cap / iw, cap / ih, 1.0)
        val png = if (scale >= 1.0) {
            UIImagePNGRepresentation(image)
        } else {
            val outW = iw * scale
            val outH = ih * scale
            UIGraphicsBeginImageContextWithOptions(CGSizeMake(outW, outH), false, 0.0)
            try {
                image.drawInRect(CGRectMake(0.0, 0.0, outW, outH))
                UIGraphicsGetImageFromCurrentImageContext()?.let { UIImagePNGRepresentation(it) }
            } finally {
                UIGraphicsEndImageContext()
            }
        }
        png?.toByteArray()
    } catch (e: Exception) {
        LogCat.e("decodeImageFileToPng: ${e.message}")
        null
    }
}

actual fun isAnimatedImageOrSvg(path: String, fileName: String): Boolean {
    val fp = fopen(path, "rb") ?: return false
    try {
        val header = ByteArray(256)
        val read = header.usePinned { pinned ->
            fread(pinned.addressOf(0), 1UL, header.size.toULong(), fp)
        }.toInt()
        if (read <= 0) return false
        return isAnimatedImageOrSvgHeader(fileName, header, fileSize(path))
    } finally {
        fclose(fp)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun getThumbnailBytes(
    path: String,
    width: Int,
    height: Int,
    centerCrop: Boolean,
    mediaId: String,
    fileName: String,
): ByteArray? = withIO {
    try {
        val mgr = NSFileManager.defaultManager
        if (!mgr.fileExistsAtPath(path)) return@withIO null
        val data = mgr.contentsAtPath(path) ?: return@withIO null
        val image = UIImage(data = data) ?: return@withIO null
        val iw = image.size.useContents { width }
        val ih = image.size.useContents { height }
        if (iw <= 0.0 || ih <= 0.0) return@withIO null

        val scaleW = width.toDouble() / iw
        val scaleH = height.toDouble() / ih
        val scale = if (centerCrop) maxOf(scaleW, scaleH) else minOf(scaleW, scaleH)
        val scaledW = iw * scale
        val scaledH = ih * scale

        val targetW = if (centerCrop) width.toDouble() else scaledW
        val targetH = if (centerCrop) height.toDouble() else scaledH
        val offsetX = if (centerCrop) (width - scaledW) / 2.0 else 0.0
        val offsetY = if (centerCrop) (height - scaledH) / 2.0 else 0.0

        UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetW, targetH), false, 0.0)
        try {
            image.drawInRect(CGRectMake(offsetX, offsetY, scaledW, scaledH))
            val result = UIGraphicsGetImageFromCurrentImageContext() ?: return@withIO null
            val png = UIImagePNGRepresentation(result) ?: return@withIO null
            png.toByteArray()
        } finally {
            UIGraphicsEndImageContext()
        }
    } catch (e: Exception) {
        LogCat.e("getThumbnailBytes: ${e.message}")
        null
    }
}

actual suspend fun streamZipToSink(items: List<ZipStreamEntry>, sink: StreamSink): Boolean = false

actual suspend fun streamZipFolderToSink(folderPath: String, sink: StreamSink): Boolean = false

actual suspend fun streamZipInternalDirToSink(zipVirtualPath: String, sink: StreamSink): Boolean = false

actual suspend fun fetchUrlToStream(url: String, sink: StreamSink): Pair<Int, String?> = withIO {
    val client = createDownloadClient()
    try {
        val response = client.get(url)
        val status = response.status.value
        val contentType = response.headers["Content-Type"]
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = channel.readAvailable(buffer)
            if (read <= 0) break
            sink.write(buffer, 0, read)
        }
        status to contentType
    } catch (e: Exception) {
        LogCat.e("fetchUrlToStream: ${e.message}")
        0 to null
    } finally {
        client.close()
    }
}

actual fun isContentUri(path: String): Boolean = false

actual suspend fun searchZipItems(type: String, query: String, tempId: String): List<ZipStreamEntry> = emptyList()

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun readTextFile(path: String): String = withIO {
    try {
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) {
            return@withIO ""
        }
        val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return@withIO ""
        NSString.create(data, NSUTF8StringEncoding)?.toString() ?: ""
    } catch (e: Exception) {
        ""
    }
}

actual suspend fun writeBytesToUri(uriStr: String, bytes: ByteArray): Boolean = withIO {
    writeBytesToPath(uriStr, bytes)
}

actual fun getFileNameFromUri(uriStr: String): String? {
    if (uriStr.isEmpty()) return null
    return uriStr.getFilenameFromPath().ifEmpty { null }
}

@OptIn(ExperimentalForeignApi::class)
actual fun queryPickedFileInfo(uriStr: String): PickedFileInfo? = try {
    val url = NSURL.URLWithString(uriStr) ?: return null
    val path = url.path ?: return null
    val mgr = NSFileManager.defaultManager
    if (!mgr.fileExistsAtPath(path)) return null
    val attrs = mgr.attributesOfItemAtPath(path, null) ?: return null
    val size = (attrs["NSFileSize"] as? NSNumber)?.longValue() ?: 0L
    val displayName = path.substringAfterLast('/').ifEmpty { "file" }
    val mime = getContentTypeForPath(path) ?: ""
    PickedFileInfo(displayName, size, mime)
} catch (_: Exception) {
    null
}

actual suspend fun importChatFile(uriStr: String, mimeType: String): String? = withIO {
    try {
        val url = NSURL.URLWithString(uriStr) ?: return@withIO null
        val path = url.path ?: return@withIO null
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return@withIO null
        importAppFile(path, mimeType, deleteSrc = false)
    } catch (_: Exception) {
        null
    }
}

actual suspend fun getFileByMediaId(mediaId: String): DFile? = null

internal actual fun ensureDir(path: String) {
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(path)) {
        fm.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
    }
}

internal actual fun appendLine(path: String, line: String): Long {
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(path)) {
        fm.createFileAtPath(path, null, null)
    }
    val fp = fopen(path, "a")
    if (fp == null) {
        NSLog("DiskLog: fopen failed for %@", path)
    } else {
        try {
            val data = line.encodeToByteArray()
            data.usePinned { pinned ->
                val written = fwrite(pinned.addressOf(0), 1.toULong(), data.size.toULong(), fp)
                if (written < data.size.toULong()) {
                    NSLog("DiskLog: short write for %@", path)
                }
            }
            fflush(fp)
        } finally {
            fclose(fp)
        }
    }
    return (fm.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? Long) ?: 0L
}

internal actual fun deleteFileIfExists(path: String) {
    val fm = NSFileManager.defaultManager
    if (fm.fileExistsAtPath(path)) {
        fm.removeItemAtPath(path, null)
    }
}

internal actual fun renameFile(from: String, to: String) {
    NSFileManager.defaultManager.moveItemAtPath(from, toPath = to, error = null)
}
