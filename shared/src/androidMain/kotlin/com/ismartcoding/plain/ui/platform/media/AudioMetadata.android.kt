package com.ismartcoding.plain.ui.platform.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ismartcoding.plain.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun decodeLocalAudioCover(path: String): ImageBitmap? =
    decodeAudioArtwork(path, 1024)?.asImageBitmap()

internal suspend fun decodeAudioArtwork(path: String, maxSize: Int): Bitmap? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    val bytes = try {
        val uri = Uri.parse(path)
        if (uri.scheme == "content" || uri.scheme == "file") retriever.setDataSource(appContext, uri)
        else retriever.setDataSource(path)
        retriever.embeddedPicture
    } finally {
        retriever.release()
    } ?: return@withContext null
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null
    var sample = 1
    while (maxOf(options.outWidth, options.outHeight) / sample > maxSize) sample *= 2
    options.inJustDecodeBounds = false
    options.inSampleSize = sample
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}
