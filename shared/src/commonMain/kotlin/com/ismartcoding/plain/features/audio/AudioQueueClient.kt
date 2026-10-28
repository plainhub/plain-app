package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.models.AudioPlayback
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.selectionQuery

object AudioQueueClient {
    suspend fun playback(): AudioPlayback =
        GraphqlApi.execute(Query.audioPlayback())

    suspend fun resolveNext(isNext: Boolean, shuffle: Boolean = false): AudioItem? {
        val current = playback()
        val action = if (isNext) AudioSelectionAction.NEXT else AudioSelectionAction.PREVIOUS
        val result = GraphqlApi.execute(
            Mutation.advanceAudioSelection(action, current.path.orEmpty(), current.revision, shuffle),
        )
        return result?.track
    }

    suspend fun clearQueue() {
        GraphqlApi.execute(Mutation.clearAudioQueue())
    }

    suspend fun enqueue(items: List<AudioItem>) {
        val ids = items.mapNotNull { it.mediaId }.filter { it.isNotEmpty() }
        if (ids.isNotEmpty()) add(selectionQuery(ids.toSet()))
    }

    suspend fun add(query: String) {
        GraphqlApi.execute(Mutation.addAudioToQueue(query))
    }

    suspend fun removeQueued(mediaId: String) {
        GraphqlApi.execute(Mutation.removeAudioFromQueue(mediaId))
    }

    suspend fun reorderQueued(mediaIds: List<String>) {
        GraphqlApi.execute(Mutation.reorderAudioQueue(mediaIds))
    }

    suspend fun queuedMediaIds(): Set<String> =
        GraphqlApi.execute(Query.audioQueueIds(Int.MAX_VALUE))
            .mapNotNull { it.mediaId }
            .toSet()

    suspend fun queueTotal(): Int =
        GraphqlApi.execute(Query.audioQueueItemCount())

    suspend fun queuePage(offset: Int, limit: Int): List<AudioItem> =
        queuePageFiltered("", offset, limit)

    suspend fun queuePageFiltered(query: String, offset: Int, limit: Int): List<AudioItem> =
        GraphqlApi.execute(Query.audioQueueItems(offset, limit, query))
}
