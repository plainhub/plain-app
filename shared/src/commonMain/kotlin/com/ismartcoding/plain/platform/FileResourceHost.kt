package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object FileResourceHost {
    suspend fun handle(params: JsonObject): JsonElement {
        val request = JsonHelper.jsonDecodeFromElement<FileResourceRequest>(params)
        val path = request.path
        val extra = request.params
        val operation = request.operation
        return when (operation) {
            "mime" -> JsonHelper.jsonEncodeToElement(getContentTypeForPath(path))
            "appDir" -> JsonPrimitive(appDir())
            "zipExtract" -> extractZipEntryToCache(path)?.let(::JsonPrimitive) ?: JsonNull
            "probe" -> JsonPrimitive(probeVideoCodec(path))
            "animated" -> JsonPrimitive(isAnimatedImageOrSvg(path, extra.fileName))
            "transcode" -> transcodeMp4ForBrowser(path)?.let(::JsonPrimitive) ?: JsonNull
            "remux" -> remuxMp4ForBrowser(path)?.let(::JsonPrimitive) ?: JsonNull
            "convert3gp", "packageIcon", "decodePng" -> {
                val bytes = when (operation) {
                    "convert3gp" -> convert3gpToMp4(path)
                    "packageIcon" -> getPackageIconBytes(path)
                    else -> decodeImageFileToPng(path)
                }
                if (bytes == null) JsonNull else {
                    val output = extra.output
                    ensureParentDir(output)
                    check(writeBytesToPath(output, bytes)) { "Unable to write native resource output" }
                    JsonPrimitive(true)
                }
            }
            else -> error("Unsupported native resource operation")
        }
    }

    suspend fun stream(socket: PlainWebSocketSession, path: String) {
        val sink = ResourceStreamSink(socket)
        try {
            check(streamContentUri(path, sink) != null) { "Unable to open content URI" }
            socket.sendText(JsonHelper.jsonEncode(ResourceStreamEnd()))
        } finally { sink.close() }
    }
}
