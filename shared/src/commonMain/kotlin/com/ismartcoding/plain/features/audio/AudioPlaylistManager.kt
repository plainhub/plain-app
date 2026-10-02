package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.db.DAudioPlaylist
import com.ismartcoding.plain.db.DAudioPlaylistItem
import kotlinx.serialization.json.*

object AudioPlaylistManager {
    suspend fun playlist(id: String): DAudioPlaylist? = RustContentApi.query("audioHostPlaylist(id: ${gql(id)}) { $AUDIO_PLAYLIST_FIELDS }")
        .getValue("audioHostPlaylist").takeUnless { it is JsonNull }?.audioPlaylist()
    suspend fun playlists(): List<Pair<DAudioPlaylist, Int>> = RustContentApi.query("audioHostPlaylists { $AUDIO_PLAYLIST_FIELDS }")
        .getValue("audioHostPlaylists").jsonArray.map { it.audioPlaylist() to it.jsonObject.getValue("itemCount").jsonPrimitive.int }
    suspend fun createPlaylist(name: String): DAudioPlaylist = RustContentApi.mutate("audioHostCreatePlaylist(name: ${gql(name)}) { $AUDIO_PLAYLIST_FIELDS }")
        .getValue("audioHostCreatePlaylist").audioPlaylist()
    suspend fun renamePlaylist(id: String, name: String) { RustContentApi.mutate("audioHostRenamePlaylist(id: ${gql(id)}, name: ${gql(name)})") }
    suspend fun deletePlaylist(id: String) { RustContentApi.mutate("audioHostDeletePlaylist(id: ${gql(id)})") }
    suspend fun addPlaylistItems(playlistId: String, items: List<DPlaylistAudio>): Int =
        RustContentApi.mutate("audioHostAddPlaylistItems(id: ${gql(playlistId)}, items: ${items.audioInput()})").getValue("audioHostAddPlaylistItems").jsonPrimitive.int
    suspend fun removePlaylistItem(playlistId: String, path: String) = removePlaylistItems(playlistId,listOf(path))
    suspend fun removePlaylistItems(playlistId: String, paths: Collection<String>) { RustContentApi.mutate("audioHostRemovePlaylistItems(id: ${gql(playlistId)}, paths: ${gqlIds(paths)})") }
    suspend fun playlistItemsPage(playlistId: String, offset: Int, limit: Int): List<DAudioPlaylistItem> = playlistItemsPageFiltered(playlistId,"",offset,limit)
    suspend fun playlistItemsPageFiltered(playlistId: String, text: String, offset: Int, limit: Int): List<DAudioPlaylistItem> =
        RustContentApi.query("audioHostPlaylistItems(id: ${gql(playlistId)}, offset: $offset, limit: $limit, query: ${gql(text)}) { $AUDIO_ITEM_FIELDS }")
            .getValue("audioHostPlaylistItems").jsonArray.map { it.audioItem() }
    suspend fun playlistItemCount(playlistId: String): Int = RustContentApi.query("audioHostPlaylistItemCount(id: ${gql(playlistId)})").getValue("audioHostPlaylistItemCount").jsonPrimitive.int
}
