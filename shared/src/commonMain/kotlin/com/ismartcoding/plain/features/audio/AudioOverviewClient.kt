package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.AudioSortBy
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.models.AudioArtist
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal data class AudioHomeContent(val artists: List<AudioArtist>, val recentItems: List<Audio>)

class AudioOverviewApi internal constructor(private val api: GraphqlClient) {
    internal suspend fun read(): AudioHomeContent {
        val data = api.execute(Query.audioHomeContent())
        val recent = data.mediaPlayHistory.mapNotNull { item ->
            item.media.takeIf { it["__typename"]?.jsonPrimitive?.contentOrNull == "Audio" }
                ?.let { JsonHelper.jsonDecodeFromElement<Audio>(it) }
        }.ifEmpty {
            api.execute(Query.mediaRows(DataType.AUDIO, "", 8, 0, AudioSortBy.UPDATED_AT_DESC)).filterIsInstance<Audio>()
        }
        return AudioHomeContent(data.audioArtists, recent)
    }
}
