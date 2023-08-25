package com.ismartcoding.lib.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import coil.decode.DataSource
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.request.videoFrameMillis
import coil.request.videoFrameOption
import com.commit451.coiltransformations.CropTransformation
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.logcat.LogCat
import java.io.File

fun File.getDirectChildrenCount(countHiddenItems: Boolean): Int {
    return listFiles()?.filter {
        if (countHiddenItems) {
            true
        } else {
            !it.name.startsWith('.')
        }
    }?.size ?: 0
}

fun File.newName(): String {
    var index = 1
    var candidate: String
    val split = nameWithoutExtension.split(' ').toMutableList()
    val last = split.last()
    if ("""^\(\d+\)$""".toRegex().matches(last)) {
        split.removeLast()
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

suspend fun File.getBitmapAsync(context: Context, width: Int, height: Int): Bitmap? {
    var bitmap: Bitmap? = null
    if (this.path.isPartialSupportVideo()) {
        try {
            bitmap = if (isQPlus()) {
                ThumbnailUtils.createVideoThumbnail(this, Size(width, height), null)
            } else {
                ThumbnailUtils.createVideoThumbnail(this.absolutePath, MediaStore.Video.Thumbnails.MICRO_KIND)
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }
    } else {
        try {
            val imageLoader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(this)
                .size(width, height)
                .videoFrameMillis(3000)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .transformations(CropTransformation())
                .build()
            val result = (imageLoader.execute(request) as? SuccessResult)
            bitmap = when (result?.dataSource) {
                DataSource.MEMORY_CACHE, DataSource.DISK -> {
                    (result.drawable as? BitmapDrawable)?.bitmap
                }
                else -> null
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }
    }
    return bitmap
}

fun File.getDuration(context: Context): Long {
    if (!this.name.isVideoFast() && !this.name.isAudioFast()) {
        return 0L
    }
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, Uri.fromFile(this))
    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    retriever.release()
    return (time?.toLong()?.div(1000)) ?: 0L
}