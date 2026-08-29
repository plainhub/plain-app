package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.urlEncode
import com.ismartcoding.plain.platform.convert3gpToMp4
import com.ismartcoding.plain.platform.decodeImageFileToPng
import com.ismartcoding.plain.platform.extractZipEntryToCache
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.getContentTypeForPath
import com.ismartcoding.plain.platform.getPackageIconBytes
import com.ismartcoding.plain.platform.getThumbnailBytes
import com.ismartcoding.plain.platform.isAnimatedImageOrSvg
import com.ismartcoding.plain.platform.isContentUri
import com.ismartcoding.plain.platform.readFileRange
import com.ismartcoding.plain.platform.statFile
import com.ismartcoding.plain.platform.streamContentUri
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.httpserver.http.HttpStatus

/**
 * Shared file-serving core used by `/fs` (main-UI, peer and shared-link modes).
 *
 * Given a resolved filesystem path (plus an optional media id / display name),
 * streams the bytes out with support for:
 * - zip virtual-path extraction
 * - byte-range requests (BLE low-throughput downloads)
 * - content:// URIs (3gp→MP4 conversion)
 * - package icons
 * - thumbnails (`w`/`h`/`cc` query params)
 * - HEIF→PNG conversion for browsers without native HEIF support
 * - download mode (`dl=1`) with Content-Disposition
 *
 * The route handlers only differ in how they authenticate + resolve the path
 * to a real device path before delegating here — byte output is not duplicated.
 */
object FileServer {
    private const val MAX_MANUAL_RANGE_LENGTH = 256 * 1024

    suspend fun serve(
        call: HttpCall,
        path: String,
        mediaId: String = "",
        jsonName: String = "",
    ) {
        val isDownload = call.queryParam("dl") == "1"
        val widthParam = call.queryParam("w")?.toIntOrNull()
        val heightParam = call.queryParam("h")?.toIntOrNull()
        // `cc` defaults to true (matches the original `!= false` behavior).
        val centerCrop = call.queryParam("cc")?.toBooleanStrictOrNull() != false

        // Zip virtual path: extract the entry to cache and serve it.
        if (ZipBrowserHelper.isZipPath(path)) {
            val cachePath = extractZipEntryToCache(path)
            if (cachePath == null) {
                call.respondNoBody(HttpStatus.NOT_FOUND)
                return
            }
            val zipEntryName = ZipBrowserHelper.getInternalPath(path).trimEnd('/').substringAfterLast('/')
            val displayName = jsonName.ifEmpty { zipEntryName }.urlEncode()
            val contentDisposition = if (isDownload) {
                "attachment; filename=\"${displayName}\"; filename*=utf-8''${displayName}"
            } else {
                "inline; filename=\"${displayName}\"; filename*=utf-8''${displayName}"
            }
            val contentType = getContentTypeForPath(cachePath) ?: "application/octet-stream"
            call.responseHeader("Access-Control-Expose-Headers", "Content-Disposition")
            call.respondFile(cachePath, contentType = contentType, contentDisposition = contentDisposition)
            return
        }

        // Byte-range request: used by low-throughput transports (BLE) to
        // download a file in small chunks. Only applies to regular files
        // (not content:// URIs or package icons) and skips all
        // conversion/thumbnail logic — raw bytes are served directly.
        val rangeOffset = call.queryParam("offset")?.toLongOrNull()
        val rangeLength = call.queryParam("length")?.toIntOrNull()
        if (rangeOffset != null && rangeLength != null && rangeLength > 0 &&
            !isContentUri(path) && !path.startsWith("pkgicon://")
        ) {
            if (rangeLength > MAX_MANUAL_RANGE_LENGTH) {
                call.respondText("range length is too large", status = HttpStatus.BAD_REQUEST)
                return
            }
            val bytes = readFileRange(path, rangeOffset, rangeLength)
            if (bytes == null) {
                call.respondNoBody(HttpStatus.NOT_FOUND)
            } else {
                call.respond(bytes, contentType = "application/octet-stream")
            }
            return
        }

        when {
            isContentUri(path) -> serveContentUri(
                call,
                path = path,
                jsonName = jsonName,
                isDownload = isDownload,
            )

            path.startsWith("pkgicon://") -> servePackageIcon(call, path.substring("pkgicon://".length))

            else -> serveRegularFile(
                call,
                path = path,
                jsonName = jsonName,
                mediaId = mediaId,
                isDownload = isDownload,
                widthParam = widthParam,
                heightParam = heightParam,
                centerCrop = centerCrop,
            )
        }
    }

    /**
     * Serve a `content://` URI: try 3gp→MP4 conversion first, then stream the
     * raw bytes. When [isDownload] is true, attach a `Content-Disposition:
     * attachment` header built from [jsonName] or the URI's last path segment.
     */
    private suspend fun serveContentUri(
        call: HttpCall,
        path: String,
        jsonName: String,
        isDownload: Boolean,
    ) {
        val mp4Bytes = convert3gpToMp4(path)
        if (mp4Bytes != null) {
            call.respond(mp4Bytes, contentType = "video/mp4")
            return
        }

        val headers = mutableMapOf<String, String>()
        if (isDownload) {
            val fileName = (jsonName.ifEmpty { path.substringAfterLast('/') }).urlEncode()
            if (fileName.isNotEmpty()) {
                headers["Access-Control-Expose-Headers"] = "Content-Disposition"
                headers["Content-Disposition"] =
                    "attachment; filename=\"${fileName}\"; filename*=utf-8''${fileName}"
            }
        }
        // Content-Type is unknown until we open the stream; let the platform default
        // to application/octet-stream. The browser infers the type from the
        // Content-Disposition filename extension when present.
        call.respondStream(headers = headers) { sink ->
            streamContentUri(path, sink)
        }
    }

    /** Serve a package icon as JPEG bytes. */
    private suspend fun servePackageIcon(call: HttpCall, packageName: String) {
        val bytes = getPackageIconBytes(packageName)
        if (bytes != null) {
            call.respond(bytes)
        } else {
            call.respondNoBody(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * Serve a regular file from the filesystem. Supports:
     * - Download mode (`dl=1`) with Content-Disposition attachment header
     * - Animated images / SVG served as-is with native range support
     * - Thumbnail generation when `w` and `h` query params are present
     * - HEIF → PNG conversion for browsers that cannot render HEIF natively
     * - Default: serve the file as-is
     */
    private suspend fun serveRegularFile(
        call: HttpCall,
        path: String,
        jsonName: String,
        mediaId: String,
        isDownload: Boolean,
        widthParam: Int?,
        heightParam: Int?,
        centerCrop: Boolean,
    ) {
        if (!fileExists(path)) {
            call.respondNoBody(HttpStatus.NOT_FOUND)
            return
        }
        val stat = statFile(path)
        if (stat == null || stat.isDir) {
            call.respondNoBody(HttpStatus.BAD_REQUEST)
            return
        }

        val fileName = (jsonName.ifEmpty { path.substringAfterLast('/') }).urlEncode()
        call.responseHeader("Access-Control-Expose-Headers", "Content-Disposition")

        if (isDownload) {
            val contentDisposition =
                "attachment; filename=\"${fileName}\"; filename*=utf-8''${fileName}"
            val contentType = getContentTypeForPath(path) ?: "application/octet-stream"
            call.respondFile(path, contentType = contentType, contentDisposition = contentDisposition)
            return
        }
        call.responseHeader("Content-Disposition", "inline; filename=\"${fileName}\"; filename*=utf-8''${fileName}")

        // Animated images (GIF, animated WebP, animated HEIF) and SVG: serve as-is
        // so the browser can render them with native range support.
        if (fileName.isImageFast() && isAnimatedImageOrSvg(path, fileName)) {
            val contentType = getContentTypeForPath(path) ?: "application/octet-stream"
            call.respondFile(path, contentType = contentType)
            return
        }

        // Thumbnail request: ?w=...&h=...[&cc=false]
        if (widthParam != null && heightParam != null) {
            val thumbBytes = getThumbnailBytes(
                path = path,
                width = widthParam,
                height = heightParam,
                centerCrop = centerCrop,
                mediaId = mediaId,
                fileName = fileName,
            )
            if (thumbBytes != null) {
                call.respond(thumbBytes)
            }
            return
        }

        // HEIF → PNG conversion for browsers without native HEIF support.
        val pngBytes = decodeImageFileToPng(path)
        if (pngBytes != null) {
            call.respond(pngBytes, contentType = "image/png")
            return
        }

        // Default: serve the file as-is with content type sniffed from extension.
        val contentType = getContentTypeForPath(path) ?: "application/octet-stream"
        call.respondFile(path, contentType = contentType)
    }
}
