package com.ismartcoding.plain.tests

import kotlinx.serialization.json.JsonObject
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlApi

import com.ismartcoding.plain.models.AudioPlayHistory
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.jsonArray

internal object TestAudioHistoryClient {
    suspend fun recentPageFiltered(text: String, limit: Int, offset: Int): List<AudioPlayHistory> =
        GraphqlApi.execute(GraphqlRequest<JsonObject>("query { audioPlayHistory(offset: $offset, limit: $limit, query: ${JsonPrimitive(text)}) { mediaId path title artist durationMs playCount playedAt } }"))
            .getValue("audioPlayHistory").jsonArray.map { JsonHelper.jsonDecodeFromElement<AudioPlayHistory>(it) }
}
