package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.events.ClearAudioQueueEvent
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.features.audio.toPlaylistAudio
import kotlin.reflect.typeOf
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.audioClear
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.audioPlayerProgress
import com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.getAudioLyrics
import com.ismartcoding.plain.platform.playlistAudioFromPath
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.preferences.AudioPlayModePreference
import com.ismartcoding.plain.preferences.AudioPlayingPreference
import com.ismartcoding.plain.preferences.AudioSortByPreference
import com.ismartcoding.plain.httpserver.loaders.TagsLoader
import com.ismartcoding.plain.httpserver.models.Audio
import com.ismartcoding.plain.httpserver.models.AudioPlayback
import com.ismartcoding.plain.httpserver.models.AudioPlayHistory
import com.ismartcoding.plain.httpserver.models.AudioPlaylist
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.AudioItem
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun audioCount(query: String): Int {
    return if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
        countMedia(DataType.AUDIO, query)
    } else {
        0
    }
}

/** The active playback queue (manual items + context), paginated. */
@GraphQLQuery
suspend fun audioQueueItems(offset: Int, limit: Int, query: String): List<AudioItem> {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    val text = QueryHelper.textOf(query)
    return (if (text.isEmpty()) AudioQueueManager.queuePage(offset, limit) else AudioQueueManager.queuePageFiltered(text, offset, limit)).map { it.toModel() }
}

@GraphQLQuery
suspend fun audioQueueItemCount(): Int {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    return AudioQueueManager.queueTotal()
}

/** Player state: play mode preference plus the current track path. */
@GraphQLQuery
suspend fun audioPlayback(): AudioPlayback {
    return AudioPlayback(
        mode = AudioPlayModePreference.getValueAsync(),
        currentPath = AudioPlayingPreference.getValueAsync(),
        isPlaying = audioIsPlayingFlow().value,
        positionMs = audioPlayerProgress(),
    )
}

/** Play the given track: adds it to the manual queue when missing and marks it current. */
@GraphQLMutation
suspend fun playAudio(path: String): AudioItem {
    val audio = playlistAudioFromPath(path)
    AudioPlayingPreference.putAsync(audio.path)
    AudioQueueManager.enqueue(listOf(audio))
    return audio.toModel()
}

@GraphQLMutation
suspend fun updateAudioPlayMode(mode: MediaPlayMode): Boolean {
    AudioPlayModePreference.putAsync(mode)
    return true
}

@GraphQLMutation
suspend fun clearAudioQueue(): Boolean {
    AudioPlayingPreference.putAsync("")
    AudioQueueManager.clearQueue()
    coMain {
        audioClear()
    }
    sendEvent(ClearAudioQueueEvent())
    return true
}

@GraphQLMutation
suspend fun removeAudioFromQueue(path: String): Boolean {
    AudioQueueManager.removeQueued(path)
    return true
}

@GraphQLMutation
suspend fun addAudiosToQueue(query: String): Boolean {
    // 1000 items at most
    val items = searchMedia(DataType.AUDIO, query, 1000, 0, AudioSortByPreference.getValueAsync())
        .filterIsInstance<DAudio>()
    AudioQueueManager.enqueue(items.map { it.toPlaylistAudio() })
    return true
}

@GraphQLMutation
suspend fun reorderAudioQueue(paths: List<String>): Boolean {
    AudioQueueManager.reorderQueued(paths)
    return true
}

@GraphQLQuery
suspend fun audios(offset: Int, limit: Int, query: String, sortBy: FileSortBy): List<Audio> {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    return searchMedia(DataType.AUDIO, query, limit, offset, sortBy)
        .filterIsInstance<DAudio>()
        .map { it.toModel() }
}

@GraphQLQuery
suspend fun audioLyrics(path: String): String? {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    return getAudioLyrics(path).ifEmpty { null }
}

// ---------- user playlists ----------

@GraphQLQuery
suspend fun audioPlaylists(): List<AudioPlaylist> {
    return AudioQueueManager.playlists().map { (pl, count) ->
        AudioPlaylist(id = ID(pl.id), name = pl.name, itemCount = count, createdAt = pl.createdAt, updatedAt = pl.updatedAt)
    }
}

@GraphQLQuery
suspend fun audioPlaylistItems(id: ID, offset: Int, limit: Int, query: String): List<AudioItem> {
    val text = QueryHelper.textOf(query)
    val items = if (text.isEmpty()) AudioQueueManager.playlistItemsPage(id.value, offset, limit)
    else AudioQueueManager.playlistItemsPageFiltered(id.value, text, offset, limit)
    return items.map { it.toPlaylistAudio().toModel() }
}

@GraphQLQuery
suspend fun audioPlaylistItemCount(id: ID): Int {
    return AudioQueueManager.playlistItemCount(id.value)
}

@GraphQLQuery
suspend fun audioPlayHistory(offset: Int, limit: Int, query: String): List<AudioPlayHistory> {
    val text = QueryHelper.textOf(query)
    val items = if (text.isEmpty()) AudioQueueManager.recentPage(limit, offset)
    else AudioQueueManager.recentPageFiltered(text, limit, offset)
    return items.map { it.toModel() }
}

@GraphQLMutation
suspend fun createAudioPlaylist(name: String): AudioPlaylist {
    val pl = AudioQueueManager.createPlaylist(name)
    return AudioPlaylist(id = ID(pl.id), name = pl.name, itemCount = 0, createdAt = pl.createdAt, updatedAt = pl.updatedAt)
}

@GraphQLMutation
suspend fun renameAudioPlaylist(id: ID, name: String): Boolean {
    AudioQueueManager.renamePlaylist(id.value, name)
    return true
}

@GraphQLMutation
suspend fun deleteAudioPlaylist(id: ID): Boolean {
    AudioQueueManager.deletePlaylist(id.value)
    return true
}

@GraphQLMutation
suspend fun addAudioPlaylistItems(id: ID, paths: List<String>): Boolean {
    AudioQueueManager.addPlaylistItems(id.value, paths.map { playlistAudioFromPath(it) })
    return true
}

@GraphQLMutation
suspend fun removeAudioPlaylistItem(id: ID, path: String): Boolean {
    AudioQueueManager.removePlaylistItem(id.value, path)
    return true
}

/**
 * Play a user playlist: sets it as the playback context and starts playback.
 * Returns the track that started, or null when the context has nothing to play.
 */
@GraphQLMutation
suspend fun playAudioPlaylist(id: ID, path: String? = null, shuffle: Boolean = false): AudioItem? {
    val start = AudioQueueManager.setPlaylistSource(id.value, path)
    if (start == null) {
        return null
    }
    val track = if (shuffle) {
        AudioQueueManager.resolveNext(isNext = true, shuffle = true)
    } else {
        start
    }
    if (track != null) {
        coMain { audioJustPlayWithNotificationCheck(track) }
    }
    return track?.toModel()
}

/**
 * Play the whole audio library: full-library playback context.
 * Returns the track that started, or null when the library is empty.
 */
@GraphQLMutation
suspend fun playAllAudios(path: String? = null, shuffle: Boolean = false): AudioItem? {
    val start = AudioQueueManager.setLibrarySource(startPath = path, shuffle = shuffle)
    if (start != null) {
        coMain { audioJustPlayWithNotificationCheck(start) }
    }
    return start?.toModel()
}

fun SchemaBuilder.addAudioSchema() {
    type<AudioPlayback> {
        // currentPath is "" when idle — expose null instead of the empty sentinel.
        property("currentPath", typeOf<String?>(), { it: AudioPlayback -> it.currentPath.ifEmpty { null } })
    }
    type<Audio> {
        dataProperty("tags") {
            prepare { item -> item.id.value }
            loader { ids ->
                TagsLoader.load(ids, DataType.AUDIO)
            }
        }
    }
}
