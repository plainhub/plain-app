package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.serialization.json.*

internal object SystemImageSearchHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemImageSearchStatus" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.buildImageSearchStatus())
        "systemEnableImageSearch" -> {
            sendEvent(com.ismartcoding.plain.events.HEnableImageSearchEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemDisableImageSearch" -> {
            sendEvent(com.ismartcoding.plain.events.HDisableImageSearchEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemCancelImageModelDownload" -> {
            sendEvent(com.ismartcoding.plain.events.HCancelImageModelDownloadEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemStartImageIndex" -> {
            com.ismartcoding.plain.platform.startImageIndexFullScan(
                params.getValue("force").jsonPrimitive.boolean
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemCancelImageIndex" -> {
            com.ismartcoding.plain.platform.cancelImageIndex()
            JsonHelper.jsonEncodeToElement(true)
        }
        else -> error("Unsupported provider operation")
    }
}
