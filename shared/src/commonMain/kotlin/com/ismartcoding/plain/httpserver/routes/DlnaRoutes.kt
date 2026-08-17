package com.ismartcoding.plain.httpserver.routes

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.features.dlna.receiver.DlnaHttpRouter
import com.ismartcoding.plain.features.dlna.receiver.DlnaReceiverEngine
import com.ismartcoding.plain.features.dlna.sender.DlnaTransportController
import com.ismartcoding.plain.lib.dlna.common.DlnaHttpRequest
import com.ismartcoding.plain.lib.dlna.common.DlnaHttpResponse
import com.ismartcoding.plain.lib.dlna.common.resolveSenderName
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.isContentUri
import com.ismartcoding.plain.platform.streamContentUri
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.httpserver.http.HttpMethod
import com.ismartcoding.plain.httpserver.http.HttpRouter
import com.ismartcoding.plain.httpserver.http.HttpStatus

/**
 * DLNA endpoints served by the shared web server.
 *
 * Sender routes (`/media/{id}`, `NOTIFY /callback/cast`) and receiver routes
 * (`/description.xml`, `/AVTransport/...`, `/RenderingControl/...`) are registered
 * here so they share the web server port. The receiver routes are gated by
 * `TempData.dlnaReceiverEnabled` — when the DLNA receiver toggle is off they
 * return 404.
 */
fun HttpRouter.addDlnaRoutes() {
    addDlnaSenderRoutes()
    addDlnaReceiverRoutes()
}

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
 * callback) the player auto-advances to the next playlist item.
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
                            val url = UrlHelper.getMediaHttpUrl(current.path)
                            DlnaTransportController.setAVTransportURIAsync(
                                device,
                                url,
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

/**
 * DLNA MediaRenderer receiver routes, served by the shared web server so the
 * receiver shares the web server port. All routing / SOAP dispatch lives in
 * [DlnaHttpRouter]; these handlers adapt the platform-agnostic [HttpCall] to
 * the [DlnaHttpRequest]/[DlnaHttpResponse] types the router expects.
 *
 * Gated by `TempData.dlnaReceiverEnabled` — when the DLNA receiver toggle is
 * off every receiver route returns 404.
 */
private fun HttpRouter.addDlnaReceiverRoutes() {
    listOf(
        "/description.xml",
        "/AVTransport/scpd.xml",
        "/RenderingControl/scpd.xml",
    ).forEach { get(it) { call -> handleDlnaReceiver(call) } }

    listOf(
        "/AVTransport/control",
        "/RenderingControl/control",
    ).forEach { post(it) { call -> handleDlnaReceiver(call) } }

    val eventPaths = listOf("/AVTransport/event", "/RenderingControl/event")
    listOf(HttpMethod("SUBSCRIBE"), HttpMethod("UNSUBSCRIBE")).forEach { m ->
        eventPaths.forEach { path -> method(m, path) { call -> handleDlnaReceiver(call) } }
    }
}

private suspend fun handleDlnaReceiver(call: HttpCall) {
    if (!TempData.canDLNAAccess()) {
        call.respondNoBody(HttpStatus.NOT_FOUND)
        return
    }
    val senderIp = call.remoteHost
    val headers = buildMap<String, String> {
        call.header("soapaction")?.let { put("soapaction", it) }
        call.header("c-name")?.let { put("c-name", it) }
    }
    val body = if (call.method.name == "POST") call.receiveText() else ""
    val request = DlnaHttpRequest(
        method = call.method.name,
        path = call.path,
        headers = headers,
        body = body,
    )
    val senderName = resolveSenderName(headers, senderIp)
    val response = DlnaHttpRouter.route(request, DlnaReceiverEngine.deviceUuid, senderIp, senderName)
    applyDlnaResponse(call, response)
}

private suspend fun applyDlnaResponse(call: HttpCall, response: DlnaHttpResponse) {
    response.headers.forEach { (name, value) -> call.responseHeader(name, value) }
    if (response.body.isNotEmpty()) {
        call.respondText(
            body = response.body,
            contentType = response.contentType,
            status = response.status,
        )
    } else {
        call.respondNoBody(response.status)
    }
}
