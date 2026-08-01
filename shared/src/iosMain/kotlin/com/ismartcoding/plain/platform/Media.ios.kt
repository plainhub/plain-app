@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.data.DImageMeta
import com.ismartcoding.plain.data.DVideoMeta
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVURLAsset
import platform.CoreGraphics.CGSize
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSFileManager
import platform.Foundation.NSInvocation
import platform.Foundation.NSMethodSignature
import platform.Foundation.NSURL
import platform.UIKit.UIImage
import platform.darwin.NSObject
import platform.objc.sel_registerName
import platform.posix.memcpy
import kotlin.math.atan2

actual fun getImageRotation(path: String): Int = 0

actual fun getImageIntrinsicSize(path: String, rotation: Int): IntSize {
    val (w, h) = readImageDimensions(path) ?: return IntSize.Zero
    return if (rotation == 90 || rotation == 270) IntSize(h, w) else IntSize(w, h)
}

actual fun getVideoIntrinsicSize(path: String): IntSize {
    val (w, h, _) = readVideoTrackInfo(path) ?: return IntSize.Zero
    return IntSize(w, h)
}

actual fun getVideoMeta(path: String): DVideoMeta? {
    val (w, h, rotation) = readVideoTrackInfo(path) ?: return null
    val durationMs = readAssetDurationMs(path)
    val (displayW, displayH) = if (rotation == 90 || rotation == 270) h to w else w to h
    return DVideoMeta(
        width = displayW,
        height = displayH,
        rotation = rotation,
        duration = if (durationMs > 0) durationMs / 1000 else 0L,
        bitrate = 0L,
        frameRate = 0f,
        title = "",
        artist = "",
        album = "",
        genre = "",
        takenAt = null,
        writer = "",
        composer = "",
    )
}

actual fun getImageMeta(path: String): DImageMeta? {
    val (w, h) = readImageDimensions(path) ?: return null
    val rotation = getImageRotation(path)
    val (displayW, displayH) = if (rotation == 90 || rotation == 270) h to w else w to h
    return DImageMeta(
        make = "",
        model = "",
        width = displayW,
        height = displayH,
        rotation = rotation,
        colorSpace = "",
        apertureValue = 0.0,
        exposureTime = "",
        focalLength = "",
        isoSpeed = 0,
        takenAt = null,
        flash = 0,
        fNumber = 0.0,
        exposureProgram = 0,
        meteringMode = 0,
        whiteBalance = 0,
        creator = "",
        resolutionX = 0,
        resolutionY = 0,
        description = "",
    )
}

actual fun tryDecodeQrCode(path: String): String? = try {
    val image = UIImage.imageWithContentsOfFile(path) ?: return null
    val ciImage = image.CIImage() ?: return null
    val context = platform.CoreImage.CIContext.context()
    val detector = platform.CoreImage.CIDetector.detectorOfType(
        platform.CoreImage.CIDetectorTypeQRCode, context, null,
    ) ?: return null
    val features = detector.featuresInImage(ciImage) ?: return null
    for (raw in features) {
        val feature = raw as? NSObject ?: continue
        val msg = feature.performSelector(sel_registerName("messageString")) as? String
        if (!msg.isNullOrEmpty()) return msg
    }
    null
} catch (e: Exception) {
    LogCat.e("tryDecodeQrCode: ${e.message}")
    null
}

actual fun fileLength(path: String): Long = try {
    val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
    val size = attrs?.get("NSFileSize")
    (size as? Number)?.toLong() ?: 0L
} catch (e: Exception) {
    0L
}

actual suspend fun renameMediaFile(path: String, newName: String): String? = try {
    val parent = path.substringBeforeLast('/', "")
    if (parent.isEmpty()) return null
    val newPath = "$parent/$newName"
    NSFileManager.defaultManager.moveItemAtPath(path, newPath, null)
    newPath
} catch (e: Exception) {
    LogCat.e("renameMediaFile: ${e.message}")
    null
}

actual fun getMediaDurationMs(path: String): Long = readAssetDurationMs(path)

actual fun getAudioDurationMsFromPath(path: String): Long = readAssetDurationMs(path)

actual fun generateQrCode(text: String, width: Int, height: Int): ImageBitmap {
    return ImageBitmap(width, height, ImageBitmapConfig.Argb8888)
}

actual fun getSvgSize(path: String): IntSize = parseSvgSize(path) ?: IntSize(150, 150)

actual fun addMediaShortcut(path: String, label: String) {
    // iOS does not support dynamic home-screen shortcuts for arbitrary files
}

actual suspend fun processSingleDurationZero(
    mediaType: String,
    item: com.ismartcoding.plain.events.MediaDurationZeroItem,
) {
    // iOS MediaStore equivalent does not exist; duration is read on demand
}

// ---- helpers ----

private fun readImageDimensions(path: String): Pair<Int, Int>? {
    return try {
        val image = UIImage.imageWithContentsOfFile(path) ?: return null
        val size = image.size
        val w = size.useContents { this.width.toInt() }
        val h = size.useContents { this.height.toInt() }
        if (w <= 0 || h <= 0) null else w to h
    } catch (e: Exception) {
        LogCat.e("readImageDimensions: ${e.message}")
        null
    }
}

private data class VideoTrackInfo(val width: Int, val height: Int, val rotation: Int)

private fun readVideoTrackInfo(path: String): VideoTrackInfo? {
    return try {
        val asset = AVURLAsset(NSURL.fileURLWithPath(path), null)
        val tracksObj = asset.performSelector(
            sel_registerName("tracksWithMediaType:"),
            withObject = AVMediaTypeVideo,
        )
        val tracks = (tracksObj as? List<*>)?.filterIsInstance<AVAssetTrack>() ?: emptyList()
        val firstVideoTrack = tracks.firstOrNull() ?: return null
        val sizeBytes = callStructReturningSelectorRaw(firstVideoTrack, "naturalSize") ?: return null
        val (w, h) = memScoped {
            val cs = alloc<CGSize>()
            val copyLen = minOf(sizeBytes.size, 16).toULong()
            sizeBytes.usePinned { pinned ->
                memcpy(cs.ptr, pinned.addressOf(0), copyLen)
            }
            Pair(cs.width.toInt(), cs.height.toInt())
        }
        val rotation = readVideoRotation(firstVideoTrack)
        VideoTrackInfo(w, h, rotation)
    } catch (e: Exception) {
        LogCat.e("readVideoTrackInfo: ${e.message}")
        null
    }
}

private fun readVideoRotation(track: AVAssetTrack): Int {
    val transformBytes = callStructReturningSelectorRaw(track, "preferredTransform") ?: return 0
    if (transformBytes.size < 48) return 0
    val a = Double.fromBits(transformBytes.toLongAt(0))
    val b = Double.fromBits(transformBytes.toLongAt(8))
    val radians = atan2(b, a)
    val degrees = radians * 180.0 / kotlin.math.PI
    return ((degrees % 360.0 + 360.0) % 360.0).toInt()
}

private fun ByteArray.toLongAt(offset: Int): Long =
    ((this[offset].toLong() and 0xFF) shl 56) or
        ((this[offset + 1].toLong() and 0xFF) shl 48) or
        ((this[offset + 2].toLong() and 0xFF) shl 40) or
        ((this[offset + 3].toLong() and 0xFF) shl 32) or
        ((this[offset + 4].toLong() and 0xFF) shl 24) or
        ((this[offset + 5].toLong() and 0xFF) shl 16) or
        ((this[offset + 6].toLong() and 0xFF) shl 8) or
        (this[offset + 7].toLong() and 0xFF)

private fun callStructReturningSelectorRaw(target: NSObject, selName: String): ByteArray? {
    return try {
        val sel = sel_registerName(selName)
        val sigRaw = target.methodSignatureForSelector(sel) ?: return null
        val sig = sigRaw as Any as NSMethodSignature
        val inv = NSInvocation.invocationWithMethodSignature(sig)
        inv.setTarget(target)
        inv.setSelector(sel)
        inv.invoke()
        val size = sig.methodReturnLength.toInt().coerceAtLeast(16)
        val buf = ByteArray(size)
        buf.usePinned { pinned ->
            inv.getReturnValue(pinned.addressOf(0))
        }
        buf
    } catch (e: Exception) {
        LogCat.e("callStructReturningSelectorRaw($selName): ${e.message}")
        null
    }
}

private fun readAssetDurationMs(path: String): Long = try {
    val asset = AVURLAsset(NSURL.fileURLWithPath(path), null)
    val seconds = CMTimeGetSeconds(asset.duration)
    if (seconds.isNaN() || seconds.isInfinite() || seconds <= 0.0) 0L else (seconds * 1000).toLong()
} catch (e: Exception) {
    LogCat.e("readAssetDurationMs: ${e.message}")
    0L
}

private fun parseSvgSize(path: String): IntSize? {
    return try {
        val nsData = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
        val text = nsData.toByteArray().decodeToString()
        val wMatch = Regex("""<svg[^>]*\bwidth\s*=\s*"?([0-9.]+)""", RegexOption.IGNORE_CASE).find(text)
        val hMatch = Regex("""<svg[^>]*\bheight\s*=\s*"?([0-9.]+)""", RegexOption.IGNORE_CASE).find(text)
        val w = wMatch?.groupValues?.get(1)?.toIntOrNull()
        val h = hMatch?.groupValues?.get(1)?.toIntOrNull()
        if (w != null && h != null) IntSize(w, h) else null
    } catch (e: Exception) {
        null
    }
}
