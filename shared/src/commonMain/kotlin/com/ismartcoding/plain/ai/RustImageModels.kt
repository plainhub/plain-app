package com.ismartcoding.plain.ai

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

internal object RustImageModels {
    private val projectionLock = com.ismartcoding.plain.platform.PlatformLock()
    private var version = -1L
    private val state = MutableStateFlow(ImageModelsSnapshot())
    val snapshot = state.asStateFlow()
    suspend fun call(command: ImageModelsCommand) {
        val response = RustContentApi.postJsonOrThrow("system/image-models", JsonHelper.jsonEncodeToElement<ImageModelsCommand>(command).jsonObject)
        apply(response.getValue("result").jsonObject)
    }
    suspend fun search(text: String, limit: Int): List<SemanticSearchResult> {
        val response = RustContentApi.postJsonOrThrow("system/image-models", JsonHelper.jsonEncodeToElement<ImageModelsCommand>(ImageModelsCommand.Search(text, limit)).jsonObject)
        return JsonHelper.jsonDecodeFromElement(response.getValue("items"))
    }
    suspend fun refresh() = call(ImageModelsCommand.Snapshot)
    fun apply(value: JsonObject) {
        val next = JsonHelper.jsonDecodeFromElement<ImageModelsSnapshot>(value)
        projectionLock.withLock {
            if (next.version < version) return@withLock
            version = next.version
            state.value = next
            sendEvent(HImageSearchStatusChangedEvent(next.status,next.downloadProgress,next.errorMessage))
            sendEvent(HImageIndexProgressEvent(next.totalImages,next.indexedImages,next.isIndexing))
        }
    }
}
