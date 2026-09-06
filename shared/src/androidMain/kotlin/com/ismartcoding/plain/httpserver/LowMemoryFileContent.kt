package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.withIO
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import java.io.File
import java.io.FileInputStream

private const val FILE_RESPONSE_BUFFER_SIZE = 64 * 1024
private const val FILE_RESPONSE_FLUSH_BYTES = 64 * 1024
internal const val BROWSER_MEDIA_RANGE_BYTES = 4L * 1024 * 1024

internal data class ResolvedFileRange(
    val start: Long,
    val endInclusive: Long,
    val isPartial: Boolean,
) {
    val length: Long
        get() = if (endInclusive >= start) endInclusive - start + 1 else 0
}

internal fun fullFileRange(fileLength: Long): ResolvedFileRange =
    ResolvedFileRange(
        start = 0,
        endInclusive = fileLength - 1,
        isPartial = false,
    )

internal data class FileResponsePlan(
    val range: ResolvedFileRange,
    val useBufferedResponse: Boolean,
)

private fun String?.isMediaType(): Boolean {
    if (isNullOrBlank()) return false
    return startsWith("video/", ignoreCase = true) || startsWith("audio/", ignoreCase = true)
}

/**
 * HTML media elements support incremental range requests. Bound each requested
 * video/audio range so Ktor can hand Netty one ByteArray-backed message instead
 * of converting thousands of channel segments on affected Android runtimes.
 * Ordinary downloads and requests without a Range header keep streaming.
 * Media elements are detected via Sec-Fetch-Dest, with the response content
 * type as fallback for WebKit clients (Safari/WKWebView) that never send it.
 */
internal fun resolveFileResponsePlan(
    rangeHeader: String?,
    fileLength: Long,
    fetchDestination: String?,
    contentType: String? = null,
): FileResponsePlan? {
    val requested = resolveSingleByteRange(rangeHeader, fileLength) ?: return null
    val isMediaElement = fetchDestination.equals("video", ignoreCase = true) ||
        fetchDestination.equals("audio", ignoreCase = true) ||
        contentType.isMediaType()
    if (!isMediaElement || !requested.isPartial) {
        return FileResponsePlan(requested, useBufferedResponse = false)
    }

    val bufferedLength = minOf(requested.length, BROWSER_MEDIA_RANGE_BYTES)
    val limitedEnd = requested.start + bufferedLength - 1
    return FileResponsePlan(
        range = ResolvedFileRange(requested.start, limitedEnd, isPartial = true),
        useBufferedResponse = true,
    )
}

/**
 * Resolve the single byte-range form used by browsers/downloaders. Multipart
 * ranges are intentionally ignored so APK downloads stay on a one-pass,
 * fixed-buffer response path. Returns null when the range is unsatisfiable.
 */
internal fun resolveSingleByteRange(rangeHeader: String?, fileLength: Long): ResolvedFileRange? {
    if (rangeHeader.isNullOrBlank()) return fullFileRange(fileLength)

    val value = rangeHeader.trim()
    if (!value.startsWith("bytes=", ignoreCase = true)) return fullFileRange(fileLength)

    val spec = value.substringAfter('=').trim()
    if (spec.isEmpty() || spec.contains(',')) return fullFileRange(fileLength)

    val dashIndex = spec.indexOf('-')
    if (dashIndex < 0) return fullFileRange(fileLength)

    val startPart = spec.substring(0, dashIndex).trim()
    val endPart = spec.substring(dashIndex + 1).trim()
    if (startPart.isEmpty() && endPart.isEmpty()) return fullFileRange(fileLength)
    if (fileLength <= 0) return null

    if (startPart.isEmpty()) {
        val suffixLength = endPart.toLongOrNull() ?: return fullFileRange(fileLength)
        if (suffixLength <= 0) return null
        val start = (fileLength - suffixLength).coerceAtLeast(0)
        return ResolvedFileRange(start, fileLength - 1, isPartial = true)
    }

    val start = startPart.toLongOrNull() ?: return fullFileRange(fileLength)
    if (start < 0) return fullFileRange(fileLength)
    if (start >= fileLength) return null

    val requestedEnd = if (endPart.isEmpty()) {
        fileLength - 1
    } else {
        endPart.toLongOrNull() ?: return fullFileRange(fileLength)
    }
    if (requestedEnd < start) return null

    return ResolvedFileRange(
        start = start,
        endInclusive = minOf(requestedEnd, fileLength - 1),
        isPartial = true,
    )
}

/**
 * Streams a file (optionally a byte range of it) through a fixed-size buffer
 * so that serving a large APK never allocates heap proportional to file size.
 */
internal class LowMemoryFileContent(
    private val file: File,
    override val contentType: ContentType?,
    override val status: HttpStatusCode,
    override val contentLength: Long,
    private val range: ResolvedFileRange,
    private val totalLength: Long,
) : OutgoingContent.WriteChannelContent() {
    override val headers: Headers = if (range.isPartial) {
        headersOf(
            "Accept-Ranges" to listOf("bytes"),
            "Content-Range" to listOf("bytes ${range.start}-${range.endInclusive}/$totalLength"),
        )
    } else {
        headersOf("Accept-Ranges" to listOf("bytes"))
    }

    override suspend fun writeTo(channel: ByteWriteChannel) {
        copyFileRangeToChannel(file, range.start, contentLength, channel)
    }
}

private suspend fun copyFileRangeToChannel(
    file: File,
    start: Long,
    length: Long,
    channel: ByteWriteChannel,
) {
    if (length <= 0) {
        channel.flush()
        return
    }

    withIO {
        FileInputStream(file).use { input ->
            input.channel.position(start)
            val buffer = ByteArray(FILE_RESPONSE_BUFFER_SIZE)
            var remaining = length
            var bytesSinceFlush = 0

            while (remaining > 0) {
                val readLength = minOf(buffer.size.toLong(), remaining).toInt()
                val read = input.read(buffer, 0, readLength)
                if (read <= 0) break

                channel.writeFully(buffer, 0, read)
                remaining -= read
                bytesSinceFlush += read

                if (bytesSinceFlush >= FILE_RESPONSE_FLUSH_BYTES) {
                    channel.flush()
                    bytesSinceFlush = 0
                }
            }
            channel.flush()
        }
    }
}
