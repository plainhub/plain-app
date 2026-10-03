package com.ismartcoding.plain.platform

import com.ismartcoding.plain.ai.ImageIndexCatalog
import com.ismartcoding.plain.ai.ImageIndexInference
import com.ismartcoding.plain.ai.ImageSearchIndexer
import com.ismartcoding.plain.api.string
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

actual suspend fun handleImageIndexHost(method: String, params: JsonObject): JsonElement = withContext(Dispatchers.IO) {
    when (method) {
        "imageIndexBegin" -> ImageIndexInference.begin(params.string("jobId"))
        "imageIndexPage" -> ImageIndexCatalog.page(params.string("revision"),params.string("cursor"),params.getValue("limit").jsonPrimitive.int)
        "imageIndexResolve" -> ImageIndexCatalog.resolve(params.string("revision"),params.getValue("ids").jsonArray.map { it.jsonPrimitive.content })
        "imageIndexEmbed" -> ImageIndexInference.embed(params.string("jobId"),params.string("revision"),params.getValue("items").jsonArray)
        "imageIndexVerify" -> { ImageIndexCatalog.verify(params.string("revision"));JsonPrimitive(true) }
        "imageIndexEnd" -> JsonPrimitive(ImageIndexInference.end(params.string("jobId")))
        "imageIndexProgress" -> { withContext(Dispatchers.Main) { ImageSearchIndexer.applyStatus(params) };JsonPrimitive(true) }
        else -> error("Unknown image index host operation: $method")
    }
}
actual suspend fun disconnectImageIndexHost() = withContext(NonCancellable + Dispatchers.IO) { ImageIndexInference.disconnect() }
