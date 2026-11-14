package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.models.AudioPlaylist
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.selectionQuery

object AudioPlaylistsClient : AudioPlaylistsApi()

open class AudioPlaylistsApi internal constructor(protected val api: GraphqlClient = GraphqlApi) {
    suspend fun playlist(id: String): AudioPlaylist? =
        api.execute(Query.audioPlaylist(id))

    suspend fun playlists(includeCovers: Boolean = false): List<Pair<AudioPlaylist, Int>> =
        api.execute(Query.audioPlaylists(includeCovers))
            .map { it to it.itemCount }

    suspend fun createPlaylist(name: String): AudioPlaylist =
        api.execute(Mutation.createAudioPlaylist(name))

    suspend fun renamePlaylist(id: String, name: String) {
        api.execute(Mutation.updateAudioPlaylist(id, name))
    }

    suspend fun deletePlaylist(id: String) {
        api.execute(Mutation.deleteAudioPlaylist(id))
    }

    suspend fun addPlaylistItems(playlistId: String, items: List<AudioItem>) {
        val ids = items.mapNotNull { it.mediaId }
        if (ids.isEmpty()) return
        api.execute(Mutation.addAudioPlaylistItems(playlistId, selectionQuery(ids)))
    }

    suspend fun removePlaylistItem(playlistId: String, mediaId: String) =
        removePlaylistItems(playlistId, listOf(mediaId))

    suspend fun removePlaylistItems(playlistId: String, mediaIds: Collection<String>) {
        if (mediaIds.isNotEmpty()) {
            api.execute(Mutation.removeAudioPlaylistItems(playlistId, selectionQuery(mediaIds)))
        }
    }

    suspend fun playlistItemsPage(playlistId: String, offset: Int, limit: Int): List<AudioItem> =
        playlistItemsPageFiltered(playlistId, "", offset, limit)

    suspend fun playlistItemsPageFiltered(
        playlistId: String,
        text: String,
        offset: Int,
        limit: Int,
    ): List<AudioItem> =
        api.execute(Query.audioPlaylistItems(playlistId, offset, limit, text))
}
