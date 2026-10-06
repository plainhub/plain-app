package com.ismartcoding.plain.httpserver.routes

import com.ismartcoding.plain.lib.JsonHelper.jsonDecode
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.httpserver.FileIdParams
import com.ismartcoding.plain.httpserver.FileServer
import com.ismartcoding.plain.httpserver.http.HttpRouter
import com.ismartcoding.plain.httpserver.http.HttpStatus

/**
 * `/fs` — mobile file serving, reached through the bridge: the Rust
 * `peer_files` handler only fast-paths the desktop `fid:` payload and
 * forwards everything else here.
 *
 * `/fs` decrypts the `id` query parameter into either a filesystem path, a
 * `content://` URI, or a `pkgicon://` URI, then streams the appropriate bytes
 * back to the client. Supports thumbnail generation, HEIF→PNG conversion,
 * 3gp→MP4 transcoding, and download (`dl=1`) mode with Content-Disposition.
 *
 * With a `sid` query parameter `/fs` serves a shared file link instead: `id`
 * is a `{sharedId, virtualPath}` payload ChaCha20-encrypted with the share's
 * `url_token` and `sid` carries the public `shared_id`. The bytes stream
 * through [FileServer] exactly like the main-UI variant.
 *
 * `/proxyfs` (peer URL proxying) is served by the Rust public listener.
 */
fun HttpRouter.addFilesRoutes() {
    get("/fs") { call ->
        val id = call.queryParam("id") ?: ""
        if (id.isEmpty()) {
            call.respondNoBody(HttpStatus.BAD_REQUEST)
            return@get
        }
        val sid = call.queryParam("sid")
        try {
            if (sid != null) {
                // Shared-link mode: `id` is encrypted with the share's url_token.
                val realPath = ShareManager.resolveSharedPath(sid, id)
                if (realPath == null) {
                    call.respondNoBody(HttpStatus.FORBIDDEN)
                    return@get
                }
                FileServer.serve(call, realPath, jsonName = realPath.substringAfterLast('/'))
                return@get
            }
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

            FileServer.serve(call, path, mediaId = mediaId, jsonName = jsonName)
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respondText("File is expired or does not exist. $ex", status = HttpStatus.FORBIDDEN)
        }
    }
}
