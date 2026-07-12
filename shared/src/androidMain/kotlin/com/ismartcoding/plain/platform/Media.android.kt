package com.ismartcoding.plain.platform

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.data.DImageMeta
import com.ismartcoding.plain.data.DVideoMeta
import com.ismartcoding.plain.extensions.getDuration
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.QrCodeGenerateHelper
import com.ismartcoding.plain.helpers.QrCodeScanHelper
import com.ismartcoding.plain.helpers.VideoHelper
import com.ismartcoding.plain.ui.components.renameAndScan
import java.io.File

actual fun getImageRotation(path: String): Int = ImageHelper.getRotation(path)

actual fun getImageIntrinsicSize(path: String, rotation: Int): IntSize = ImageHelper.getIntrinsicSize(path, rotation)

actual fun getVideoIntrinsicSize(path: String): IntSize = VideoHelper.getIntrinsicSize(path)

actual fun getVideoMeta(path: String): DVideoMeta? = VideoHelper.getMeta(path)

actual fun getImageMeta(path: String): DImageMeta? = ImageHelper.getMeta(path)

actual fun tryDecodeQrCode(path: String): String? {
    return try {
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        QrCodeScanHelper.tryDecode(bitmap)?.text
    } catch (e: Exception) {
        null
    }
}

actual fun fileLength(path: String): Long {
    return runCatching { File(path).takeIf { it.exists() }?.length() ?: 0L }.getOrDefault(0L)
}

actual suspend fun renameMediaFile(path: String, newName: String): String? = renameAndScan(path, newName)

actual fun getMediaDurationMs(path: String): Long = File(path).getDuration(appContext)

actual fun getAudioDurationMsFromPath(path: String): Long =
    DPlaylistAudio.fromPath(appContext, path).duration

actual fun generateQrCode(text: String, width: Int, height: Int): ImageBitmap {
    return QrCodeGenerateHelper.generate(text, width, height).asImageBitmap()
}
