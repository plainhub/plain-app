package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.extensions.queryOpenableFile
import com.ismartcoding.plain.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.web.http.StreamSink
import android.net.Uri
import androidx.core.net.toUri
import com.ismartcoding.plain.data.DownloadFileItem
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.ImageType
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.helpers.Mp4Helper
import com.ismartcoding.plain.helpers.ZipHelper
import com.ismartcoding.plain.helpers.TempHelper
import com.ismartcoding.plain.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.media.FileMediaStoreHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.lib.extensions.compress
import com.ismartcoding.plain.lib.extensions.getContentType
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.thumbnail.ThumbnailProvider
import com.ismartcoding.plain.ui.page.appfiles.AppFileDisplayNameHelper
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual fun getFileId(path: String): String = FileHelper.getFileId(path)

actual fun deleteFileAt(path: String) {
    File(path).delete()
}

actual fun createLongTextFile(text: String): DMessageContent {
    val timestamp = TimeHelper.now().toEpochMilliseconds()
    val fileName = "message-$timestamp.txt"
    val dir = appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
    if (!dir!!.exists()) dir.mkdirs()
    val file = java.io.File(dir, fileName)
    file.writeText(text)
    val summary = text.substring(0, minOf(text.length, Constants.TEXT_FILE_SUMMARY_LENGTH))
    val messageFile = DMessageFile(uri = file.absolutePath, size = file.length(), summary = summary, fileName = fileName)
    return DMessageContent(DMessageType.FILES.value, DMessageFiles(listOf(messageFile)))
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
    return DFile(
        name = file.name,
        path = file.absolutePath,
        permission = "rw",
        createdAt = null,
        updatedAt = kotlin.time.Instant.fromEpochMilliseconds(file.lastModified()),
        size = file.length(),
        isDir = false,
        children = 0,
        mediaId = "",
    )
}

actual fun getUploadTmpDirPath(): String =
    File(appContext.filesDir, "upload_tmp").absolutePath

actual fun getUploadCacheMergeDirPath(): String =
    File(appContext.cacheDir, "upload_merge").apply { mkdirs() }.absolutePath

actual fun listUploadedChunks(fileId: String): List<String> {
    val chunkDir = File(getUploadTmpDirPath(), fileId)
    if (!chunkDir.exists()) return emptyList()
    return chunkDir.listFiles()
        ?.filter { it.name.startsWith("chunk_") }
        ?.mapNotNull { file ->
            val index = file.name.removePrefix("chunk_").toIntOrNull()
            if (index != null) "${index}:${file.length()}" else null
        }
        ?.sortedBy { it.substringBefore(':').toInt() }
        ?: emptyList()
}

actual fun deleteUploadedChunks(fileId: String): Boolean {
    val chunkDir = File(getUploadTmpDirPath(), fileId)
    if (chunkDir.exists()) {
        chunkDir.deleteRecursively()
    }
    return true
}

actual suspend fun mergeUploadedChunks(
    fileId: String,
    totalChunks: Int,
    path: String,
    replace: Boolean,
    isAppFile: Boolean,
): String {
    val chunkDir = File(getUploadTmpDirPath(), fileId)
    if (!chunkDir.exists()) {
        throw com.ismartcoding.plain.lib.kgraphql.GraphQLError("No chunks found for $fileId")
    }

    var expectedMergedSize = 0L
    for (i in 0 until totalChunks) {
        val chunkFile = File(chunkDir, "chunk_$i")
        if (!chunkFile.exists()) {
            throw com.ismartcoding.plain.lib.kgraphql.GraphQLError("Missing chunk $i")
        }
        expectedMergedSize += chunkFile.length()
    }

    val outputFile = if (replace) {
        File(path)
    } else {
        val originalFile = File(path)
        if (originalFile.exists()) {
            File(originalFile.newPath())
        } else {
            originalFile
        }
    }
    outputFile.parentFile?.mkdirs()

    val tempMergeFile = File(getUploadCacheMergeDirPath(), ".merge_tmp_${fileId}_${System.currentTimeMillis()}")
    try {
        java.io.FileOutputStream(tempMergeFile).use { fos ->
            for (i in 0 until totalChunks) {
                val chunkFile = File(chunkDir, "chunk_$i")
                chunkFile.inputStream().use { input ->
                    input.copyTo(fos)
                }
            }
        }

        val mergedSize = tempMergeFile.length()

        if (mergedSize != expectedMergedSize) {
            tempMergeFile.delete()
            throw com.ismartcoding.plain.lib.kgraphql.GraphQLError("Merge integrity failed: expected $expectedMergedSize, got $mergedSize")
        }

        if (outputFile.exists() && replace) {
            outputFile.delete()
        }
        tempMergeFile.copyTo(outputFile, overwrite = true)
        java.io.FileOutputStream(outputFile, true).use { it.fd.sync() }
        tempMergeFile.delete()
    } catch (e: Exception) {
        tempMergeFile.delete()
        throw e
    }

    val mergedSize = outputFile.length()

    chunkDir.deleteRecursively()
    if (isAppFile) {
        val dFile = com.ismartcoding.plain.helpers.AppFileStore.importFile(outputFile, "", deleteSrc = true)
        val fidSuffix = java.io.File(dFile.realPath).name
        return "${fidSuffix}:$mergedSize"
    } else {
        appContext.scanFileByConnection(outputFile, null)
        return "${outputFile.name}:$mergedSize"
    }
}

actual fun saveUploadChunk(fileId: String, chunkIndex: Int, data: ByteArray): String {
    val chunkDir = File(getUploadTmpDirPath(), fileId).apply { mkdirs() }
    val chunkFile = File(chunkDir, "chunk_$chunkIndex")
    chunkFile.writeBytes(data)
    return chunkFile.absolutePath
}

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
    val dFile = AppFileStore.importFile(File(tempFilePath), contentType, deleteSrc)
    File(dFile.realPath).name
}

actual fun getContentTypeForPath(path: String): String? {
    val file = File(path)
    if (!file.exists()) return null
    return file.name.getContentType().toString()
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
    val isHeif = headerSize >= 12 &&
            header[4] == 0x66.toByte() && // 'f'
            header[5] == 0x74.toByte() && // 't'
            header[6] == 0x79.toByte() && // 'y'
            header[7] == 0x70.toByte() && // 'p'
            String(header.copyOfRange(8, 12)) in listOf("heic", "heix", "hevc", "hevx", "avif")
    if (!isHeif) return@withIO null
    val bitmap = android.graphics.BitmapFactory.decodeFile(path) ?: return@withIO null
    try {
        ByteArrayOutputStream().use { baos ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        }
    } finally {
        bitmap.recycle()
    }
}

actual fun isAnimatedImageOrSvg(path: String, fileName: String): Boolean {
    val imageType = ImageHelper.getImageType(path, fileName)
    return imageType.isApplicableAnimated() || imageType == ImageType.SVG
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

actual suspend fun fetchUrlToStream(url: String, sink: StreamSink): Pair<Int, String?> = withIO {
    try {
        val client = com.ismartcoding.plain.api.OkHttpClientFactory.createUnsafeOkHttpClient()
        val request = okhttp3.Request.Builder().url(url).build()
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
                ZipStreamEntry(file.realPath, displayName)
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

actual suspend fun getFileByMediaId(mediaId: String): DFile? = withIO {
    if (mediaId.isEmpty()) null
    else FileMediaStoreHelper.getByIdAsync(appContext, mediaId)
}
