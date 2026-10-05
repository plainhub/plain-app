package com.ismartcoding.plain.thumbnail

import kotlinx.serialization.json.*
import com.ismartcoding.plain.api.ContentApiSession
import com.ismartcoding.plain.platform.*

object ThumbnailHost {
    private val client by lazy { createPlainHttpClient(PlainHttpClientSpec.HttpHost) }
    suspend fun handle(session: ContentApiSession, method: String, params: JsonObject): JsonElement {
        if (method == "thumbnailAuthorize") {
            check(readFileRange(params.getValue("paths").jsonArray.single().jsonPrimitive.content, 0, 1) != null) { "Thumbnail source access denied" }
            return JsonPrimitive(cacheDirPath())
        }
        check(method == "thumbnailDecode")
        val path = params.getValue("path").jsonPrimitive.content
        check(readFileRange(path, 0, 1) != null) { "Thumbnail source access denied" }
        val bytes = decodeThumbnailBytes(path,
            params.getValue("width").jsonPrimitive.int,
            params.getValue("height").jsonPrimitive.int,
            params.getValue("centerCrop").jsonPrimitive.boolean,
            params.getValue("mediaId").jsonPrimitive.content,
            params.getValue("fileName").jsonPrimitive.content,
        ) ?: return JsonNull
        val token = params.getValue("outputToken").jsonPrimitive.content
        client.request(PlainRequest("POST", "${session.baseUrl}/files/thumbnail/output/$token", session.headers(), bytes, "application/octet-stream")).use {
            check(it.status.value == 204) { "Thumbnail output refused: ${it.status}" }
        }
        return JsonPrimitive(true)
    }
}
