package com.ismartcoding.plain.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.data.DImageMeta
import com.ismartcoding.plain.data.DVideoMeta

actual fun getImageRotation(path: String): Int = 0

actual fun getImageIntrinsicSize(path: String, rotation: Int): IntSize = IntSize.Zero

actual fun getVideoIntrinsicSize(path: String): IntSize = IntSize.Zero

actual fun getVideoMeta(path: String): DVideoMeta? = null

actual fun getImageMeta(path: String): DImageMeta? = null

actual fun tryDecodeQrCode(path: String): String? = null

actual fun fileLength(path: String): Long = 0L

actual suspend fun renameMediaFile(path: String, newName: String): String? = null

actual fun getMediaDurationMs(path: String): Long = 0L

actual fun getAudioDurationMsFromPath(path: String): Long = 0L

actual fun generateQrCode(text: String, width: Int, height: Int): ImageBitmap {
    return ImageBitmap(width, height, ImageBitmapConfig.Argb8888)
}

actual fun getSvgSize(path: String): IntSize = IntSize(150, 150)

actual fun addMediaShortcut(path: String, label: String) {
    // iOS stub — home screen shortcuts not supported
}
