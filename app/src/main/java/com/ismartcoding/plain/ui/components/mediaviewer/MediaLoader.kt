package com.ismartcoding.plain.ui.components.mediaviewer

import android.graphics.BitmapRegionDecoder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.isSPlus
import com.ismartcoding.plain.ui.components.mediaviewer.hugeimage.ImageDecoder
import kotlinx.coroutines.launch
import java.io.InputStream

@OptIn(ExperimentalCoilApi::class)
@Composable
fun rememberCoilImagePainter(path: String): Painter {
    val imageRequest = ImageRequest
        .Builder(LocalContext.current)
        .data(path)
        .size(coil3.size.Size.ORIGINAL)
        .build()
    return rememberAsyncImagePainter(imageRequest)
}


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