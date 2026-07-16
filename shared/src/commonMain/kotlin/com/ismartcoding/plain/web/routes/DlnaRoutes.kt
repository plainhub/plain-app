package com.ismartcoding.plain.web.routes

import com.ismartcoding.plain.features.dlna.sender.DlnaTransportController
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.isContentUri
import com.ismartcoding.plain.platform.streamContentUri
import com.ismartcoding.plain.web.http.HttpMethod
import com.ismartcoding.plain.web.http.HttpRouter
import com.ismartcoding.plain.web.http.HttpStatus

/**
 * `/media/{id}` and `NOTIFY /callback/cast` — DLNA endpoints shared between
 * Android (Ktor) and iOS (SwiftNIO future).
 *
 * `/media/{id}` looks up a previously-registered media path by short id
 * (see `UrlHelper.getMediaHttpUrl`) and serves it. URL sources are proxied,
 * `content://` URIs are streamed, images are served as-is, and all other
 * files are served with DLNA-specific headers + HTTP 206 so that TVs and
 * renderers accept the stream.
 *
 * `/callback/cast` receives the DLNA renderer's event NOTIFY XML and updates
 * `CastPlayer` state accordingly. When the renderer reports STOPPED (and the
 * callback has no AVTransportURIMetaData — which would indicate a duplicate
 * callback) the player auto-advances to the next playlist item.
 */
fun HttpRouter.addDlnaRoutes() {
    get("/media/{id}") { call ->
        val rawId = call.pathParam("id") ?: ""
        val id = rawId.split(".").firstOrNull() ?: ""
        if (id.isEmpty()) {
            call.respondNoBody(HttpStatus.BAD_REQUEST)
            return@get
        }
        try {
            val path = UrlHelper.getMediaPath(id)
            if (path.isEmpty()) {
                call.respondNoBody(HttpStatus.BAD_REQUEST)
                return@get
            }

            when {
                path.isUrl() -> {
                    if (!call.proxyUrl(path)) {
                        call.respondText(
                            "Failed to fetch data from URL: $path",
                            status = HttpStatus.INTERNAL_SERVER_ERROR,
                        )
                    }
                }

                isContentUri(path) -> {
                    // Stream the content URI bytes directly. Once the body has
                    // started the status code can no longer be changed, so any
                    // mid-stream failure is surfaced only via a truncated body.
                    call.respondStream { sink ->
                        streamContentUri(path, sink)
                    }
                }

                path.isImageFast() -> {
                    if (fileExists(path)) {
                        call.respondFile(path)
                    } else {
                        call.respondNoBody(HttpStatus.NOT_FOUND)
                    }
                }

                else -> {
                    if (!call.respondDlnaFile(path)) {
                        call.respondNoBody(HttpStatus.NOT_FOUND)
                    }
                }
            }
        } catch (ex: Exception) {
            call.respondText(
                "File is expired or does not exist. $ex",
                status = HttpStatus.FORBIDDEN,
            )
        }
    }

    method(HttpMethod("NOTIFY"), "/callback/cast") { call ->
        val xml = call.receiveText()
        LogCat.d(xml)

        // The TV may send the callback twice in quick succession. The second
        // one carries AVTransportURIMetaData and should be ignored when the
        // state is STOPPED — otherwise we'd skip a track on every stop event.
        if (xml.contains("TransportState val=\"STOPPED\"") &&
            !xml.contains("AVTransportURIMetaData")
        ) {
            withIO {
                CastPlayer.isPlaying.value = false
                val castItems = CastPlayer.items.value
                if (castItems.isNotEmpty()) {
                    CastPlayer.currentDevice?.let { device ->
                        val currentUri = CastPlayer.currentUri.value
                        var index = castItems.indexOfFirst { it.path == currentUri }
                        index++
                        if (index > castItems.size - 1) {
                            index = 0
                        }
                        val current = castItems[index]
                        if (current.path != currentUri) {
                            LogCat.d(current.path)
                            DlnaTransportController.setAVTransportURIAsync(
                                device,
                                UrlHelper.getMediaHttpUrl(current.path),
                                current.title,
                            )
                            CastPlayer.setCurrentUri(current.path)
                            CastPlayer.isPlaying.value = true
                        }
                    }
                }
            }
        } else if (xml.contains("TransportState val=\"PLAYING\"")) {
            withIO { CastPlayer.isPlaying.value = true }
        } else if (xml.contains("TransportState val=\"PAUSED_PLAYBACK\"")) {
            withIO { CastPlayer.isPlaying.value = false }
        }

        if (xml.contains("RelTime val=") && xml.contains("TrackDuration val=")) {
            withIO {
                try {
                    val relTimeMatch = Regex("RelTime val=\"([^\"]+)\"").find(xml)
                    val durationMatch = Regex("TrackDuration val=\"([^\"]+)\"").find(xml)
                    if (relTimeMatch != null && durationMatch != null) {
                        CastPlayer.updatePositionInfo(
                            relTimeMatch.groupValues[1],
                            durationMatch.groupValues[1],
                        )
                    }
                } catch (e: Exception) {
                    LogCat.e(e.toString())
                }
            }
        }

        call.respondNoBody(HttpStatus.OK)
    }
}
