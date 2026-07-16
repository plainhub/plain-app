package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.web.http.NullStreamSink
import com.ismartcoding.plain.web.http.StreamSink
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import kotlin.time.Instant

@OptIn(ExperimentalForeignApi::class)
actual fun deleteFileAt(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}

actual fun getFileId(path: String): String = ""

actual fun createLongTextFile(text: String): DMessageContent {
    return DMessageContent(
        DMessageType.FILES.value,
        DMessageFiles(emptyList())
    )
}

actual fun saveFileToDownloads(path: String, fileName: String): String = ""

actual fun fileToUriString(path: String): String = path

actual fun getFileIconPath(extension: String): String = ""

@OptIn(ExperimentalForeignApi::class)
actual fun fileExists(path: String): Boolean =
    NSFileManager.defaultManager.fileExistsAtPath(path)

actual suspend fun copyPickedFileToAppStorage(uriStr: String, destRelativePath: String): String? = null

actual fun writeFileText(path: String, content: String, overwrite: Boolean): DFile {
    // TODO: iOS implementation using NSFileManager/NSData
    return DFile(
        name = path.substringAfterLast('/'),
        path = path,
        permission = "rw",
        createdAt = null,
        updatedAt = Instant.fromEpochMilliseconds(0),
        size = content.encodeToByteArray().size.toLong(),
        isDir = false,
        children = 0,
        mediaId = "",
    )
}

actual fun getUploadTmpDirPath(): String =
    appDir() + "/upload_tmp"

actual fun getUploadCacheMergeDirPath(): String =
    appDir() + "/upload_merge"

actual fun listUploadedChunks(fileId: String): List<String> = emptyList()

actual fun deleteUploadedChunks(fileId: String): Boolean = true

actual suspend fun mergeUploadedChunks(
    fileId: String,
    totalChunks: Int,
    path: String,
    replace: Boolean,
    isAppFile: Boolean,
): String {
    // TODO: iOS implementation
    return ":0"
}

actual fun saveUploadChunk(fileId: String, chunkIndex: Int, data: ByteArray): String {
    // TODO: iOS implementation
    return ""
}

// --- HTTP streaming & file sink abstractions (iOS stubs) ---

actual suspend fun streamFileTo(path: String, sink: StreamSink): Boolean = false

actual suspend fun createFileSink(path: String): StreamSink = NullStreamSink

actual suspend fun renameFileAtomic(from: String, to: String): Boolean = false

actual suspend fun ensureParentDir(path: String) {}

actual suspend fun createTempFilePath(prefix: String): String = ""

actual suspend fun importAppFile(tempFilePath: String, contentType: String, deleteSrc: Boolean): String? = null

actual fun getContentTypeForPath(path: String): String? = null

actual suspend fun streamContentUri(uri: String, sink: StreamSink): String? = null

actual suspend fun convert3gpToMp4(uri: String): ByteArray? = null

actual suspend fun getPackageIconBytes(packageName: String): ByteArray? = null

actual suspend fun decodeImageFileToPng(path: String): ByteArray? = null

actual fun isAnimatedImageOrSvg(path: String, fileName: String): Boolean = false

actual suspend fun getThumbnailBytes(
    path: String,
    width: Int,
    height: Int,
    centerCrop: Boolean,
    mediaId: String,
    fileName: String,
): ByteArray? = null

actual suspend fun streamZipToSink(items: List<ZipStreamEntry>, sink: StreamSink): Boolean = false

actual suspend fun streamZipFolderToSink(folderPath: String, sink: StreamSink): Boolean = false

actual suspend fun fetchUrlToStream(url: String, sink: StreamSink): Pair<Int, String?> = 0 to null

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

actual suspend fun getFileByMediaId(mediaId: String): DFile? = null
