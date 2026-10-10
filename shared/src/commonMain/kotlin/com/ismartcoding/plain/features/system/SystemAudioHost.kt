package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.preferences.UserPrefs
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.serialization.json.*

internal object SystemAudioHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemAudioPlaylistTracks" -> JsonHelper.jsonEncodeToElement(
            params.getValue("paths").jsonArray.map { value ->
                val track = com.ismartcoding.plain.platform.playlistAudioFromPath(
                    value.jsonPrimitive.content)
                playlistTrackFacts(track)
        })
        "systemAudioSearchTracks" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.searchMedia(
                com.ismartcoding.plain.enums.DataType.AUDIO,
                params.getValue("query").jsonPrimitive.content,
                params.getValue("limit").jsonPrimitive.int,
                params.getValue("offset").jsonPrimitive.int,
                com.ismartcoding.plain.features.file.FileSortBy.valueOf(
                    params.getValue("sortBy").jsonPrimitive.content),
            ).filterIsInstance<com.ismartcoding.plain.audio.DAudio>()
            .map { playlistTrackFacts(com.ismartcoding.plain.audio.DPlaylistAudio(
                title = it.title, path = it.path, artist = it.artist,
                durationMs = it.durationMs)) })
        "systemAudioLyrics" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getAudioLyrics(
                params.getValue("path").jsonPrimitive.content))
        "systemAudioPlaybackState" -> JsonHelper.jsonEncodeToElement(AudioPlaybackFacts(
            isPlaying = com.ismartcoding.plain.platform.audioIsPlayingFlow().value,
            positionMs = com.ismartcoding.plain.platform.audioPlayerProgressAsync(),
        ))
        "systemAudioPlayMode" -> when (val mode = params["mode"]) {
            null -> JsonHelper.jsonEncodeToElement(UserPrefs.audioPlayMode.value.name)
            else -> {
                UserPrefs.audioPlayMode.set(com.ismartcoding.plain.enums.MediaPlayMode
                    .valueOf(mode.jsonPrimitive.content))
                JsonHelper.jsonEncodeToElement(UserPrefs.audioPlayMode.value.name)
            }
        }
        "systemAudioLibrarySort" -> JsonHelper.jsonEncodeToElement(UserPrefs.audioSortByValue().name)
        "systemAudioPlay" -> {
            com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck(
                playlistTrack(params.getValue("track").jsonObject))
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemAudioClear" -> {
            com.ismartcoding.plain.platform.audioClear()
            com.ismartcoding.plain.lib.sendEvent(
                com.ismartcoding.plain.events.ClearAudioQueueEvent())
            JsonHelper.jsonEncodeToElement(true)
        }
        else -> error("Unsupported provider operation")
    }

    private fun playlistTrack(track: JsonObject): com.ismartcoding.plain.audio.DPlaylistAudio =
        JsonHelper.jsonDecodeFromElement(track)

    private fun playlistTrackFacts(track: com.ismartcoding.plain.audio.DPlaylistAudio): PlaylistTrackFacts =
        PlaylistTrackFacts(
            title = track.title,
            artist = track.artist,
            path = track.path,
            durationMs = track.durationMs,
        )
}
