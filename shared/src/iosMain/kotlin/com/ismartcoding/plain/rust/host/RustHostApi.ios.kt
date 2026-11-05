package com.ismartcoding.plain.rust.host

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObject

internal actual suspend fun executePlatformHostRequest(method: String, params: JsonObject, scope: CoroutineScope): HostResult =
    when (method) {
        "photoCatalogSnapshot" -> hostResult(com.ismartcoding.plain.platform.media.IosPhotoLibrary.mediaSnapshot())
        "photoCatalogVerify" -> {
            val request = com.ismartcoding.plain.lib.JsonHelper.jsonDecodeFromElement<com.ismartcoding.plain.platform.media.ImageCatalogRevisionRequest>(params)
            com.ismartcoding.plain.platform.media.IosPhotoLibrary.verifyMedia(request.revision)
            hostResult(true)
        }
        "photoCatalogPage" -> {
            val request = com.ismartcoding.plain.lib.JsonHelper.jsonDecodeFromElement<PhotoCatalogPageRequest>(params)
            hostResult(com.ismartcoding.plain.platform.media.IosPhotoLibrary.mediaPage(request.revision, request.offset, request.limit))
        }
        else -> error("Unknown Rust host operation: $method")
    }

@kotlinx.serialization.Serializable
private data class PhotoCatalogPageRequest(val revision: String, val offset: Int, val limit: Int)
