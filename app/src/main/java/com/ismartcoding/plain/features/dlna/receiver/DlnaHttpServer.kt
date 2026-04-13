package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.features.dlna.DlnaCommand
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.features.dlna.PendingCastRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

/** Lightweight HTTP server that handles UPnP/DLNA AVTransport SOAP requests. */
object DlnaHttpServer {

    suspend fun run(port: Int) = withContext(Dispatchers.IO) {
        var serverSocket: ServerSocket? = null
        try {
            serverSocket = ServerSocket(port)
            serverSocket.reuseAddress = true
            LogCat.d("DLNA HTTP server started on port $port")
            while (isActive) {
                val client = try { serverSocket.accept() } catch (_: Exception) { break }
                launch { handleClient(client, client.inetAddress?.hostAddress.orEmpty()) }
            }
        } catch (e: Exception) {
            LogCat.e("DLNA HTTP server error: ${e.message}")
        } finally {
            serverSocket?.close()
        }
    }

    private suspend fun handleClient(socket: Socket, senderIp: String) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 5_000
            val bis = BufferedInputStream(socket.inputStream)
            val writer = PrintWriter(socket.outputStream, false, Charsets.UTF_8)
            val requestLine = bis.readHttpLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext
            val method = parts[0]
            val path = parts[1]

            val headers = mutableMapOf<String, String>()
            var headerLine = bis.readHttpLine()
            while (!headerLine.isNullOrEmpty()) {
                val idx = headerLine.indexOf(':')
                if (idx > 0) {
                    headers[headerLine.substring(0, idx).trim().lowercase()] =
                        headerLine.substring(idx + 1).trim()
                }
                headerLine = bis.readHttpLine()
            }
            val senderName = resolveSenderName(headers, senderIp)
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val body = readBodyBytes(bis, contentLength)

            val response = route(method, path, headers, body, senderIp, senderName)
            writer.print(response)
            writer.flush()
        } catch (e: Exception) {
            LogCat.e("DLNA client error: ${e.message}")
        } finally {
            socket.close()
        }
    }

    private suspend fun route(method: String, path: String, headers: Map<String, String>, body: String, senderIp: String = "", senderName: String = ""): String {
        return when {
            path.endsWith("description.xml") -> {
                val ip = NetworkHelper.getDeviceIP4()
                val port = DlnaRendererState.port.value
                val xml = DlnaXmlTemplates.deviceDescription(ip, port, DlnaRenderer.deviceUuid)
                httpOk(xml, "text/xml; charset=\"utf-8\"")
            }
            path.endsWith("scpd.xml") -> httpOk(DlnaXmlTemplates.scpdXml, "text/xml; charset=\"utf-8\"")
            method == "POST" && (path.endsWith("control") || path.contains("AVTransport")) ->
                handleSoap(headers, body, senderIp, senderName)
            method == "POST" && path.contains("RenderingControl") ->
                httpOk(DlnaSoapHandler.buildResponse("GetVolume", "<CurrentVolume>100</CurrentVolume>"), "text/xml; charset=\"utf-8\"")
            method == "SUBSCRIBE" -> httpOkSubscribe()
            method == "UNSUBSCRIBE" -> httpOk("")
            else -> httpNotFound()
        }
    }

    private suspend fun handleSoap(headers: Map<String, String>, body: String, senderIp: String = "", senderName: String = ""): String {
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
            "Pause" -> { DlnaRendererState.commandChannel.trySend(DlnaCommand.Pause); DlnaSoapHandler.buildResponse("Pause") }
            "Stop" -> { DlnaRendererState.commandChannel.trySend(DlnaCommand.Stop); DlnaSoapHandler.buildResponse("Stop") }
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
