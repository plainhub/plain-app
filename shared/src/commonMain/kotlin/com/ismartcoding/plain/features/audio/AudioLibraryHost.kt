package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.api.string
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.PlatformLock
import kotlinx.serialization.json.*

object AudioLibraryHost {
    private val lock = PlatformLock()
    private var currentProvider: AudioLibraryProvider = PlatformAudioLibraryProvider
    fun install(provider: AudioLibraryProvider): AudioLibraryProvider = lock.withLock {
        val previous = currentProvider
        currentProvider = provider
        previous
    }
    suspend fun handle(method: String, params: JsonObject): JsonElement {
        val provider = lock.withLock { currentProvider }
        return when (method) {
            "audioMetadata" -> provider.metadata(params.string("path")).let { row ->
                buildJsonObject { put("path",row.path);put("title",row.title);put("artist",row.artist);put("albumId",row.albumId);put("durationMs",row.durationMs) }
            }
            "audioLibraryCount" -> JsonPrimitive(provider.count())
            "audioLibraryPath" -> provider.page(params.getValue("offset").jsonPrimitive.int, 1, sort(params)).firstOrNull()?.path?.let(::JsonPrimitive) ?: JsonNull
            "audioLibraryPage" -> JsonArray(provider.page(params.getValue("offset").jsonPrimitive.int, params.getValue("limit").jsonPrimitive.int, sort(params)).map { row ->
                buildJsonObject {
                    put("path", row.path); put("title", row.title); put("artist", row.artist)
                    put("albumId", row.albumId); put("durationMs", row.durationMs)
                }
            })
            "audioLibraryLocate" -> JsonPrimitive(locate(provider, params.string("path"), sort(params)))
            "audioLibraryContains" -> JsonPrimitive(provider.contains(params.string("path")))
            else -> error("Unknown Rust host operation: $method")
        }
    }

    private fun sort(params: JsonObject): FileSortBy = FileSortBy.valueOf(params.string("sortBy"))

    private suspend fun locate(provider: AudioLibraryProvider, path: String, sort: FileSortBy): Int {
        val total = provider.count()
        var offset = 0
        while (offset < total) {
            val rows = provider.page(offset, 500, sort)
            if (rows.isEmpty()) break
            val index = rows.indexOfFirst { it.path == path }
            if (index >= 0) return offset + index
            offset += rows.size
        }
        return -1
    }
}
