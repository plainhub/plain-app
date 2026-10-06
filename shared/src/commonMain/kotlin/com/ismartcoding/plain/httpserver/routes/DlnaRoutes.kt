package com.ismartcoding.plain.httpserver.routes

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.features.dlna.sender.DlnaTransportController
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.isContentUri
import com.ismartcoding.plain.platform.streamContentUri
import com.ismartcoding.plain.httpserver.http.HttpMethod
import com.ismartcoding.plain.httpserver.http.HttpRouter
import com.ismartcoding.plain.httpserver.http.HttpStatus

/**
 * DLNA sender endpoints (`/media/{id}`, `NOTIFY /callback/cast`).
 *
 * The MediaRenderer receiver side — description.xml, the scpd documents, SOAP
 * control and the GENA event paths — is served by the Rust listener.
 */
fun HttpRouter.addDlnaRoutes() = addDlnaSenderRoutes()

/**
 * `/media/{id}` and `NOTIFY /callback/cast` — DLNA sender endpoints.
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
 * callback) the player auto-advances via [advanceCastToNextTrack]: next cast
 * queue item, or — when the cast queue is empty — the next track of the audio
 * playback order.
 *
 * These are sender (casting) routes and have no separate feature toggle —
 * they are available whenever the service is running, independently of the
 * desktop-access gate and the DLNA receiver toggle. The platform HTTP
 * intercepts bypass `desktopAccessEnabled` for them via
 * [com.ismartcoding.plain.httpserver.isDlnaSenderPath]; the HTTP server itself only
 * runs while `serviceEnabled=true`.
 */
private fun HttpRouter.addDlnaSenderRoutes() {
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
                advanceCastToNextTrack()
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

/**
 * End-of-track auto-advance for the active cast session, run when the renderer
 * reports a natural STOP:
 *
 *  - the explicit cast queue (row toggles) when it can produce a different
 *    track — it wraps around, so a multi-track queue loops;
 *  - otherwise, for audio casts only, the audio playback order
 *    ([AudioQueueManager.resolveNext] — the same resolution local playback
 *    runs on STATE_ENDED → skipToNext), continuing after the queue's current
 *    track, which [com.ismartcoding.plain.ui.models.CastViewModel.castItem]
 *    registers via onPlaying.
 *
 * When neither source can advance (queue exhausted on its single track, no
 * playback order, or a non-audio cast) the cast just stops.
 */
private suspend fun advanceCastToNextTrack() {
    val device = CastPlayer.currentDevice ?: return
    val currentUri = CastPlayer.currentUri.value
    val castItems = CastPlayer.items.value
    var nextPath = ""
    var nextTitle = ""
    if (castItems.isNotEmpty()) {
        var index = castItems.indexOfFirst { it.path == currentUri }
        index++
        if (index > castItems.size - 1) {
            index = 0
        }
        val next = castItems[index]
        nextPath = next.path
        nextTitle = next.title
    } else if (currentUri.isAudioFast()) {
        val next = AudioQueueManager.resolveNext(
            isNext = true,
            shuffle = UserPrefs.audioPlayMode.value == MediaPlayMode.SHUFFLE,
        )
        nextPath = next?.path ?: ""
        nextTitle = next?.title ?: ""
    }
    if (nextPath.isEmpty() || nextPath == currentUri) return
    LogCat.d(nextPath)
    DlnaTransportController.setAVTransportURIAsync(device, UrlHelper.getMediaHttpUrl(nextPath), nextTitle)
    CastPlayer.setCurrentUri(nextPath)
    CastPlayer.isPlaying.value = true
}
