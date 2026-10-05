package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.RustContentApi
import kotlinx.serialization.json.*

suspend fun getThumbnailResponse(path: String, width: Int, height: Int, centerCrop: Boolean, mediaId: String, fileName: String, ifNoneMatch: String? = null): PlainResponse =
    RustContentApi.postStream("files/thumbnail", buildJsonObject {
        put("path", path); put("width", width); put("height", height)
        put("centerCrop", centerCrop); put("mediaId", mediaId); put("fileName", fileName)
        put("ifNoneMatch", ifNoneMatch?.let(::JsonPrimitive) ?: JsonNull)
    })

suspend fun getThumbnailBytes(path: String, width: Int, height: Int, centerCrop: Boolean, mediaId: String, fileName: String): ByteArray? =
    getThumbnailResponse(path, width, height, centerCrop, mediaId, fileName).use {
        if (it.status.value == 204) null else {
            check(it.isSuccess()) { "Thumbnail generation failed: ${it.bodyAsText()}" }
            it.bodyAsBytes()
        }
    }
