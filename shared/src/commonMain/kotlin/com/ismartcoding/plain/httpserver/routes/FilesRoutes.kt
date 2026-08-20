package com.ismartcoding.plain.httpserver.routes

import com.ismartcoding.plain.lib.JsonHelper.jsonDecode
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.httpserver.FileIdParams
import com.ismartcoding.plain.httpserver.ShareFileParams
import com.ismartcoding.plain.httpserver.FileServer
import com.ismartcoding.plain.httpserver.http.HttpRouter
import com.ismartcoding.plain.httpserver.http.HttpStatus
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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

            // Byte streaming (zip extraction, byte-range, thumbnails, HEIF→PNG,
            // download mode) is shared with `/sfs` via FileServer.
            FileServer.serve(call, path, mediaId = mediaId, jsonName = jsonName)
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respondText("File is expired or does not exist. $ex", status = HttpStatus.FORBIDDEN)
        }
    }

    /**
     * `/sfs` — file serving behind a shared link. Reuses [FileServer] (the exact
     * same byte stream as `/fs`) but authenticates differently:
     * - `sid` is the public `shared_id` (used as the lookup + whitelist key).
     * - `id` is a [ShareFileParams] payload ChaCha20-encrypted with the share's
     *   dedicated `url_token`. Decryption yields `{sharedId, virtualPath}`,
     *   which is then whitelisted against the share's roots via
     *   [ShareManager.resolveVirtualPath].
     */
    get("/sfs") { call ->
        if (!TempData.serviceEnabled.value) {
            call.respondNoBody(HttpStatus.FORBIDDEN)
            return@get
        }
        val sid = call.queryParam("sid") ?: ""
        val id = call.queryParam("id") ?: ""
        if (sid.isEmpty() || id.isEmpty()) {
            call.respondNoBody(HttpStatus.BAD_REQUEST)
            return@get
        }
        val share = withIO { AppDatabase.instance.shareDao().getById(sid) }
        if (share == null || !share.isActive) {
            call.respondNoBody(HttpStatus.FORBIDDEN)
            return@get
        }
        try {
            @OptIn(ExperimentalEncodingApi::class)
            val key = Base64.decode(share.urlToken)
            val decrypted = UrlHelper.decrypt(id, key)
            val params = jsonDecode<ShareFileParams>(decrypted)
            // The decrypted payload must reference the same share id as `sid`.
            if (params.sharedId != sid) {
                call.respondNoBody(HttpStatus.FORBIDDEN)
                return@get
            }
            val realPath = ShareManager.resolveVirtualPath(share, params.virtualPath)
            if (realPath == null) {
                call.respondNoBody(HttpStatus.FORBIDDEN)
                return@get
            }
            FileServer.serve(call, realPath, jsonName = params.virtualPath.substringAfterLast('/'))
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respondNoBody(HttpStatus.BAD_REQUEST)
        }
    }

    get("/proxyfs") { call ->
        // `/proxyfs` proxies peer HTTP URLs for the Main Web UI (used when
        // the browser displays files hosted on a peer device). It is a
        // Main-UI route — peer file downloads over BLE/Aware use `/fs`
        // directly, not `/proxyfs`.
        if (!TempData.canDesktopAccess()) {
            call.respondNoBody(HttpStatus.FORBIDDEN)
            return@get
        }
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
