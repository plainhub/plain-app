package com.ismartcoding.plain.features.imageindex

import com.ismartcoding.plain.api.*
import kotlinx.serialization.json.*

object ImageIndexHelper {
    private const val fields = "version isRunning totalImages indexedImages skippedImages errorMessage"
    suspend fun status(): JsonObject = RustContentApi.query("imageHostIndexStatus { $fields }").getValue("imageHostIndexStatus").jsonObject
    suspend fun start(force: Boolean): JsonObject = RustContentApi.mutate("imageHostStartIndex(force: $force) { $fields }").getValue("imageHostStartIndex").jsonObject
    suspend fun selected(ids: Collection<String>): JsonObject = RustContentApi.mutate("imageHostIndexSelected(ids: ${gqlIds(ids)}) { $fields }").getValue("imageHostIndexSelected").jsonObject
    suspend fun cancel(): JsonObject = RustContentApi.mutate("imageHostCancelIndex { $fields }").getValue("imageHostCancelIndex").jsonObject
}
