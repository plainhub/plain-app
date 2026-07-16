package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.plain.features.dlna.DlnaCommand
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.features.dlna.PendingCastRequest
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.getDeviceIP4

/**
 * Pure-Kotlin HTTP request router for the DLNA receiver.
 *
 * Extracted from androidMain's `DlnaHttpServer.route()/handleSoap()` so all
 * routing / SOAP dispatch logic lives in commonMain. The platform layer is
 * only responsible for raw socket I/O (see [DlnaServerSocket] /
 * [DlnaClientConnection]).
 */
object DlnaHttpRouter {

    /**
     * Routes a parsed HTTP request and returns the full HTTP response string
     * (status line + headers + body) ready to be written to the socket.
     *
     * @param deviceUuid the renderer's UPnP UUID (used in device description)
     */
    suspend fun route(request: DlnaHttpRequest, deviceUuid: String, senderIp: String, senderName: String): String {
        val method = request.method
        val path = request.path
        val headers = request.headers
        val body = request.body
        return when {
            path.endsWith("description.xml") -> {
                val ip = getDeviceIP4()
                val port = DlnaRendererState.port.value
                val xml = DlnaXmlTemplates.deviceDescription(ip, port, deviceUuid)
                httpOk(xml, "text/xml; charset=\"utf-8\"")
            }
            path.endsWith("scpd.xml") -> httpOk(DlnaXmlTemplates.scpdXml, "text/xml; charset=\"utf-8\"")
            method == "POST" && (path.endsWith("control") || path.contains("AVTransport")) ->
                handleSoap(headers, body, senderIp, senderName)
            method == "POST" && path.contains("RenderingControl") ->
                httpOk(
                    DlnaSoapHandler.buildResponse("GetVolume", "<CurrentVolume>100</CurrentVolume>"),
                    "text/xml; charset=\"utf-8\"",
                )
            method == "SUBSCRIBE" -> httpOkSubscribe()
            method == "UNSUBSCRIBE" -> httpOk("")
            else -> httpNotFound()
        }
    }

    private suspend fun handleSoap(headers: Map<String, String>, body: String, senderIp: String, senderName: String): String {
        val soapAction = headers["soapaction"] ?: return httpInternalError()
        val (action, params) = DlnaSoapHandler.parseSoapAction(soapAction, body)
        LogCat.d("DLNA SOAP action: $action")
        val responseBody = when (action) {
            "SetAVTransportURI" -> {
                val uri = params["CurrentURI"] ?: ""
                val meta = params["CurrentURIMetaData"] ?: ""
                val rawTitle = DlnaSoapHandler.extractTitleFromDidlMeta(meta).ifEmpty {
                    uri.substringAfterLast('/').substringBefore('?')
                }
                val title = DlnaSoapHandler.cleanMediaTitle(rawTitle)
                val mediaType = DlnaSoapHandler.extractMediaTypeFromDidlMeta(meta, uri)
                val albumArtUri = DlnaSoapHandler.extractAlbumArtUriFromDidlMeta(meta)
                LogCat.d("DLNA SetAVTransportURI uri=$uri title=$title type=$mediaType")
                if (uri.isNotEmpty()) {
                    DlnaRendererState.rawPendingCastRequest.value =
                        PendingCastRequest(senderIp, senderName, uri, title, mediaType, albumArtUri)
                    DlnaRendererState.pendingPlayQueued.value = false
                }
                DlnaSoapHandler.buildResponse("SetAVTransportURI")
            }
            "Play" -> {
                LogCat.d("DLNA Play")
                val hasPending = DlnaRendererState.rawPendingCastRequest.value != null ||
                    DlnaRendererState.pendingCastRequest.value != null
                if (hasPending) {
                    DlnaRendererState.pendingPlayQueued.value = true
                } else {
                    DlnaRendererState.commandChannel.trySend(DlnaCommand.Play)
                }
                DlnaSoapHandler.buildResponse("Play")
            }
            "Pause" -> {
                DlnaRendererState.commandChannel.trySend(DlnaCommand.Pause)
                DlnaSoapHandler.buildResponse("Pause")
            }
            "Stop" -> {
                DlnaRendererState.commandChannel.trySend(DlnaCommand.Stop)
                DlnaSoapHandler.buildResponse("Stop")
            }
            "Seek" -> {
                val target = params["Target"] ?: ""
                val posMs = parseDlnaTimeToMs(target)
                if (posMs >= 0) DlnaRendererState.commandChannel.trySend(DlnaCommand.Seek(posMs))
                DlnaSoapHandler.buildResponse("Seek")
            }
            "GetTransportInfo" -> DlnaSoapHandler.buildTransportInfoResponse()
            "GetPositionInfo" -> DlnaSoapHandler.buildPositionInfoResponse()
            "GetMediaInfo" -> DlnaSoapHandler.buildMediaInfoResponse()
            "GetDeviceCapabilities" -> DlnaSoapHandler.buildResponse(
                "GetDeviceCapabilities",
                "<PlayMedia>NETWORK</PlayMedia><RecMedia>NOT_IMPLEMENTED</RecMedia><RecQualityModes>NOT_IMPLEMENTED</RecQualityModes>",
            )
            "SetPlayMode" -> DlnaSoapHandler.buildResponse("SetPlayMode")
            else -> DlnaSoapHandler.buildResponse(action)
        }
        return httpOk(responseBody, "text/xml; charset=\"utf-8\"")
    }
}
