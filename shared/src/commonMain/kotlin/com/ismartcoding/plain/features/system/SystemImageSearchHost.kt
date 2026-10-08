package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.*
import kotlinx.serialization.json.*

internal object SystemImageSearchHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemImageModelAvailability" -> JsonHelper.jsonEncodeToElement(imageModelsAvailableOnPlatform())
        "systemImageTextEmbed" -> JsonHelper.jsonEncodeToElement(embedImageSearchText(JsonHelper.jsonDecodeFromElement<com.ismartcoding.plain.ai.ImageTextTokens>(params).tokenIds))
        "systemImageModelsLoad" -> {
            loadImageModels(JsonHelper.jsonDecodeFromElement<ImageModelsFiles>(params))
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemImageModelsClose" -> { closeImageModels(); JsonHelper.jsonEncodeToElement(true) }
        else -> error("Unsupported image model platform operation")
    }
}
