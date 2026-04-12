package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.features.dlna.DlnaCommand
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
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
                launch { handleClient(client) }
            }
        } catch (e: Exception) {
            LogCat.e("DLNA HTTP server error: ${e.message}")
        } finally {
            serverSocket?.close()
        }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 5_000
            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
            val writer = PrintWriter(socket.outputStream, false, Charsets.UTF_8)
            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext
            val method = parts[0]
            val path = parts[1]

            val headers = mutableMapOf<String, String>()
            var headerLine = reader.readLine()
            while (!headerLine.isNullOrEmpty()) {
                val idx = headerLine.indexOf(':')
                if (idx > 0) {
                    headers[headerLine.substring(0, idx).trim().lowercase()] =
                        headerLine.substring(idx + 1).trim()
                }
                headerLine = reader.readLine()
            }
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val body = if (contentLength > 0) {
                val buf = CharArray(contentLength)
                var offset = 0
                while (offset < contentLength) {
                    val read = reader.read(buf, offset, contentLength - offset)
                    if (read == -1) break
                    offset += read
                }
                String(buf, 0, offset)
            } else ""

            val response = route(method, path, headers, body)
            writer.print(response)
            writer.flush()
        } catch (e: Exception) {
            LogCat.e("DLNA client error: ${e.message}")
        } finally {
            socket.close()
        }
    }

    private suspend fun route(method: String, path: String, headers: Map<String, String>, body: String): String {
        return when {
            path.endsWith("description.xml") -> {
                val ip = NetworkHelper.getDeviceIP4()
                val port = DlnaRendererState.port.value
                val xml = DlnaXmlTemplates.deviceDescription(ip, port, DlnaRenderer.deviceUuid)
                httpOk(xml, "text/xml; charset=\"utf-8\"")
            }
            path.endsWith("scpd.xml") -> httpOk(DlnaXmlTemplates.scpdXml, "text/xml; charset=\"utf-8\"")
            method == "POST" && (path.endsWith("control") || path.contains("AVTransport")) ->
                handleSoap(headers, body)
            method == "POST" && path.contains("RenderingControl") ->
                httpOk(DlnaSoapHandler.buildResponse("GetVolume", "<CurrentVolume>100</CurrentVolume>"), "text/xml; charset=\"utf-8\"")
            method == "SUBSCRIBE" -> httpOkSubscribe()
            method == "UNSUBSCRIBE" -> httpOk("")
            else -> httpNotFound()
        }
    }

    private suspend fun handleSoap(headers: Map<String, String>, body: String): String {
        val soapAction = headers["soapaction"] ?: return httpInternalError()
        val (action, params) = DlnaSoapHandler.parseSoapAction(soapAction, body)
        LogCat.d("DLNA SOAP action: $action")
        val responseBody = when (action) {
            "SetAVTransportURI" -> {
                val uri = params["CurrentURI"] ?: ""
                val meta = params["CurrentURIMetaData"] ?: ""
                val title = DlnaSoapHandler.extractTitleFromDidlMeta(meta).ifEmpty {
                    uri.substringAfterLast('/').substringBefore('?')
                }
                LogCat.d("DLNA SetAVTransportURI uri=$uri title=$title")
                if (uri.isNotEmpty()) {
                    DlnaRendererState.commandChannel.trySend(DlnaCommand.SetUri(uri, title))
                }
                DlnaSoapHandler.buildResponse("SetAVTransportURI")
            }
            "Play" -> {
                LogCat.d("DLNA Play")
                DlnaRendererState.commandChannel.trySend(DlnaCommand.Play)
                DlnaSoapHandler.buildResponse("Play")
            }
            "Pause" -> { DlnaRendererState.commandChannel.trySend(DlnaCommand.Pause); DlnaSoapHandler.buildResponse("Pause") }
            "Stop" -> { DlnaRendererState.commandChannel.trySend(DlnaCommand.Stop); DlnaSoapHandler.buildResponse("Stop") }
            "Seek" -> {
                val target = params["Target"] ?: ""
                val posMs = parseTimeToMs(target)
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

    private fun parseTimeToMs(time: String): Long {
        val parts = time.split(":")
        return if (parts.size >= 3) {
            val h = parts[0].toLongOrNull() ?: return -1L
            val m = parts[1].toLongOrNull() ?: return -1L
            val s = parts[2].split(".")[0].toLongOrNull() ?: return -1L
            (h * 3600 + m * 60 + s) * 1000
        } else -1L
    }

    private fun httpOk(body: String, contentType: String = "text/plain"): String {
        val bytes = body.toByteArray(Charsets.UTF_8)
        return "HTTP/1.1 200 OK\r\nContent-Type: $contentType\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n$body"
    }

    private fun httpOkSubscribe(): String =
        "HTTP/1.1 200 OK\r\nSID: uuid:dlna-plain-sub\r\nTIMEOUT: Second-3600\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"

    private fun httpNotFound(): String = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"

    private fun httpInternalError(): String = "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
}
