package com.ismartcoding.plain.web.routes

import com.ismartcoding.plain.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.lib.extensions.getFinalPath
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.urlEncode
import com.ismartcoding.plain.platform.convert3gpToMp4
import com.ismartcoding.plain.platform.decodeImageFileToPng
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.getContentTypeForPath
import com.ismartcoding.plain.platform.getPackageIconBytes
import com.ismartcoding.plain.platform.getThumbnailBytes
import com.ismartcoding.plain.platform.isAnimatedImageOrSvg
import com.ismartcoding.plain.platform.isContentUri
import com.ismartcoding.plain.platform.statFile
import com.ismartcoding.plain.platform.streamContentUri
import com.ismartcoding.plain.web.FileIdParams
import com.ismartcoding.plain.web.http.HttpCall
import com.ismartcoding.plain.web.http.HttpRouter
import com.ismartcoding.plain.web.http.HttpStatus

/**
 * `/fs` and `/proxyfs` — file serving endpoints shared between Android (Ktor)
 * and iOS (SwiftNIO future).
 *
 * `/fs` decrypts the `id` query parameter into either a filesystem path, a
 * `content://` URI, or a `pkgicon://` URI, then streams the appropriate bytes
 * back to the client. Supports thumbnail generation, HEIF→PNG conversion,
 * 3gp→MP4 transcoding, and download (`dl=1`) mode with Content-Disposition.
 *
 * `/proxyfs` decrypts the `id` into a peer HTTP URL and proxies the upstream
 * response (status, headers, body) through to the client — used for
 * peer-to-peer file downloads over Wi-Fi Aware.
 */
fun HttpRouter.addFilesRoutes() {
    get("/fs") { call ->
        val id = call.queryParam("id") ?: ""
        if (id.isEmpty()) {
            call.respondNoBody(HttpStatus.BAD_REQUEST)
            return@get
        }
        try {
            val decryptedId = UrlHelper.decrypt(id).getFinalPath()
            val path: String
            val mediaId: String
            val jsonName: String
            if (decryptedId.startsWith("{")) {
                val params = jsonDecode<FileIdParams>(decryptedId)
                path = params.path.getFinalPath()
                mediaId = params.mediaId
                jsonName = params.name
            } else {
                path = decryptedId
                mediaId = ""
                jsonName = ""
            }

            val isDownload = call.queryParam("dl") == "1"
            val widthParam = call.queryParam("w")?.toIntOrNull()
            val heightParam = call.queryParam("h")?.toIntOrNull()
            // `cc` defaults to true (matches the original `!= false` behavior).
            val centerCrop = call.queryParam("cc")?.toBooleanStrictOrNull() != false

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
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respondText("File is expired or does not exist. $ex", status = HttpStatus.FORBIDDEN)
        }
    }

    get("/proxyfs") { call ->
        val id = call.queryParam("id") ?: ""
        if (id.isEmpty()) {
            call.respondNoBody(HttpStatus.BAD_REQUEST)
            return@get
        }
        try {
            val peerUrl = UrlHelper.decrypt(id)
            if (peerUrl.isEmpty() || !peerUrl.startsWith("http")) {
                call.respondText("Invalid peer URL", status = HttpStatus.BAD_REQUEST)
                return@get
            }
            if (!call.proxyUrl(peerUrl)) {
                call.respondNoBody(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respondText(ex.message ?: "", status = HttpStatus.INTERNAL_SERVER_ERROR)
        }
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
        val contentType = getContentTypeForPath(path)
        call.respondFile(path, contentType = contentType, contentDisposition = contentDisposition)
        return
    }
    call.responseHeader("Content-Disposition", "inline; filename=\"${fileName}\"; filename*=utf-8''${fileName}")

    // Animated images (GIF, animated WebP, animated HEIF) and SVG: serve as-is
    // so the browser can render them with native range support.
    if (fileName.isImageFast() && isAnimatedImageOrSvg(path, fileName)) {
        val contentType = getContentTypeForPath(path)
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
    val contentType = getContentTypeForPath(path)
    call.respondFile(path, contentType = contentType)
}
