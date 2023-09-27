package com.ismartcoding.lib.extensions

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.Downsampler
import com.bumptech.glide.request.RequestOptions
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.logcat.LogCat
import java.io.ByteArrayOutputStream
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

suspend fun File.getBitmapAsync(
    context: Context,
    width: Int,
    height: Int,
    centerCrop: Boolean = true,
): Bitmap? {
    var bitmap: Bitmap? = null
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
            var options =
                RequestOptions()
                    .set(Downsampler.ALLOW_HARDWARE_CONFIG, true)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(width, height)
            if (centerCrop) {
                options = options.centerCrop()
            }
            bitmap =
                Glide.with(context).asBitmap().load(this)
                    .apply(options)
                    .submit().get()
            // https://stackoverflow.com/questions/58314397/java-lang-illegalstateexception-software-rendering-doesnt-support-hardware-bit
//            bitmap = d.copy(Bitmap.Config.ARGB_8888, false)
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
): ByteArray {
    val stream = ByteArrayOutputStream()
    getBitmapAsync(context, width, height, centerCrop)?.let {
        if (this@toThumbBytesAsync.name.endsWith(".png", true)) {
            it.compress(Bitmap.CompressFormat.PNG, 70, stream)
        } else {
            it.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        }
    }
    return stream.toByteArray()
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
