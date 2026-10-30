package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.models.AudioPlayback
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.selectionQuery

object AudioQueueClient : AudioQueueApi()

open class AudioQueueApi internal constructor(private val api: GraphqlClient = GraphqlApi) {
    internal suspend fun select(mediaId: String, enqueue: Boolean): AudioItem =
        api.execute(if (enqueue) Mutation.enqueueAndSelectAudio(mediaId) else Mutation.playAudio(mediaId))

    internal suspend fun snapshot(limit: Int): com.ismartcoding.plain.models.AudioQueueSnapshot =
        api.execute(Query.audioQueueSnapshot(limit))

    internal suspend fun playbackSettings(): com.ismartcoding.plain.models.AudioPlaybackSettings =
        api.execute(Query.audioPlaybackSettings())

    suspend fun playbackSpeed(): Float =
        com.ismartcoding.plain.models.audioPlaybackSpeed(api.execute(Query.prefs()))

    suspend fun playback(): AudioPlayback =
        api.execute(Query.audioPlayback())

    suspend fun resolveNext(isNext: Boolean, shuffle: Boolean = false): AudioItem? {
        val current = playback()
        val action = if (isNext) AudioSelectionAction.NEXT else AudioSelectionAction.PREVIOUS
        val result = api.execute(
            Mutation.advanceAudioSelection(action, current.path.orEmpty(), current.revision, shuffle),
        )
        return result?.track
    }

    suspend fun replace(query: String, sortBy: FileSortBy, shuffle: Boolean = false): AudioItem? =
        api.execute(Mutation.replaceAudioQueue(query, sortBy, shuffle))

    suspend fun clearQueue() {
        api.execute(Mutation.clearAudioQueue())
    }

    suspend fun enqueue(items: List<AudioItem>) {
        val ids = items.mapNotNull { it.mediaId }.filter { it.isNotEmpty() }
        if (ids.isNotEmpty()) add(selectionQuery(ids.toSet()))
    }

    suspend fun add(query: String) {
        api.execute(Mutation.addAudioToQueue(query))
    }

    suspend fun removeQueued(mediaId: String) {
        api.execute(Mutation.removeAudioFromQueue(mediaId))
    }

    suspend fun reorderQueued(mediaIds: List<String>) {
        api.execute(Mutation.reorderAudioQueue(mediaIds))
    }

    suspend fun queuedMediaIds(): Set<String> =
        api.execute(Query.audioQueueIds(Int.MAX_VALUE))
            .mapNotNull { it.mediaId }
            .toSet()

    suspend fun queueTotal(): Int =
        api.execute(Query.audioQueueItemCount())

    suspend fun queuePage(offset: Int, limit: Int): List<AudioItem> =
        queuePageFiltered("", offset, limit)

    suspend fun queuePageFiltered(query: String, offset: Int, limit: Int): List<AudioItem> =
        api.execute(Query.audioQueueItems(offset, limit, query))
}
