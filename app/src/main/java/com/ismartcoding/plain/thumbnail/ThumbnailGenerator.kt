package com.ismartcoding.plain.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.PictureDrawable
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.util.Size
import androidx.core.graphics.drawable.toBitmap
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Scale
import com.ismartcoding.lib.androidsvg.SVG
import com.ismartcoding.lib.extensions.getMediaContentUri
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isPartialSupportVideo
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.extensions.pathToMediaStoreUri
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.logcat.LogCat
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Generates thumbnails for images, videos, and SVG files.
 *
 * Decode pipeline for images:
 *  1. SVG → render via AndroidSVG at target size.
 *  2. Video → MediaStore.loadThumbnail (Q+) or ThumbnailUtils.
 *  3. Image → [decodeSampledBitmapFromFile] fast BitmapFactory path
 *     (single-pass: inSampleSize + inDensity/inTargetDensity).
 *  4. Fallback → Coil (for formats BitmapFactory cannot decode, e.g. AVIF, HEIC).
 *
 * Encoding:
 *  JPEG at quality 85 — ~4-8× faster to encode than WebP, sufficient for thumbnails.
 *
 * Caching:
 *  Disk cache in [ThumbnailCache] keyed by path + mtime + dimensions + mode.
 *  mediaId-based (MediaStore) thumbnails are excluded from disk cache since
 *  they are already served from MediaStore's own cache layer.
 */
object ThumbnailGenerator {

    /**
     * Generate a thumbnail [Bitmap] for the given file.
     * Returns null on failure; callers must recycle the bitmap after use.
     */
    suspend fun getBitmapAsync(
        context: Context,
        file: File,
        width: Int,
        height: Int,
        centerCrop: Boolean = true,
        mediaId: String = "",
        fileName: String = "",
    ): Bitmap? {
        val effectiveName = fileName.ifEmpty { file.name }
        if (effectiveName.endsWith(".svg", true)) {
            return try {
                val svg = SVG.getFromString(file.readText())
                val picture = svg.renderToPicture(width, height)
                PictureDrawable(picture).toBitmap(width, height)
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
                null
            }
        }

        var bitmap: Bitmap? = null

        // MediaStore thumbnail via ContentResolver.loadThumbnail (API 29+).
        // Covers images, videos, and audio. For images/audio we only attempt this when
        // mediaId is known (path lookup would require a full content-resolver scan).
        if (isQPlus()) {
            val isMedia = effectiveName.isVideoFast() || effectiveName.isImageFast() || effectiveName.isAudioFast()
            if (isMedia) {
                val contentUri = when {
                    mediaId.isNotEmpty() -> file.path.pathToMediaStoreUri(mediaId)
                    effectiveName.isVideoFast() -> context.contentResolver.getMediaContentUri(file.path)
                    else -> null  // images/audio without mediaId: skip to BitmapFactory
                }
                if (contentUri != null) {
                    try {
                        bitmap = context.contentResolver.loadThumbnail(contentUri, Size(width, height), null)
                    } catch (ex: Exception) {
                        LogCat.e(ex.toString())
                    }
                }
                if (bitmap != null) return bitmap
            }
        }

        if (effectiveName.isPartialSupportVideo()) {
            return try {
                if (isQPlus()) {
                    ThumbnailUtils.createVideoThumbnail(file, Size(width, height), null)
                } else {
                    @Suppress("DEPRECATION")
                    ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Video.Thumbnails.MICRO_KIND)
                }
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
                null
            }
        }

        if (effectiveName.isVideoFast()) {
            return try {
                if (isQPlus()) {
                    ThumbnailUtils.createVideoThumbnail(file, Size(width, height), null)
                } else {
                    @Suppress("DEPRECATION")
                    ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Video.Thumbnails.MICRO_KIND)
                }
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
                null
            }
        }

        // Fast BitmapFactory path (single-pass decode + scale + optional sharpen)
        try {
            bitmap = decodeSampledBitmapFromFile(file.absolutePath, width, height, centerCrop)
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }

        // Coil fallback for formats BitmapFactory cannot decode (AVIF, HEIC, embedded audio art…)
        if (bitmap == null) {
            try {
                val imageLoader = SingletonImageLoader.get(context)
                val request = ImageRequest.Builder(context)
                    .data(file)
                    .size(width, height)
                    .scale(if (centerCrop) Scale.FILL else Scale.FIT)
                    .allowHardware(false)
                    .build()
                bitmap = (imageLoader.execute(request).image as? BitmapImage)?.bitmap
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
            }
        }

        return bitmap
    }

    /**
     * Generate thumbnail JPEG bytes for the given file, using the disk cache.
     *
     * Returns null if the file cannot be decoded.
     */
    suspend fun toThumbBytesAsync(
        context: Context,
        file: File,
        width: Int,
        height: Int,
        centerCrop: Boolean,
        mediaId: String,
        fileName: String = "",
    ): ByteArray? {
        // Priority 1: Android system thumbnail (MediaStore already manages its own cache layer).
        // Return immediately without touching our disk cache.
        if (mediaId.isNotEmpty() && width <= 512) {
            val bitmap = getBitmapAsync(context, file, width, height, centerCrop, mediaId, fileName)
            if (bitmap != null) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                bitmap.recycle()
                return stream.toByteArray()
            }
        }

        // Priority 2: Our disk cache — avoids decode entirely on repeated requests (~20 ms read vs ~200 ms decode)
        ThumbnailCache.get(context, file.absolutePath, width, height, centerCrop)?.let { return it }

        // Priority 3: Self-generate, then cache the result
        val bitmap = getBitmapAsync(context, file, width, height, centerCrop, fileName = fileName) ?: return null
        val stream = ByteArrayOutputStream()
        // JPEG is ~4-8× faster to encode than WebP, adequate quality for thumbnails
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        bitmap.recycle()
        val bytes = stream.toByteArray()

        ThumbnailCache.put(context, file.absolutePath, width, height, centerCrop, bytes)

        return bytes
    }
}
