@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.darwin.NSObject
import platform.objc.sel_registerName

actual suspend fun getAudioMetadata(path: String): Pair<String, String> = withIO {
    extractMetadata(path)
}

actual fun playlistAudioFromPath(path: String): DPlaylistAudio {
    val fallbackTitle = path.getFilenameWithoutExtensionFromPath()
    val (title, artist) = tryExtractMetadata(path) ?: (fallbackTitle to "")
    val duration = tryExtractDurationMs(path)
    return DPlaylistAudio(
        title = title.ifEmpty { fallbackTitle },
        path = path,
        artist = artist,
        duration = duration,
    )
}

actual fun loadAudioCoverBitmap(path: String): ImageBitmap? = try {
    extractArtworkData(path)?.let { nsData ->
        val skiaBitmap = Bitmap.makeFromImage(Image.makeFromEncoded(nsData.toByteArray()))
        skiaBitmap.asComposeImageBitmap()
    }
} catch (e: Exception) {
    LogCat.e("loadAudioCoverBitmap: ${e.message}")
    null
}

actual fun getAudioAlbumArtFileId(audio: DAudio): String = ""

private fun tryExtractMetadata(path: String): Pair<String, String>? {
    return try {
        extractMetadata(path)
    } catch (e: Exception) {
        LogCat.e("tryExtractMetadata: ${e.message}")
        null
    }
}

private fun extractMetadata(path: String): Pair<String, String> {
    val fallbackTitle = path.getFilenameWithoutExtensionFromPath()
    return try {
        val asset = AVURLAsset(NSURL.fileURLWithPath(path), null)
        val metadata = asset.performSelector(sel_registerName("commonMetadata")) as? List<*>
            ?: return fallbackTitle to ""
        var title = ""
        var artist = ""
        for (raw in metadata) {
            val item = raw as? NSObject ?: continue
            val key = item.performSelector(sel_registerName("commonKey")) as? String
            when (key) {
                "title" -> {
                    (item.performSelector(sel_registerName("stringValue")) as? String)?.let { title = it }
                }
                "artist" -> {
                    (item.performSelector(sel_registerName("stringValue")) as? String)?.let { artist = it }
                }
            }
        }
        title.ifEmpty { fallbackTitle } to artist
    } catch (e: Exception) {
        LogCat.e("extractMetadata: ${e.message}")
        fallbackTitle to ""
    }
}

private fun tryExtractDurationMs(path: String): Long {
    return try {
        val asset = AVURLAsset(NSURL.fileURLWithPath(path), null)
        val seconds = CMTimeGetSeconds(asset.duration)
        if (seconds.isNaN() || seconds <= 0.0) 0L else (seconds * 1000).toLong()
    } catch (e: Exception) {
        LogCat.e("tryExtractDurationMs: ${e.message}")
        0L
    }
}

private fun extractArtworkData(path: String): NSData? = try {
    val asset = AVURLAsset(NSURL.fileURLWithPath(path), null)
    val metadata = asset.performSelector(sel_registerName("commonMetadata")) as? List<*>
        ?: return null
    for (raw in metadata) {
        val item = raw as? NSObject ?: continue
        val key = item.performSelector(sel_registerName("commonKey")) as? String
        if (key == "artwork") {
            return item.performSelector(sel_registerName("value")) as? NSData
        }
    }
    null
} catch (e: Exception) {
    LogCat.e("extractArtworkData: ${e.message}")
    null
}
