package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.AudioCommand
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.models.AudioPlayback
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.selectionQuery

object AudioQueueClient : AudioQueueApi()

open class AudioQueueApi internal constructor(private val api: GraphqlClient = GraphqlApi) {
    internal suspend fun select(mediaId: String, enqueue: Boolean): AudioItem =
        api.execute(if (enqueue) Mutation.enqueueAndSelectAudio(mediaId) else Mutation.selectAudio(mediaId))

    internal suspend fun snapshot(limit: Int): com.ismartcoding.plain.models.AudioQueueSnapshot =
        api.execute(Query.audioQueueSnapshot(limit))

    internal suspend fun playbackSettings(): com.ismartcoding.plain.models.AudioPlaybackSettings =
        api.execute(Query.audioPlaybackSettings())

    suspend fun playbackSpeed(): Float =
        com.ismartcoding.plain.models.audioPlaybackSpeed(api.execute(Query.prefs(listOf("audio_playback_speed"))))

    internal suspend fun playMode(): MediaPlayMode =
        com.ismartcoding.plain.models.audioPlayMode(api.execute(Query.prefs(listOf("audio_play_mode"))))

    suspend fun playback(): AudioPlayback =
        api.execute(Query.audioPlayback())

    suspend fun resolveNext(isNext: Boolean, shuffle: Boolean = false): AudioItem? {
        val action = if (isNext) AudioCommand.NEXT else AudioCommand.PREVIOUS
        return nextMediaId(snapshot(1), action, shuffle)?.let { select(it, enqueue = false) }
    }

    internal fun nextMediaId(
        snapshot: com.ismartcoding.plain.models.AudioQueueSnapshot,
        action: AudioCommand,
        shuffle: Boolean = false,
    ): String? {
        val current = snapshot.audioPlayback
        val mode = com.ismartcoding.plain.models.audioPlayMode(snapshot.prefs)
        if (action == AudioCommand.COMPLETED && mode == MediaPlayMode.REPEAT_ONE) {
            return current.mediaId
        }
        val ids = snapshot.queuedIds.mapNotNull { it.mediaId }
        if (ids.isEmpty()) return null
        val index = current.mediaId?.let { ids.indexOf(it) } ?: -1
        if (shuffle || mode == MediaPlayMode.SHUFFLE) {
            if (ids.size == 1 || index < 0) return ids.random()
            val choice = kotlin.random.Random.nextInt(ids.size - 1)
            return ids[if (choice >= index) choice + 1 else choice]
        }
        return if (action == AudioCommand.PREVIOUS) {
            ids[if (index < 0) ids.lastIndex else (index + ids.lastIndex) % ids.size]
        } else {
            ids[(index + 1) % ids.size]
        }
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
