package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.db.DAudioPlaylist
import com.ismartcoding.plain.db.DAudioPlaylistItem
import com.ismartcoding.plain.helpers.StringHelper
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.platform.AppDatabase

/** User playlists: metadata and item CRUD, independent of the play queue. */
object AudioPlaylistManager {
    private val playlistDao get() = AppDatabase.instance.audioPlaylistDao()
    private val itemDao get() = AppDatabase.instance.audioPlaylistItemDao()

    suspend fun playlist(id: String): DAudioPlaylist? = playlistDao.getById(id)

    suspend fun playlists(): List<Pair<DAudioPlaylist, Int>> {
        val all = playlistDao.getAll()
        val counts = playlistDao.itemCounts().associate { it.playlistId to it.cnt }
        return all.map { it to (counts[it.id] ?: 0) }
    }

    suspend fun createPlaylist(name: String): DAudioPlaylist {
        val pl = DAudioPlaylist(id = StringHelper.shortUUID(), name = name)
        playlistDao.upsert(pl)
        return pl
    }

    suspend fun renamePlaylist(id: String, name: String) {
        playlistDao.getById(id)?.let {
            playlistDao.upsert(it.copy(name = name))
            playlistDao.touch(id, TimeHelper.now())
        }
    }

    suspend fun deletePlaylist(id: String) {
        playlistDao.delete(id)
        itemDao.deleteByPlaylist(id)
        AudioQueueManager.onPlaylistDeleted(id)
    }

    suspend fun addPlaylistItems(playlistId: String, items: List<DPlaylistAudio>): Int {
        var added = 0
        var next = itemDao.maxSortOrder(playlistId) + 1
        items.forEach { a ->
            val row = DAudioPlaylistItem(
                id = StringHelper.shortUUID(),
                playlistId = playlistId,
                audioPath = a.path,
                title = a.title,
                artist = a.artist,
                albumId = a.albumId,
                durationMs = a.durationMs,
                sortOrder = next,
            )
            if (itemDao.insert(row) != -1L) {
                added++
                next++
            }
        }
        playlistDao.touch(playlistId, TimeHelper.now())
        return added
    }

    suspend fun removePlaylistItem(playlistId: String, path: String) {
        itemDao.deleteByPath(playlistId, path)
        playlistDao.touch(playlistId, TimeHelper.now())
    }

    suspend fun removePlaylistItems(playlistId: String, paths: Collection<String>) {
        if (paths.isEmpty()) return
        itemDao.deleteByPlaylistPaths(playlistId, paths.toList())
        playlistDao.touch(playlistId, TimeHelper.now())
    }

    /** Cascade cleanup when media files are deleted or trashed. */
    suspend fun removePaths(paths: Collection<String>) {
        if (paths.isEmpty()) return
        itemDao.deleteByPaths(paths.toList())
    }

    suspend fun playlistItemsPage(playlistId: String, offset: Int, limit: Int): List<DAudioPlaylistItem> =
        itemDao.pageByPlaylist(playlistId, limit, offset)

    suspend fun playlistItemsPageFiltered(playlistId: String, text: String, offset: Int, limit: Int): List<DAudioPlaylistItem> =
        itemDao.pageByPlaylistText(playlistId, "%$text%", limit, offset)

    suspend fun playlistItemCount(playlistId: String): Int = itemDao.countByPlaylist(playlistId)

    /** Backfills the album snapshot of rows written before the column existed. */
    suspend fun updatePlaylistItemAlbums(updates: List<Pair<String, String>>) {
        updates.forEach { (id, albumId) -> itemDao.updateAlbumId(id, albumId) }
    }
}
