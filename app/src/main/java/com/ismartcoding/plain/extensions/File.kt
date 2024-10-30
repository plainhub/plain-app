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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.bitmap.Downsampler
import com.bumptech.glide.request.RequestOptions
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

fun File.getBitmapAsync(
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
            // if file size less than 500KB just load it directly
            if (width == 1024 && height == 1024 && length() < 500 * 1024) {
                bitmap = Glide.with(context).asBitmap().load(this).submit().get()
            } else {
                var options =
                    RequestOptions()
                        .set(Downsampler.ALLOW_HARDWARE_CONFIG, true)
                        .downsample(DownsampleStrategy.CENTER_INSIDE)
//                    .format(DecodeFormat.PREFER_RGB_565)
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
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }
    }
    return bitmap
}

fun File.toThumbBytesAsync(
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
    if (!this.name.isVideoFast() && !this.name.isAudioFast()) {
        return 0L
    }
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, Uri.fromFile(this))
    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    retriever.release()
    return (time?.toLong()?.div(1000)) ?: 0L
}