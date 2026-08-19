package com.ismartcoding.plain.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.extensions.resolveAppFileRealPath
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.plain.helpers.ChatFileSaveHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.helpers.FileHashEdgeBytes
import com.ismartcoding.plain.lib.extensions.isAnimatedImageOrSvgHeader
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.isHeifHeader
import com.ismartcoding.plain.lib.extensions.toHexString
import com.ismartcoding.plain.lib.extensions.queryOpenableFile
import com.ismartcoding.plain.lib.extensions.queryOpenableFileName
import com.ismartcoding.plain.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.httpserver.http.StreamSink
import android.net.Uri
import androidx.core.net.toUri
import com.ismartcoding.plain.api.OkHttpClientFactory
import com.ismartcoding.plain.data.DownloadFileItem
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.lib.JsonHelper.jsonDecode
import com.ismartcoding.plain.helpers.Mp4Helper
import com.ismartcoding.plain.helpers.ZipHelper
import com.ismartcoding.plain.helpers.TempHelper
import com.ismartcoding.plain.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.media.FileMediaStoreHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.lib.extensions.compress
import com.ismartcoding.plain.thumbnail.DecodeLimiter
import com.ismartcoding.plain.thumbnail.DecodePolicy
import com.ismartcoding.plain.thumbnail.ThumbnailProvider
import com.ismartcoding.plain.ui.page.appfiles.AppFileDisplayNameHelper
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import okhttp3.Request
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

actual fun deleteFileAt(path: String) {
    File(path).delete()
}

actual fun fileSize(path: String): Long = File(path).takeIf { it.isFile }?.length() ?: 0L

actual fun copyFile(srcPath: String, destPath: String): Boolean {
    return try {
        File(srcPath).copyTo(File(destPath), overwrite = true)
        true
    } catch (_: Exception) {
        false
    }
}

actual fun moveFile(fromPath: String, toPath: String): Boolean {
    return try {
        File(fromPath).renameTo(File(toPath))
    } catch (_: Exception) {
        false
    }
}

actual fun listFilesInDir(path: String): List<String> =
    File(path).listFiles()?.mapNotNull { it.name } ?: emptyList()

actual fun deleteDirRecursively(path: String) {
    File(path).deleteRecursively()
}

actual suspend fun sha256File(path: String): String = withIO {
    try {
        java.security.MessageDigest.getInstance("SHA-256").let { digest ->
            File(path).inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().toHexString()
        }
    } catch (_: Exception) {
        ""
    }
}

actual suspend fun sha256FileEdges(path: String, size: Long): String = withIO {
    try {
        val sha256Hex = java.security.MessageDigest.getInstance("SHA-256").digest(
            if (size <= FileHashEdgeBytes * 2) {
                File(path).readBytes()
            } else {
                val first = ByteArray(FileHashEdgeBytes)
                val last = ByteArray(FileHashEdgeBytes)
                File(path).inputStream().use { it.read(first) }
                File(path).inputStream().use { inp ->
                    inp.skip(size - FileHashEdgeBytes)
                    inp.read(last)
                }
                first + last
            },
        ).toHexString()
        sha256Hex
    } catch (_: Exception) {
        ""
    }
}

actual fun writeBytesToPath(path: String, bytes: ByteArray): Boolean {
    return try {
        File(path).writeBytes(bytes)
        true
    } catch (_: Exception) {
        false
    }
}

actual fun createLongTextFile(text: String): DMessageContent {
    val timestamp = TimeHelper.now().toEpochMilliseconds()
    val fileName = "message-$timestamp.txt"
    val dir = appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
    if (!dir!!.exists()) dir.mkdirs()
    val file = java.io.File(dir, fileName)
    file.writeText(text)
    return buildLongTextMessage(file.absolutePath, fileName, text, file.length())
}

actual fun saveFileToDownloads(path: String, fileName: String): String {
    return FileHelper.copyFileToDownloads(path, fileName)
}

actual fun fileToUriString(path: String): String = File(path).toUri().toString()

actual fun getFileIconPath(extension: String): String =
    AppHelper.getFileIconPath(extension)

actual fun fileExists(path: String): Boolean = File(path).exists()

actual suspend fun copyPickedFileToAppStorage(uriStr: String, destRelativePath: String): String? = withIO {
    val context = appContext
    val uri = Uri.parse(uriStr)
    val file = context.contentResolver.queryOpenableFile(uri) ?: return@withIO null
    val destFile = File(appDir(), destRelativePath)
    destFile.parentFile?.mkdirs()
    FileHelper.copyFile(context, uri, destFile.absolutePath)
    file.displayName
}

actual fun writeFileText(path: String, content: String, overwrite: Boolean): DFile {
    val file = File(path)
    if (!overwrite && file.exists()) {
        throw com.ismartcoding.plain.lib.kgraphql.GraphQLError("File already exists")
    }
    file.writeText(content)
    appContext.scanFileByConnection(path)
    return buildTextFile(file.absolutePath, file.length(), file.lastModified())
}

actual fun getUploadTmpDirPath(): String =
    File(appContext.filesDir, "upload_tmp").absolutePath

actual fun getUploadCacheMergeDirPath(): String =
    File(appContext.cacheDir, "upload_merge").apply { mkdirs() }.absolutePath

// --- HTTP streaming & file sink abstractions ---

/** Adapter that writes to a Java [OutputStream] from a [StreamSink] interface. */
private class OutputStreamSink(private val os: OutputStream) : StreamSink {
    override suspend fun write(bytes: ByteArray) = withIO { os.write(bytes) }
    override suspend fun write(bytes: ByteArray, offset: Int, length: Int) = withIO { os.write(bytes, offset, length) }
    override suspend fun flush() = withIO { os.flush() }
    override suspend fun close() = withIO { os.close() }
}

actual suspend fun streamFileTo(path: String, sink: StreamSink): Boolean = withIO {
    try {
        File(path).inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                sink.write(buffer, 0, read)
            }
        }
        true
    } catch (e: Exception) {
        false
    }
}

actual suspend fun readFileRange(path: String, offset: Long, length: Int): ByteArray? = withIO {
    try {
        val file = File(path)
        if (!file.exists() || !file.isFile) return@withIO null
        val fileLen = file.length()
        if (offset >= fileLen) return@withIO ByteArray(0)
        val readLen = minOf(length.toLong(), fileLen - offset).toInt()
        if (readLen <= 0) return@withIO ByteArray(0)
        val bytes = ByteArray(readLen)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            raf.readFully(bytes)
        }
        bytes
    } catch (e: Exception) {
        null
    }
}

actual suspend fun createFileSink(path: String): StreamSink = withIO {
    val file = File(path)
    file.parentFile?.mkdirs()
    OutputStreamSink(FileOutputStream(file))
}

actual suspend fun renameFileAtomic(from: String, to: String): Boolean = withIO {
    val src = File(from)
    val dest = File(to)
    if (src.renameTo(dest)) return@withIO true
    src.copyTo(dest, overwrite = true)
    src.delete()
    true
}

actual suspend fun ensureParentDir(path: String) {
    withIO {
        File(path).parentFile?.mkdirs()
    }
}

actual suspend fun createTempFilePath(prefix: String): String = withIO {
    File(appContext.cacheDir, "${prefix}_${System.currentTimeMillis()}_${Thread.currentThread().id}").absolutePath
}

actual suspend fun importAppFile(tempFilePath: String, contentType: String, deleteSrc: Boolean): String? = withIO {
    val dFile = AppFileStore.importFile(tempFilePath, contentType, deleteSrc)
    dFile.realPath.substringAfterLast('/')
}

actual suspend fun streamContentUri(uri: String, sink: StreamSink): String? = withIO {
    val context = appContext
    val parsed = Uri.parse(uri)
    val mimeType = context.contentResolver.getType(parsed).orEmpty()
    try {
        context.contentResolver.openInputStream(parsed)?.buffered()?.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                sink.write(buffer, 0, read)
            }
        }
        mimeType.ifEmpty { null }
    } catch (e: Exception) {
        null
    }
}

actual suspend fun convert3gpToMp4(uri: String): ByteArray? = withIO {
    val context = appContext
    val parsed = uri.toUri()
    val mimeType = context.contentResolver.getType(parsed).orEmpty()
    if (mimeType.equals("video/3gpp", true) || mimeType.equals("video/3gp", true) || uri.endsWith(".3gp", true)) {
        Mp4Helper.convert3gpToMp4(context, parsed)
    } else {
        null
    }
}

actual suspend fun getPackageIconBytes(packageName: String): ByteArray? = withIO {
    val bitmap = PackageHelper.getIcon(packageName)
    ByteArrayOutputStream().use {
        bitmap.compress(80, it)
        it.toByteArray()
    }
}

actual suspend fun decodeImageFileToPng(path: String): ByteArray? = withIO {
    val header = ByteArray(12)
    val headerSize = File(path).inputStream().use { it.read(header) }
    val isHeif = headerSize >= 12 && isHeifHeader(header)
    // Cheap header sniff stays outside the permit so non-HEIF requests
    // (e.g. every <video> playback fetch) never queue behind decodes.
    if (!isHeif) return@withIO null
    DecodeLimiter.withPermit {
        // Cap the decoded edge: a 48 MP HEIC would otherwise allocate ~192 MB
        // per open view, which is fatal on small-heap compatibility containers.
        // 4096 px keeps 12 MP photos pixel-exact.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withPermit null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = DecodePolicy.capSampleSize(
                bounds.outWidth, bounds.outHeight, DecodePolicy.MAX_FULL_VIEW_EDGE
            )
        }
        val bitmap = BitmapFactory.decodeFile(path, opts) ?: return@withPermit null
        try {
            ByteArrayOutputStream().use { baos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                baos.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}

actual fun isAnimatedImageOrSvg(path: String, fileName: String): Boolean {
    val file = File(path)
    if (!file.exists()) return false
    return try {
        file.inputStream().use { input ->
            val header = ByteArray(256)
            input.read(header)
            isAnimatedImageOrSvgHeader(fileName, header, file.length())
        }
    } catch (_: Exception) {
        false
    }
}

actual suspend fun getThumbnailBytes(
    path: String,
    width: Int,
    height: Int,
    centerCrop: Boolean,
    mediaId: String,
    fileName: String,
): ByteArray? = withIO {
    val file = File(path)
    if (!file.exists()) return@withIO null
    ThumbnailProvider.instance?.toThumbBytesAsync(appContext, file, width, height, centerCrop, mediaId, fileName)
}

actual suspend fun streamZipToSink(items: List<ZipStreamEntry>, sink: StreamSink): Boolean = withIO {
    val os = object : OutputStream() {
        override fun write(b: Int) { runBlocking { sink.write(byteArrayOf(b.toByte())) } }
        override fun write(b: ByteArray, off: Int, len: Int) { runBlocking { sink.write(b, off, len) } }
    }
    try {
        ZipOutputStream(os).use { zip ->
            val dirs = items.filter { File(it.sourcePath).isDirectory }
            items.forEach { item ->
                val file = File(item.sourcePath)
                if (!file.exists()) return@forEach
                val skip = dirs.any {
                    file.absolutePath != it.sourcePath && file.absolutePath.startsWith(it.sourcePath)
                }
                if (skip) return@forEach
                val entryName = item.entryName.ifEmpty { file.name }
                if (file.isDirectory) {
                    zip.putNextEntry(ZipEntry("$entryName/"))
                    ZipHelper.zipFolderToStreamAsync(file, zip, entryName)
                } else {
                    zip.putNextEntry(ZipEntry(entryName))
                    file.inputStream().copyTo(zip)
                }
                zip.closeEntry()
            }
        }
        runBlocking { sink.flush() }
        true
    } catch (e: Exception) {
        false
    }
}

actual suspend fun streamZipFolderToSink(folderPath: String, sink: StreamSink): Boolean = withIO {
    val folder = File(folderPath)
    if (!folder.exists() || !folder.isDirectory) return@withIO false
    val os = object : OutputStream() {
        override fun write(b: Int) { runBlocking { sink.write(byteArrayOf(b.toByte())) } }
        override fun write(b: ByteArray, off: Int, len: Int) { runBlocking { sink.write(b, off, len) } }
    }
    try {
        ZipOutputStream(os).use { zip ->
            ZipHelper.zipFolderToStreamAsync(folder, zip)
        }
        runBlocking { sink.flush() }
        true
    } catch (e: Exception) {
        false
    }
}

actual suspend fun streamZipInternalDirToSink(zipVirtualPath: String, sink: StreamSink): Boolean = withIO {
    val zipFilePath = ZipBrowserHelper.getZipFilePath(zipVirtualPath)
    val prefix = ZipBrowserHelper.getInternalPath(zipVirtualPath).trimStart('/')
    val os = object : OutputStream() {
        override fun write(b: Int) { runBlocking { sink.write(byteArrayOf(b.toByte())) } }
        override fun write(b: ByteArray, off: Int, len: Int) { runBlocking { sink.write(b, off, len) } }
    }
    try {
        ZipFile(zipFilePath).use { zf ->
            ZipOutputStream(os).use { zip ->
                val it = zf.entries()
                while (it.hasMoreElements()) {
                    val entry = it.nextElement()
                    val entryName = entry.name.trimStart('/')
                    if (!entryName.startsWith(prefix)) continue
                    val newName = entryName.removePrefix(prefix).trimStart('/')
                    if (newName.isEmpty()) continue
                    zip.putNextEntry(ZipEntry(newName))
                    zf.getInputStream(entry).copyTo(zip)
                    zip.closeEntry()
                }
            }
        }
        runBlocking { sink.flush() }
        true
    } catch (e: Exception) {
        false
    }
}

actual suspend fun fetchUrlToStream(url: String, sink: StreamSink): Pair<Int, String?> = withIO {
    try {
        val client = OkHttpClientFactory.createUnsafeOkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val status = response.code
        val contentType = response.header("Content-Type")
        response.body?.byteStream()?.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                sink.write(buffer, 0, read)
            }
        }
        status to contentType
    } catch (e: Exception) {
        0 to null
    }
}

actual fun isContentUri(path: String): Boolean = path.startsWith("content://")

actual suspend fun searchZipItems(type: String, query: String, tempId: String): List<ZipStreamEntry> = withIO {
    val context = appContext
    when (type) {
        DataType.PACKAGE.name -> {
            PackageHelper.searchAsync(query, Int.MAX_VALUE, 0, FileSortBy.NAME_ASC).map {
                ZipStreamEntry(it.path, "${it.name.replace(" ", "")}-${it.id}.apk")
            }
        }
        DataType.VIDEO.name -> {
            VideoMediaStoreHelper.searchAsync(context, query, Int.MAX_VALUE, 0, FileSortBy.DATE_DESC).map {
                ZipStreamEntry(it.path, "")
            }
        }
        DataType.AUDIO.name -> {
            AudioMediaStoreHelper.searchAsync(context, query, Int.MAX_VALUE, 0, FileSortBy.DATE_DESC).map {
                ZipStreamEntry(it.path, "")
            }
        }
        DataType.IMAGE.name -> {
            ImageMediaStoreHelper.searchAsync(context, query, Int.MAX_VALUE, 0, FileSortBy.DATE_DESC).map {
                ZipStreamEntry(it.path, "")
            }
        }
        DataType.APP_FILE.name -> {
            val appFileDao = AppDatabase.instance.appFileDao()
            val chatDao = AppDatabase.instance.chatDao()
            val ids = query.removePrefix("ids:").split(",").filter { it.isNotEmpty() }
            val appFiles = if (ids.isNotEmpty()) appFileDao.getByIds(ids) else appFileDao.getAll()
            val nameMap = AppFileDisplayNameHelper.buildNameMap(chatDao.getAll())
            appFiles.map { file ->
                val displayName = AppFileDisplayNameHelper.resolveDisplayName(file, nameMap)
                ZipStreamEntry(file.realPath.resolveAppFileRealPath(), displayName)
            }
        }
        DataType.FILE.name -> {
            val value = TempHelper.getValue(tempId)
            TempHelper.clearValue(tempId)
            if (value.isEmpty()) emptyList()
            else jsonDecode<List<DownloadFileItem>>(value).map { ZipStreamEntry(it.path, it.name) }
        }
        else -> emptyList()
    }
}

actual suspend fun readTextFile(path: String): String = withIO {
    try {
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: ""
        } else {
            File(path).readText()
        }
    } catch (e: Exception) {
        ""
    }
}

actual suspend fun writeBytesToUri(uriStr: String, bytes: ByteArray): Boolean = withIO {
    try {
        if (uriStr.startsWith("content://")) {
            val uri = Uri.parse(uriStr)
            appContext.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
        } else {
            File(uriStr).writeBytes(bytes)
            true
        }
    } catch (_: Exception) {
        false
    }
}

actual fun getFileNameFromUri(uriStr: String): String? {
    if (!uriStr.startsWith("content://")) {
        return uriStr.getFilenameFromPath()
    }
    return try {
        appContext.contentResolver.queryOpenableFileName(Uri.parse(uriStr)).takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }
}

actual fun queryPickedFileInfo(uriStr: String): PickedFileInfo? {
    return try {
        val uri = Uri.parse(uriStr)
        val cr = appContext.contentResolver
        val file = cr.queryOpenableFile(uri) ?: return null
        val mime = cr.getType(uri) ?: ""
        PickedFileInfo(file.displayName, file.size, mime)
    } catch (_: Exception) {
        null
    }
}

actual suspend fun importChatFile(uriStr: String, mimeType: String): String? = withIO {
    try {
        ChatFileSaveHelper.importFromUri(appContext, Uri.parse(uriStr), mimeType)
    } catch (_: Exception) {
        null
    }
}

actual suspend fun getFileByMediaId(mediaId: String): DFile? = withIO {
    if (mediaId.isEmpty()) null
    else FileMediaStoreHelper.getByIdAsync(appContext, mediaId)
}

internal actual fun ensureDir(path: String) {
    val dir = File(path)
    if (!dir.exists()) dir.mkdirs()
}

internal actual fun appendLine(path: String, line: String): Long {
    val file = File(path)
    file.appendText(line)
    return file.length()
}

internal actual fun deleteFileIfExists(path: String) {
    val file = File(path)
    if (file.exists()) file.delete()
}

internal actual fun renameFile(from: String, to: String) {
    File(from).renameTo(File(to))
}
