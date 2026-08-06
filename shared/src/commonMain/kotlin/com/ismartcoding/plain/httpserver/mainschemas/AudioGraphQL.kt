package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.helpers.coMain
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.audio.toPlaylistAudio
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.events.ClearAudioPlaylistEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.audioClear
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.playlistAudioFromPath
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.preferences.AudioPlayModePreference
import com.ismartcoding.plain.preferences.AudioPlayingPreference
import com.ismartcoding.plain.preferences.AudioPlaylistPreference
import com.ismartcoding.plain.preferences.AudioSortByPreference
import com.ismartcoding.plain.httpserver.loaders.TagsLoader
import com.ismartcoding.plain.httpserver.models.Audio
import com.ismartcoding.plain.httpserver.models.PlaylistAudio
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun audioCount(query: String): Int {
    return if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
        countMedia(DataType.AUDIO, query)
    } else {
        0
    }
}

@GraphQLMutation
suspend fun playAudio(path: String): PlaylistAudio {
    val audio = playlistAudioFromPath(path)
    AudioPlayingPreference.putAsync(audio.path)
    if (!AudioPlaylistPreference.getValueAsync().any { it.path == audio.path }) {
        AudioPlaylistPreference.addAsync(listOf(audio))
    }
    return audio.toModel()
}

@GraphQLMutation
suspend fun updateAudioPlayMode(mode: MediaPlayMode): Boolean {
    AudioPlayModePreference.putAsync(mode)
    return true
}

@GraphQLMutation
suspend fun clearAudioPlaylist(): Boolean {
    AudioPlayingPreference.putAsync("")
    AudioPlaylistPreference.putAsync(arrayListOf())
    coMain {
        audioClear()
    }
    sendEvent(ClearAudioPlaylistEvent())
    return true
}

@GraphQLMutation
suspend fun deletePlaylistAudio(path: String): Boolean {
    AudioPlaylistPreference.deleteAsync(setOf(path))
    return true
}

@GraphQLMutation
suspend fun addPlaylistAudios(query: String): Boolean {
    // 1000 items at most
    val items = searchMedia(DataType.AUDIO, query, 1000, 0, AudioSortByPreference.getValueAsync())
        .filterIsInstance<DAudio>()
    AudioPlaylistPreference.addAsync(items.map { it.toPlaylistAudio() })
    return true
}

@GraphQLMutation
suspend fun reorderPlaylistAudios(paths: List<String>): Boolean {
    // Get current playlist
    val currentPlaylist = AudioPlaylistPreference.getValueAsync()
    if (currentPlaylist.isEmpty() || paths.isEmpty()) {
        return true
    }

    // Create a map of paths to audio items
    val audioMap = currentPlaylist.associateBy { it.path }

    // Reorder the playlist based on the provided paths
    val reorderedPlaylist = mutableListOf<DPlaylistAudio>()

    // First add audio items in the new order
    paths.forEach { path ->
        audioMap[path]?.let { audio ->
            reorderedPlaylist.add(audio)
        }
    }

    // Add other audio items that are not in the reorder list (keep their original positions)
    currentPlaylist.forEach { audio ->
        if (!paths.contains(audio.path)) {
            reorderedPlaylist.add(audio)
        }
    }

    // Save the reordered playlist
    AudioPlaylistPreference.putAsync(reorderedPlaylist)

    return true
}

@GraphQLQuery
suspend fun audios(offset: Int, limit: Int, query: String, sortBy: FileSortBy): List<Audio> {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    return searchMedia(DataType.AUDIO, query, limit, offset, sortBy)
        .filterIsInstance<DAudio>()
        .map { it.toModel() }
}

fun SchemaBuilder.addAudioSchema() {
    type<Audio> {
        dataProperty("tags") {
            prepare { item -> item.id.value }
            loader { ids ->
                TagsLoader.load(ids, DataType.AUDIO)
            }
        }
    }
}
