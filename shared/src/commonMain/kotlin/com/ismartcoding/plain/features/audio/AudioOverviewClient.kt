package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.models.AudioArtist
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal data class AudioHomeContent(val artists: List<AudioArtist>, val recentItems: List<Audio>)

class AudioOverviewApi internal constructor(private val api: GraphqlClient) {
    internal suspend fun read(recentIds: List<String>? = null): AudioHomeContent {
        val data = api.execute(Query.audioHomeContent(recentIds))
        val recent = when {
            recentIds == null -> checkNotNull(data.mediaPlayHistory).mapNotNull { item ->
                item.media.takeIf { it["__typename"]?.jsonPrimitive?.contentOrNull == "Audio" }
                    ?.let { JsonHelper.jsonDecodeFromElement<Audio>(it) }
            }
            recentIds.isEmpty() -> emptyList()
            else -> checkNotNull(data.audios)
        }
        return AudioHomeContent(data.audioArtists, recent)
    }
}
