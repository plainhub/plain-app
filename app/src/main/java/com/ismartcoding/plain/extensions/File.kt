package com.ismartcoding.plain.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.PictureDrawable
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import androidx.core.graphics.drawable.toBitmap
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Scale
import com.ismartcoding.lib.androidsvg.SVG
import com.ismartcoding.lib.extensions.compress
import com.ismartcoding.lib.extensions.getMediaContentUri
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isPartialSupportVideo
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.extensions.pathToMediaStoreUri
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.logcat.LogCat
import java.io.ByteArrayOutputStream
import java.io.File

fun File.getDirectChildrenCount(countHiddenItems: Boolean): Int {
    if (countHiddenItems) {
        return list()?.size ?: 0
    }
    return list()?.filter {
        !it.startsWith('.')
    }?.size ?: 0
}

fun File.newName(): String {
    var index = 1
    var candidate: String
    val split = nameWithoutExtension.split(' ').toMutableList()
    val last = split.last()
    if ("""^\(\d+\)$""".toRegex().matches(last)) {
        split.remove(last)
    }
    val name = split.joinToString(" ")
    while (true) {
        candidate = if (extension.isEmpty()) "$name ($index)" else "$name ($index).$extension"
        if (!File("$parent/$candidate").exists()) {
            return candidate
        }
        index++
    }
}

fun File.newPath(): String {
    return "$parent/" + newName()
}

fun File.newFile(): File {
    return File(newPath())
}

suspend fun File.getBitmapAsync(
    context: Context,
    width: Int,
    height: Int,
    centerCrop: Boolean = true,
    mediaId: String = ""
): Bitmap? {
    if (path.endsWith(".svg", true)) {
        val svg = SVG.getFromString(readText())
        val picture = svg.renderToPicture(width, height)
        val drawable = PictureDrawable(picture)
        return drawable.toBitmap(width, height)
    }

    var bitmap: Bitmap? = null
    if (isQPlus() && this.path.isVideoFast()) {
        val contentUri = if (mediaId.isNotEmpty()) path.pathToMediaStoreUri(mediaId) else context.contentResolver.getMediaContentUri(path)
        if (contentUri != null) {
            try {
                bitmap = context.contentResolver.loadThumbnail(contentUri, Size(width, height), null)
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
            }
        }
        if (bitmap != null) {
            return bitmap
        }
    }

    if (this.path.isPartialSupportVideo()) {
        try {
            bitmap =
                if (isQPlus()) {
                    ThumbnailUtils.createVideoThumbnail(this, Size(width, height), null)
                } else {
                    ThumbnailUtils.createVideoThumbnail(this.absolutePath, MediaStore.Video.Thumbnails.MICRO_KIND)
                }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }
    } else {
        try {
            val imageLoader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(this)
                .size(width, height)
                .scale(if (centerCrop) Scale.FILL else Scale.FIT)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            bitmap = (result.image as? BitmapImage)?.bitmap
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }
    }
    return bitmap
}

suspend fun File.toThumbBytesAsync(
    context: Context,
    width: Int,
    height: Int,
    centerCrop: Boolean,
    mediaId: String
): ByteArray? {
    val bitmap = getBitmapAsync(context, width, height, centerCrop, mediaId) ?: return null
    val stream = ByteArrayOutputStream()
    bitmap.compress(80, stream)
    return stream.toByteArray()
}

fun File.getDuration(context: Context): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, Uri.fromFile(this))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        (time?.toLong()?.div(1000)) ?: 0L
    } catch (ex: Exception) {
        0L
    } finally {
        runCatching { retriever.release() }
    }
}