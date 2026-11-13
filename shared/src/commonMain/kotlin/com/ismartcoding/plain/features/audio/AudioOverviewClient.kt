package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.graphql.Fragments
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.models.AudioArtist
import com.ismartcoding.plain.models.MediaScanState
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.changes
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal data class AudioHomeContent(val artists: List<AudioArtist>, val recentItems: List<Audio>)

@Serializable
private data class AudioHomeQuery(val audioArtists: List<AudioArtist>, val mediaPlayHistory: List<AudioHistoryItem>)

@Serializable
private data class AudioHistoryItem(val media: JsonObject)

object AudioOverviewClient : AudioOverviewApi()

open class AudioOverviewApi internal constructor(protected val api: GraphqlClient = GraphqlApi) {
    internal suspend fun read(): AudioHomeContent {
        val data = api.execute(GraphqlRequest<AudioHomeQuery>(
            $$"""query {
                audioArtists { name itemCount playCount samplePath }
                mediaPlayHistory(query: "type:AUDIO", offset: 0, limit: 8) {
                    media { __typename ... on Audio { $${Fragments.mediaFields(DataType.AUDIO)} } }
                }
            }""",
        ))
        val recent = data.mediaPlayHistory.mapNotNull { item ->
            item.media.takeIf { it["__typename"]?.jsonPrimitive?.contentOrNull == "Audio" }
                ?.let { JsonHelper.jsonDecodeFromElement<Audio>(it) }
        }.ifEmpty {
            api.execute(Query.mediaRows(DataType.AUDIO, "", 8, 0, FileSortBy.DATE_DESC)).filterIsInstance<Audio>()
        }
        return AudioHomeContent(data.audioArtists, recent)
    }

    val updates get() = merge(
        AppSubscriptions.contentChanged.changes(),
        AppSubscriptions.mediaUpdated.changes { DataType.AUDIO in it.types },
        AppSubscriptions.mediaScanProgress.changes {
            when (it.state) {
                MediaScanState.IDLE, MediaScanState.FAILED, MediaScanState.STOPPED, MediaScanState.PAUSED -> true
                MediaScanState.RUNNING -> false
            }
        },
    )
}
