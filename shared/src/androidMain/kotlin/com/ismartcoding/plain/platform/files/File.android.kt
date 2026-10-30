package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.features.files.FilesClient

import com.ismartcoding.plain.models.File as FileEntry

import android.graphics.Bitmap
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.isHeifHeader
import com.ismartcoding.plain.lib.extensions.queryOpenableFile
import com.ismartcoding.plain.lib.extensions.queryOpenableFileName
import android.net.Uri
import androidx.core.net.toUri
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.platform.media.provider.FileMediaStoreHelper
import com.ismartcoding.plain.lib.extensions.compress
import com.ismartcoding.plain.ui.media.thumbnail.DecodeLimiter
import com.ismartcoding.plain.lib.DecodePolicy
import java.io.ByteArrayOutputStream
import java.io.File
import android.graphics.ImageDecoder
import android.os.Environment
import com.ismartcoding.plain.helpers.PathHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.ui.media.thumbnail.ThumbnailGenerator
import kotlinx.coroutines.CancellationException

actual fun deleteFileAt(path: String) {
    File(path).delete()
}

actual fun fileSize(path: String): Long = File(path).takeIf { it.isFile }?.length() ?: 0L

actual fun writeBytesToPath(path: String, bytes: ByteArray): Boolean {
    return try {
        File(path).writeBytes(bytes)
        true
    } catch (_: Exception) {
        false
    }
}

actual suspend fun saveFileToDownloads(path: String, fileName: String): String = withIO {
    try {
        FilesClient.copyToDirectory(
            path,
            PathHelper.getPlainPublicDir(Environment.DIRECTORY_DOWNLOADS).absolutePath,
            fileName.ifEmpty { path.getFilenameFromPath() },
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        LogCat.e("Save to downloads failed", error)
        ""
    }
}

actual fun fileToUriString(path: String): String = File(path).toUri().toString()


actual fun fileExists(path: String): Boolean = File(path).exists()

actual suspend fun ensureParentDir(path: String) {
    withIO {
        File(path).parentFile?.mkdirs()
    }
}

actual suspend fun streamContentUri(uri: String, sink: StreamSink): String? = withIO {
    val resolver = appContext.contentResolver
    val parsed = Uri.parse(uri)
    val input = resolver.openInputStream(parsed) ?: return@withIO null
    input.use { it.copyChunksTo(sink) }
    resolver.getType(parsed).orEmpty()
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
        val bitmap = ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(File(path))
        ) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(DecodePolicy.capSampleSize(
                info.size.width, info.size.height, DecodePolicy.MAX_FULL_VIEW_EDGE
            ))
        }
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

actual suspend fun decodeThumbnailBytes(
    path: String,
    width: Int,
    height: Int,
    centerCrop: Boolean,
    mediaId: String,
    fileName: String,
): ByteArray? = withIO {
    val file = File(path)
    if (!file.exists()) return@withIO null
    ThumbnailGenerator.decodeBytesAsync(appContext, file, width, height, centerCrop, mediaId, fileName)
}

actual suspend fun readTextFile(path: String): String = withIO {
    try {
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: ""
        } else {
            val filePath = if (path.startsWith("file://")) Uri.parse(path).path else path
            File(filePath ?: path).readText()
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        ""
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

actual suspend fun getFileByMediaId(mediaId: String): FileEntry? = withIO {
    if (mediaId.isEmpty()) null
    else FileMediaStoreHelper.getByIdAsync(appContext, mediaId)
}

internal actual suspend fun provideResourceFile(id: String, uri: String): Unit = withIO {
    appContext.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.use { asset ->
        val error = RustNativeBindings.resourceFile(
            id, asset.parcelFileDescriptor.fd, asset.startOffset, asset.declaredLength,
        )
        check(error.isEmpty()) { error }
    } ?: error("Unable to open content URI")
}

actual suspend fun copyFileToUri(path: String, uriStr: String): Boolean = withIO {
    val uri = Uri.parse(uriStr)
    val source = File(path)
    if (uri.scheme == "content") {
        val output = appContext.contentResolver.openOutputStream(uri, "wt") ?: return@withIO false
        output.use { sink -> source.inputStream().use { input -> input.copyTo(sink) } }
    } else {
        val destination = File(if (uri.scheme == "file") uri.path ?: return@withIO false else uriStr)
        if (source.canonicalFile == destination.canonicalFile) return@withIO false
        source.copyTo(destination, overwrite = true)
    }
    true
}

internal actual suspend fun readPickedFileBytes(uri: String): ByteArray = withIO {
    appContext.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
        ?: error("Failed to read the selected file")
}
