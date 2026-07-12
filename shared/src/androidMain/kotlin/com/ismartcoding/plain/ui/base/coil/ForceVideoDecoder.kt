package com.ismartcoding.plain.ui.base.coil

import android.media.MediaMetadataRetriever
import androidx.core.graphics.drawable.toDrawable
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options

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
