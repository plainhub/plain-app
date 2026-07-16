@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.ismartcoding.plain.platform

import android.graphics.Bitmap
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.decode.ContentMetadata
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.fetch.SourceFetchResult
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.svg.SvgDecoder
import coil3.svg.internal.MIME_TYPE_SVG
import coil3.svg.isSvg
import coil3.toAndroidUri
import coil3.video.VideoFrameDecoder
import com.ismartcoding.plain.activityManager
import com.ismartcoding.plain.api.OkHttpClientFactory
import kotlin.math.roundToInt

/**
 * A Coil3 [Decoder] that extracts a thumbnail frame from a video file using
 * [MediaMetadataRetriever], bypassing the normal MIME-type check.
 *
 * This is needed for videos stored under content-addressable hash paths (fid: URIs)
 * which have no file extension, so Coil cannot detect the MIME type and therefore
 * skips [coil3.video.VideoFrameDecoder] by default.
 *
 * Register [Factory] on a specific [coil3.request.ImageRequest] (not globally) via
 * [coil3.request.ImageRequest.Builder.components] so that only known video requests
 * use this decoder.
 */
class ForceVideoDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val filePath = source.file().toString()

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val bitmap = retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: throw kotlinx.io.IOException("ForceVideoDecoder: failed to extract video frame from $filePath")

            return DecodeResult(
                image = bitmap.toDrawable(options.context.resources).asImage(),
                isSampled = false,
            )
        } finally {
            retriever.release()
        }
    }

    /**
     * A [Decoder.Factory] that creates a [ForceVideoDecoder] without any MIME-type check.
     *
     * Only add this to an [coil3.request.ImageRequest] when you are certain the target
     * file is a video (e.g. based on the original filename, not the path).
     */
    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            // Only applicable to file-backed sources (fid: / regular file paths)
            return ForceVideoDecoder(result.source, options)
        }
    }
}

fun newImageLoader(context: PlatformContext): ImageLoader {
    // Always use applicationContext to avoid leaking Activity instances through
    // the lazy diskCache / memoryCache initializer lambdas held by RealImageLoader.
    val appContext = context.applicationContext
    val memoryPercent = if (activityManager.isLowRamDevice) 0.25 else 0.75

    // Dedicated OkHttp client for image loading. It uses the system trust store and
    // OkHttp's default OkHostnameVerifier, which is what every other Android image
    // fetcher does. We deliberately do NOT reuse `createUnsafeOkHttpClient()` here:
    // that client is built for the in-app HTTP server and only accepts
    // `isLocalNetworkAddress` hostnames, which silently broke every public https
    // image (the request fails with `SSLPeerUnverifiedException` before the
    // network is even reached).
    val imageLoaderClient = OkHttpClientFactory.createImageLoaderClient()

    return ImageLoader.Builder(appContext)
        .components {
            add(SvgDecoder.Factory(true))
            add(AnimatedImageDecoder.Factory())
            // ThumbnailDecoder must be before VideoFrameDecoder: for content:// video URIs,
            // ThumbnailDecoder uses ContentResolver.loadThumbnail() which reads the pre-generated
            // MediaStore thumbnail cache (fast). VideoFrameDecoder uses MediaMetadataRetriever
            // which opens and decodes the full video file (slow). ThumbnailDecoder only fires for
            // content:// URIs (ContentMetadata check), so file-based paths still reach VideoFrameDecoder.
            add(ThumbnailDecoder.Factory())
            add(VideoFrameDecoder.Factory()) // fallback for file:// video paths without content URI
            add(OkHttpNetworkFetcherFactory(imageLoaderClient))
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(appContext, percent = memoryPercent)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(appContext.cacheDir.resolve("image_cache").absoluteFile)
                .maxSizePercent(1.0)
                .build()
        }
        .crossfade(100)
        .build()
}

class ThumbnailDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    @RequiresApi(Build.VERSION_CODES.Q)
    @OptIn(ExperimentalCoilApi::class)
    override suspend fun decode(): DecodeResult {
        val metadata = source.metadata as ContentMetadata
        val bitmap = options.context.contentResolver.loadThumbnail(
            metadata.uri.toAndroidUri(),
            options.size.toAndroidSize(),
            null
        )
        val normalizedBitmap = normalizeBitmap(bitmap, options.size)


        return DecodeResult(
            image = normalizedBitmap.toDrawable(options.context.resources).asImage(),
            isSampled = true,
        )
    }


    /** Return [inBitmap] or a copy of [inBitmap] that is valid for the input [options] and [size]. */
    private fun normalizeBitmap(inBitmap: Bitmap, size: Size): Bitmap {
        // Fast path: if the input bitmap is valid, return it.
        if (isConfigValid(inBitmap, options) && isSizeValid(inBitmap, options, size)) {
            return inBitmap
        }

        // Slow path: re-render the bitmap with the correct size + config.
        val scale = DecodeUtils.computeSizeMultiplier(
            srcWidth = inBitmap.width,
            srcHeight = inBitmap.height,
            dstWidth = size.width.pxOrElse { inBitmap.width },
            dstHeight = size.height.pxOrElse { inBitmap.height },
            scale = options.scale,
        ).toFloat()
        val dstWidth = (scale * inBitmap.width).roundToInt()
        val dstHeight = (scale * inBitmap.height).roundToInt()
        val safeConfig = when {
            options.bitmapConfig == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
            else -> options.bitmapConfig
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val outBitmap = createBitmap(dstWidth, dstHeight, safeConfig)
        outBitmap.applyCanvas {
            scale(scale, scale)
            drawBitmap(inBitmap, 0f, 0f, paint)
        }
        inBitmap.recycle()

        return outBitmap
    }

    private fun isConfigValid(bitmap: Bitmap, options: Options): Boolean {
        return bitmap.config != Bitmap.Config.HARDWARE ||
                options.bitmapConfig == Bitmap.Config.HARDWARE
    }

    private fun isSizeValid(bitmap: Bitmap, options: Options, size: Size): Boolean {
        // Thumbnails from ContentResolver.loadThumbnail() are already scaled to the requested
        // size. Accept any bitmap that already fits within the requested bounds (multiplier <= 1.0)
        // to avoid an unnecessary slow-path software canvas redraw.
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = bitmap.width,
            srcHeight = bitmap.height,
            dstWidth = size.width.pxOrElse { bitmap.width },
            dstHeight = size.height.pxOrElse { bitmap.height },
            scale = options.scale,
        )
        return multiplier <= 1.0
    }

    private fun Size.toAndroidSize(fallbackWidth: Int = 200, fallbackHeight: Int = 200) =
        android.util.Size(
            width.pxOrElse { fallbackWidth },
            height.pxOrElse { fallbackHeight }
        )

    class Factory : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result)) return null
            return ThumbnailDecoder(result.source, options)
        }

        private fun isApplicable(result: SourceFetchResult): Boolean {
            return with(result) {
                isQPlus() &&  mimeType != null && mimeType!!.isVideoOrImage &&
                        source.metadata is ContentMetadata && !isSvg(result)
            }
        }

        private val String.isVideoOrImage get() = startsWith("video/") || startsWith("image/")

        private fun isSvg(result: SourceFetchResult) = result.mimeType == MIME_TYPE_SVG || DecodeUtils.isSvg(result.source.source())

    }
}
