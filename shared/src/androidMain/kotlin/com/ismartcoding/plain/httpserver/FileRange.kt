package com.ismartcoding.plain.httpserver

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

/**
 * Upper bound for one browser media range response. Web pages keep several
 * <video> elements alive at once (grid previews, lightbox preload="auto"), and
 * an open-ended whole-file 206 lets each of them buffer the entire file, which
 * splits the phone's uplink across concurrent downloads and stalls playback
 * start. Bounding each response makes the browser finish its fetch and come
 * back for more only when it actually plays; Content-Range stays accurate so
 * follow-up requests continue from the right offset.
 */
internal const val BROWSER_MEDIA_RANGE_BYTES = 4L * 1024 * 1024

internal fun capBrowserMediaRange(range: ResolvedFileRange, fetchDestination: String?): ResolvedFileRange {
    val isMediaElement = fetchDestination.equals("video", ignoreCase = true) ||
        fetchDestination.equals("audio", ignoreCase = true)
    if (!isMediaElement || !range.isPartial || range.length <= BROWSER_MEDIA_RANGE_BYTES) {
        return range
    }
    return ResolvedFileRange(
        start = range.start,
        endInclusive = range.start + BROWSER_MEDIA_RANGE_BYTES - 1,
        isPartial = true,
    )
}

/**
 * Resolve the single byte-range form used by browsers/downloaders. Multipart
 * ranges are intentionally ignored so responses stay on the single-pass
 * file-region path. Returns null when the range is unsatisfiable.
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
