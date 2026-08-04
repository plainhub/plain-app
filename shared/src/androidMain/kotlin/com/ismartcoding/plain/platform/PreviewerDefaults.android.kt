package com.ismartcoding.plain.platform

import android.graphics.BitmapRegionDecoder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.enums.ImageType
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

@Composable
fun rememberDecoderImagePainter(
    inputStream: InputStream,
    rotation: Int,
): ImageDecoder? {
    var imageDecoder by remember { mutableStateOf<ImageDecoder?>(null) }
    LaunchedEffect(inputStream) {
        launch {
            imageDecoder = withIO {
                try {
                    val decoder = if (isSPlus()) {
                        BitmapRegionDecoder.newInstance(inputStream)
                    } else {
                        BitmapRegionDecoder.newInstance(inputStream, false)
                    }
                    if (decoder == null) {
                        null
                    } else {
                        ImageDecoder(decoder = decoder, rotation = rotation)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            imageDecoder?.release()
        }
    }
    return imageDecoder
}

private val fileExistsCache = ConcurrentHashMap<String, Boolean>()

@Composable
actual fun getModel(item: PreviewItem): Any {
    if (item.isVideo() || item.path.isUrl()) return item
    if (item.size <= 2000 * 1000) return item

    val imageType = remember { ImageHelper.getImageType(item.path, item.path) }
    if (imageType.isApplicableAnimated() || imageType == ImageType.SVG) return item

    val rotation = remember {
        if (item.rotation == -1) item.rotation = ImageHelper.getRotation(item.path)
        item.rotation
    }
    val fileExists = remember(item.path) { fileExistsCache.getOrPut(item.path) { File(item.path).exists() } }
    if (!fileExists) return item

    val inputStream = remember(item.path) {
        try { File(item.path).inputStream() } catch (e: Exception) { fileExistsCache.remove(item.path); null }
    }
    val decoder = if (inputStream != null) rememberDecoderImagePainter(inputStream = inputStream, rotation = rotation) else null
    if (decoder != null) item.intrinsicSize = IntSize(decoder.decoderWidth, decoder.decoderHeight)
    return decoder ?: item
}
