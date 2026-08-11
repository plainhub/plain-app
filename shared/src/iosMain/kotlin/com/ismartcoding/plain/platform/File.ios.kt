@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DAppFile
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.FileHashHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.toNSData
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.httpserver.http.StreamSink
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
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
import platform.UniformTypeIdentifiers.UTType
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
actual fun deleteFileAt(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}

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
    val summary = text.substring(0, minOf(text.length, Constants.TEXT_FILE_SUMMARY_LENGTH))
    val size = NSFileManager.defaultManager.contentsAtPath(file)?.length?.toInt()?.toLong() ?: 0L
    val messageFile = DMessageFile(uri = file, size = size, summary = summary, fileName = fileName)
    return DMessageContent(DMessageType.FILES.value, DMessageFiles(listOf(messageFile)))
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
    return DFile(
        name = path.substringAfterLast('/'),
        path = path,
        permission = "rw",
        createdAt = null,
        updatedAt = TimeHelper.now(),
        size = size,
        isDir = false,
        children = 0,
        mediaId = "",
    )
}

actual fun getUploadTmpDirPath(): String =
    appDir() + "/upload_tmp"

actual fun getUploadCacheMergeDirPath(): String =
    appDir() + "/upload_merge"

@OptIn(ExperimentalForeignApi::class)
actual fun listUploadedChunks(fileId: String): List<String> {
    val dir = getUploadTmpDirPath()
    if (!NSFileManager.defaultManager.fileExistsAtPath(dir)) return emptyList()
    val names = NSFileManager.defaultManager.contentsOfDirectoryAtPath(dir, null)
        ?.filterIsInstance<String>() ?: return emptyList()
    val prefix = "${fileId}_"
    return names
        .filter { it.startsWith(prefix) }
        .mapNotNull { name ->
            val index = name.removePrefix(prefix).toIntOrNull() ?: return@mapNotNull null
            val chunkPath = "$dir/$name"
            val size = NSFileManager.defaultManager.attributesOfItemAtPath(chunkPath, null)
                ?.let { attrs -> attrs[platform.Foundation.NSFileSize] as? Long } ?: 0L
            "$index:$size"
        }
        .sortedBy { it.substringBefore(':').toInt() }
}

@OptIn(ExperimentalForeignApi::class)
actual fun deleteUploadedChunks(fileId: String): Boolean {
    val dir = getUploadTmpDirPath()
    if (!NSFileManager.defaultManager.fileExistsAtPath(dir)) return true
    val prefix = "${fileId}_"
    val names = NSFileManager.defaultManager.contentsOfDirectoryAtPath(dir, null)
        ?.filterIsInstance<String>() ?: return true
    names.filter { it.startsWith(prefix) }.forEach { name ->
        NSFileManager.defaultManager.removeItemAtPath("$dir/$name", null)
    }
    return true
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun mergeUploadedChunks(
    fileId: String,
    totalChunks: Int,
    path: String,
    replace: Boolean,
    isAppFile: Boolean,
): String = withIO {
    val chunkDir = getUploadTmpDirPath()
    val chunkPrefix = "${fileId}_"

    var expectedSize = 0L
    for (i in 0 until totalChunks) {
        val chunkPath = "$chunkDir/${chunkPrefix}$i"
        if (!NSFileManager.defaultManager.fileExistsAtPath(chunkPath)) {
            throw com.ismartcoding.plain.lib.kgraphql.GraphQLError("Missing chunk $i")
        }
        val size = NSFileManager.defaultManager.attributesOfItemAtPath(chunkPath, null)
            ?.let { attrs -> attrs[platform.Foundation.NSFileSize] as? Long } ?: 0L
        expectedSize += size
    }

    val mergeDir = getUploadCacheMergeDirPath()
    NSFileManager.defaultManager.createDirectoryAtPath(
        mergeDir, withIntermediateDirectories = true, attributes = null, error = null,
    )
    val tempMergePath = "$mergeDir/.merge_tmp_${fileId}_${TimeHelper.nowMillis()}"

    try {
        val chunks = mutableListOf<ByteArray>()
        for (i in 0 until totalChunks) {
            val chunkPath = "$chunkDir/${chunkPrefix}$i"
            val data = NSFileManager.defaultManager.contentsAtPath(chunkPath)
            if (data != null && data.length > 0uL) {
                chunks.add(data.toByteArray())
            }
        }
        val mergedSize2 = chunks.sumOf { it.size }
        val mergedBytes = ByteArray(mergedSize2)
        var pos = 0
        for (chunk in chunks) {
            chunk.copyInto(mergedBytes, pos)
            pos += chunk.size
        }
        if (!mergedBytes.toNSData().writeToFile(tempMergePath, atomically = true)) {
            throw com.ismartcoding.plain.lib.kgraphql.GraphQLError("Cannot open merge temp file")
        }

        val mergedSize = NSFileManager.defaultManager.attributesOfItemAtPath(tempMergePath, null)
            ?.let { attrs -> attrs[platform.Foundation.NSFileSize] as? Long } ?: 0L

        if (mergedSize != expectedSize) {
            NSFileManager.defaultManager.removeItemAtPath(tempMergePath, null)
            throw com.ismartcoding.plain.lib.kgraphql.GraphQLError(
                "Merge integrity failed: expected $expectedSize, got $mergedSize",
            )
        }

        if (isAppFile) {
            val fidSuffix = importAppFileInternal(tempMergePath, "", deleteSrc = true)
            if (fidSuffix.isEmpty()) {
                throw com.ismartcoding.plain.lib.kgraphql.GraphQLError("Failed to import merged app file")
            }
            deleteUploadedChunks(fileId)
            "$fidSuffix:$mergedSize"
        } else {
            var destPath = path
            if (!replace && NSFileManager.defaultManager.fileExistsAtPath(destPath)) {
                destPath = getNewPath(destPath)
            }
            val parent = destPath.substringBeforeLast('/', "")
            if (parent.isNotEmpty()) {
                NSFileManager.defaultManager.createDirectoryAtPath(
                    parent, withIntermediateDirectories = true, attributes = null, error = null,
                )
            }
            if (NSFileManager.defaultManager.fileExistsAtPath(destPath)) {
                NSFileManager.defaultManager.removeItemAtPath(destPath, null)
            }
            if (!NSFileManager.defaultManager.moveItemAtPath(tempMergePath, destPath, error = null)) {
                throw com.ismartcoding.plain.lib.kgraphql.GraphQLError("Failed to move merged file to destination")
            }
            deleteUploadedChunks(fileId)
            "${destPath.substringAfterLast('/')}:$mergedSize"
        }
    } catch (e: com.ismartcoding.plain.lib.kgraphql.GraphQLError) {
        NSFileManager.defaultManager.removeItemAtPath(tempMergePath, null)
        throw e
    } catch (e: Exception) {
        NSFileManager.defaultManager.removeItemAtPath(tempMergePath, null)
        LogCat.e("mergeUploadedChunks: ${e.message}")
        throw com.ismartcoding.plain.lib.kgraphql.GraphQLError("Merge failed: ${e.message}")
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun saveUploadChunk(fileId: String, chunkIndex: Int, data: ByteArray): String {
    val dir = getUploadTmpDirPath()
    NSFileManager.defaultManager.createDirectoryAtPath(
        dir, withIntermediateDirectories = true, attributes = null, error = null,
    )
    val chunkPath = "$dir/${fileId}_$chunkIndex"
    data.toNSData().writeToFile(chunkPath, atomically = true)
    return chunkPath
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
        val fileLen = (attrs[platform.Foundation.NSFileSize] as? Long) ?: return@withIO null
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
private suspend fun importAppFileInternal(
    tempFilePath: String,
    contentType: String,
    deleteSrc: Boolean,
): String = withIO {
    val mgr = NSFileManager.defaultManager
    val data = mgr.contentsAtPath(tempFilePath) ?: return@withIO ""
    val bytes = data.toByteArray()
    if (bytes.isEmpty()) return@withIO ""

    val hash = FileHashHelper.strongHash(bytes)
    val ext = if (contentType.isNotEmpty()) {
        getExtensionFromMimeType(contentType)
    } else {
        tempFilePath.substringAfterLast('.', "").lowercase()
    }
    val fidSuffix = if (ext.isNotEmpty()) "$hash.$ext" else hash
    val destPath = "${appDir()}/${hash.substring(0, 2)}/${hash.substring(2, 4)}/$fidSuffix"

    val parent = destPath.substringBeforeLast('/', "")
    if (parent.isNotEmpty()) {
        mgr.createDirectoryAtPath(
            parent, withIntermediateDirectories = true, attributes = null, error = null,
        )
    }

    val dao = AppDatabase.instance.appFileDao()
    val existing = dao.getById(hash)
    if (existing != null) {
        if (!mgr.fileExistsAtPath(destPath)) {
            if (deleteSrc) {
                mgr.moveItemAtPath(tempFilePath, destPath, error = null)
            } else {
                mgr.copyItemAtPath(tempFilePath, destPath, error = null)
            }
        } else if (deleteSrc) {
            mgr.removeItemAtPath(tempFilePath, null)
        }
        dao.incrementRefCount(hash)
    } else {
        if (mgr.fileExistsAtPath(destPath)) {
            mgr.removeItemAtPath(destPath, null)
        }
        if (deleteSrc) {
            mgr.moveItemAtPath(tempFilePath, destPath, error = null)
        } else {
            mgr.copyItemAtPath(tempFilePath, destPath, error = null)
        }
        val record = DAppFile(hash).apply {
            size = bytes.size.toLong()
            mimeType = contentType.ifEmpty { "application/octet-stream" }
            realPath = "${hash.substring(0, 2)}/${hash.substring(2, 4)}/$fidSuffix"
            refCount = 1
            weakHash = FileHashHelper.weakHash(bytes)
        }
        dao.insert(record)
    }
    fidSuffix
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun importAppFile(tempFilePath: String, contentType: String, deleteSrc: Boolean): String? =
    importAppFileInternal(tempFilePath, contentType, deleteSrc).takeIf { it.isNotEmpty() }

actual fun getContentTypeForPath(path: String): String? {
    if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null
    val ext = path.substringAfterLast('.', "").lowercase()
    if (ext.isEmpty()) return null
    val type = UTType.typeWithFilenameExtension(ext) ?: return null
    return type.preferredMIMEType
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
        val isHeif = bytes[4] == 0x66.toByte() && // 'f'
            bytes[5] == 0x74.toByte() && // 't'
            bytes[6] == 0x79.toByte() && // 'y'
            bytes[7] == 0x70.toByte() && // 'p'
            bytes.copyOfRange(8, 12).decodeToString() in listOf("heic", "heix", "hevc", "hevx", "avif")
        if (!isHeif) return@withIO null
        val image = UIImage(data = data) ?: return@withIO null
        val png = UIImagePNGRepresentation(image) ?: return@withIO null
        png.toByteArray()
    } catch (e: Exception) {
        LogCat.e("decodeImageFileToPng: ${e.message}")
        null
    }
}

actual fun isAnimatedImageOrSvg(path: String, fileName: String): Boolean {
    val name = if (fileName.isNotEmpty()) fileName else path
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext == "gif" || ext == "webp" || ext == "svg"
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
    return uriStr.substringAfterLast('/').ifEmpty { null }
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
    if (fp != null) {
        try {
            val data = line.encodeToByteArray()
            data.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1.toULong(), data.size.toULong(), fp)
            }
        } finally {
            fclose(fp)
        }
    }
    val attrs = fm.attributesOfItemAtPath(path, error = null)
    return (attrs?.get(NSFileSize) as? Long) ?: 0L
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
