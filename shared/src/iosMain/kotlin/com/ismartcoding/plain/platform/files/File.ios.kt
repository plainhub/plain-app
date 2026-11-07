@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.features.files.FilesClient

import com.ismartcoding.plain.platform.getContentTypeForPath
import com.ismartcoding.plain.platform.media.getDownloadsDirPath

import com.ismartcoding.plain.models.File as FileEntry

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.isHeifHeader
import com.ismartcoding.plain.lib.toNSData
import com.ismartcoding.plain.lib.DecodePolicy
import com.ismartcoding.plain.lib.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.ensureActive
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import com.ismartcoding.plain.platform.media.IosPhotoResource
import com.ismartcoding.plain.rust.native.RustNativeBindings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import platform.posix.O_RDONLY
import platform.posix.SEEK_SET
import platform.posix.close
import platform.posix.fflush
import platform.posix.fileno
import platform.posix.fseek
import platform.posix.open
import platform.posix.read as posixRead
import platform.posix.tmpfile

@OptIn(ExperimentalForeignApi::class)
actual fun deleteFileAt(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun fileSize(path: String): Long =
    (NSFileManager.defaultManager.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? Long) ?: 0L

@OptIn(ExperimentalForeignApi::class)
actual fun writeBytesToPath(path: String, bytes: ByteArray): Boolean {
    return try {
        bytes.toNSData().writeToFile(path, atomically = true)
    } catch (_: Exception) {
        false
    }
}

actual suspend fun saveFileToDownloads(path: String, fileName: String): String =
    FilesClient.copyToDirectory(path, getDownloadsDirPath(), fileName)

actual suspend fun copyFileToUri(path: String, uriStr: String): Boolean = withIO {
    val destination = if (uriStr.startsWith("/")) NSURL.fileURLWithPath(uriStr)
        else NSURL.URLWithString(uriStr)?.takeIf { it.scheme == "file" } ?: return@withIO false
    val accessed = destination.startAccessingSecurityScopedResource()
    try {
        val destinationPath = destination.path ?: return@withIO false
        FilesClient.copyToPath(path, destinationPath, overwrite = true)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    } finally {
        if (accessed) destination.stopAccessingSecurityScopedResource()
    }
}

actual fun fileToUriString(path: String): String = path

internal actual fun fileUriPath(uri: String): String = requireNotNull(NSURLComponents.componentsWithString(uri)?.path) { "Invalid file URI" }


@OptIn(ExperimentalForeignApi::class)
actual fun fileExists(path: String): Boolean =
    NSFileManager.defaultManager.fileExistsAtPath(path)

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

@OptIn(ExperimentalForeignApi::class)
actual suspend fun streamContentUri(uri: String, sink: StreamSink): String? = withResourceFile(uri) { descriptor, _ ->
    val bytes = ByteArray(64 * 1024)
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = bytes.usePinned { posixRead(descriptor, it.addressOf(0), bytes.size.toULong()) }
        check(count >= 0) { "Unable to read file resource" }
        if (count == 0L) break
        sink.write(bytes, 0, count.toInt())
    }
    uri.substringAfterLast('/').ifEmpty { "resource" }
}

actual suspend fun getPackageIconBytes(packageName: String): ByteArray? = null

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun decodeImageFileToPng(path: String): ByteArray? = withIO {
    try {
        // Cheap header sniff stays outside the UIImage decode so non-HEIF
        // requests (e.g. every <video> playback fetch) never read the whole
        // file or queue behind decodes — mirrors the Android implementation.
        val fp = fopen(path, "rb") ?: return@withIO null
        val header = ByteArray(12)
        try {
            val read = header.usePinned { pinned ->
                fread(pinned.addressOf(0), 1UL, header.size.toULong(), fp)
            }.toInt()
            if (read < 12 || !isHeifHeader(header)) return@withIO null
        } finally {
            fclose(fp)
        }
        val mgr = NSFileManager.defaultManager
        val data = mgr.contentsAtPath(path) ?: return@withIO null
        val image = UIImage(data = data) ?: return@withIO null
        // Cap the decoded edge like Android so huge HEIF photos don't blow up the
        // render buffer; 4096 keeps 12 MP photos pixel-exact.
        val iw = image.size.useContents { width }
        val ih = image.size.useContents { height }
        if (iw <= 0.0 || ih <= 0.0) return@withIO null
        val cap = DecodePolicy.MAX_FULL_VIEW_EDGE.toDouble()
        val scale = minOf(cap / iw, cap / ih, 1.0)
        val outW = iw * scale
        val outH = ih * scale
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(outW, outH), false, 1.0)
        val png = try {
            image.drawInRect(CGRectMake(0.0, 0.0, outW, outH))
            UIGraphicsGetImageFromCurrentImageContext()?.let { UIImagePNGRepresentation(it) }
        } finally {
            UIGraphicsEndImageContext()
        }
        png?.toByteArray()
    } catch (e: Exception) {
        LogCat.e("decodeImageFileToPng: ${e.message}")
        null
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun decodeThumbnailBytes(
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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun readTextFile(path: String): String = withIO {
    val url = if (path.contains("://")) {
        NSURL.URLWithString(path)?.takeIf { it.scheme?.equals("file", ignoreCase = true) == true }
    } else {
        NSURL.fileURLWithPath(path)
    } ?: return@withIO ""
    val localPath = url.path ?: return@withIO ""
    val accessed = url.startAccessingSecurityScopedResource()
    try {
        if (!NSFileManager.defaultManager.fileExistsAtPath(localPath)) {
            return@withIO ""
        }
        val data = NSFileManager.defaultManager.contentsAtPath(localPath) ?: return@withIO ""
        NSString.create(data, NSUTF8StringEncoding)?.toString() ?: ""
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        ""
    } finally {
        if (accessed) url.stopAccessingSecurityScopedResource()
    }
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

actual suspend fun getFileByMediaId(mediaId: String): FileEntry? = null

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun provideResourceFile(id: String, uri: String): Unit = withResourceFile(uri) { descriptor, length ->
    val error = RustNativeBindings.resourceFile(id, descriptor, 0, length)
    check(error.isEmpty()) { error }
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun <T> withResourceFile(uri: String, block: suspend (Int, Long) -> T): T = withIO {
    if (uri.startsWith("ph://")) {
        val file = checkNotNull(tmpfile()) { "Unable to prepare photo resource" }
        try {
            val size = IosPhotoResource.write(uri, file)
            check(fflush(file) == 0) { "Unable to flush photo resource" }
            check(fseek(file, 0, SEEK_SET) == 0) { "Unable to seek photo resource" }
            block(fileno(file), size)
        } finally { fclose(file) }
    } else {
        val path = NSURL.URLWithString(uri)?.path ?: uri
        val descriptor = open(path, O_RDONLY)
        check(descriptor >= 0) { "Unable to open file resource" }
        try { block(descriptor, -1) } finally { close(descriptor) }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun readPickedFileBytes(uri: String): ByteArray = withIO {
    val path = NSURL.URLWithString(uri)?.path ?: error("Failed to read the selected file")
    NSFileManager.defaultManager.contentsAtPath(path)?.toByteArray()
        ?: error("Failed to read the selected file")
}
