package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.db.*
import kotlinx.serialization.json.*

internal const val AUDIO_TRACK_FIELDS = "path title artist albumId durationMs"
internal const val AUDIO_PLAYLIST_FIELDS = "id name itemCount createdAt updatedAt"
internal const val AUDIO_ITEM_FIELDS = "id playlistId audioPath title artist albumId durationMs sortOrder addedAt"
internal const val AUDIO_HISTORY_FIELDS = "path title artist durationMs playCount playedAt"
internal fun JsonElement.audioTrack(): DPlaylistAudio = jsonObject.let { r ->
    DPlaylistAudio(r.string("title"), r.string("path"), r.string("artist"), r.getValue("durationMs").jsonPrimitive.long, r.string("albumId"))
}
internal fun List<DPlaylistAudio>.audioInput(): String = joinToString(",", "[", "]") { r ->
    "{path:${gql(r.path)},title:${gql(r.title)},artist:${gql(r.artist)},albumId:${gql(r.albumId)},durationMs:${r.durationMs}}"
}
internal fun JsonElement.audioPlaylist(): DAudioPlaylist = jsonObject.let { r ->
    DAudioPlaylist(r.string("id"), r.string("name"), r.instant("createdAt"), r.instant("updatedAt"))
}
internal fun JsonElement.audioItem(): DAudioPlaylistItem = jsonObject.let { r ->
    DAudioPlaylistItem(id=r.string("id"),playlistId=r.string("playlistId"),audioPath=r.string("audioPath"),title=r.string("title"),artist=r.string("artist"),albumId=r.string("albumId"),durationMs=r.getValue("durationMs").jsonPrimitive.long,sortOrder=r.getValue("sortOrder").jsonPrimitive.int,addedAt=r.instant("addedAt"))
}
internal fun JsonElement.audioHistory(): DAudioPlayHistory = jsonObject.let { r ->
    DAudioPlayHistory(path=r.string("path"),title=r.string("title"),artist=r.string("artist"),durationMs=r.getValue("durationMs").jsonPrimitive.long,playCount=r.getValue("playCount").jsonPrimitive.int,playedAt=r.instant("playedAt"))
}
