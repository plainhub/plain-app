@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.ismartcoding.plain.ui.base.coil

import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.decode.ContentMetadata
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.bitmapConfig
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.svg.internal.MIME_TYPE_SVG
import coil3.svg.isSvg
import coil3.toAndroidUri
import com.ismartcoding.lib.isQPlus
import kotlin.math.roundToInt

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
//        if (options.allowInexactSize) return true // TODO: Fix this.
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = bitmap.width,
            srcHeight = bitmap.height,
            dstWidth = size.width.pxOrElse { bitmap.width },
            dstHeight = size.height.pxOrElse { bitmap.height },
            scale = options.scale,
        )
        return multiplier == 1.0
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