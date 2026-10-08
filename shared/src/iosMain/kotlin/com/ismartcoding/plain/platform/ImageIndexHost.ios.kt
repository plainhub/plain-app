package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.string
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

actual suspend fun handleImageIndexHost(method: String, params: JsonObject): JsonElement = when (method) {
    "imageIndexBegin" -> IosImageIndexCatalog.snapshot()
    "imageIndexPage" -> IosImageIndexCatalog.page(params.string("revision"), params.string("cursor"), params.getValue("limit").jsonPrimitive.int)
    "imageIndexResolve" -> IosImageIndexCatalog.resolve(params.string("revision"), params.getValue("ids").jsonArray.map { it.jsonPrimitive.content })
    "imageIndexDecode" -> JsonHelper.jsonEncodeToElement(IosImageIndexCatalog.decode(params.string("id")))
    "imageIndexRelease" -> JsonHelper.jsonEncodeToElement(IosImageIndexCatalog.release(params.string("path")))
    "imageIndexVerify" -> { IosImageIndexCatalog.verify(params.string("revision")); JsonPrimitive(true) }
    "imageIndexEnd", "imageIndexProgress" -> JsonPrimitive(true)
    else -> error("Unknown image index host operation: $method")
}
actual suspend fun disconnectImageIndexHost() = IosImageIndexCatalog.observe(false)
