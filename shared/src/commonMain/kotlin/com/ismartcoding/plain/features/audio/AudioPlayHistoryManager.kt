package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.db.DAudioPlayHistory
import kotlinx.serialization.json.*

object AudioPlayHistoryManager {
    suspend fun recordHistory(path: String, title: String, artist: String, durationMs: Long) {
        RustContentApi.mutate("audioHostRecordHistory(path: ${gql(path)}, title: ${gql(title)}, artist: ${gql(artist)}, durationMs: $durationMs)")
    }
    suspend fun artistPlayCounts(): Map<String, Long> = RustContentApi.query("audioArtistPlayCounts { artist count }")
        .getValue("audioArtistPlayCounts").jsonArray.associate { r -> r.jsonObject.string("artist") to r.jsonObject.getValue("count").jsonPrimitive.long }
    suspend fun recentPage(limit: Int, offset: Int): List<DAudioPlayHistory> = recentPageFiltered("",limit,offset)
    suspend fun recentPageFiltered(text: String, limit: Int, offset: Int): List<DAudioPlayHistory> =
        RustContentApi.query("audioHostHistory(offset: $offset, limit: $limit, query: ${gql(text)}) { $AUDIO_HISTORY_FIELDS }")
            .getValue("audioHostHistory").jsonArray.map { it.audioHistory() }
}
