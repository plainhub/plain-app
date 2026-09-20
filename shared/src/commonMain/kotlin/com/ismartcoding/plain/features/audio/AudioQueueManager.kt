package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.db.AudioPlaySource
import com.ismartcoding.plain.db.DAudioPlaylist
import com.ismartcoding.plain.db.DAudioPlaylistItem
import com.ismartcoding.plain.db.DAudioPlayHistory
import com.ismartcoding.plain.db.DAudioQueueItem
import com.ismartcoding.plain.db.DAudioQueueSource
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.helpers.StringHelper
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.preferences.AudioPlayingPreference
import com.ismartcoding.plain.preferences.AudioPlaylistPreference
import com.ismartcoding.plain.preferences.AudioQueueMigratedPreference
import com.ismartcoding.plain.preferences.AudioSortByPreference
import kotlin.random.Random

/**
 * Plan-B audio queue. The queue is never a copied list of items; it is:
 *
 *  - a **source** — a user playlist or the whole library — plus
 *  - a small **manual queue** ("play next" / "add to queue" items), and
 *  - the **current** track.
 *
 * The next/previous track is resolved from the source on demand with indexed
 * queries, so playing a 10k-item library costs the same as playing one item.
 *
 * Playback order (ranks 0..total-1):
 *
 *      source[0 .. currentPos]  ->  manual queue  ->  source[currentPos+1 ..]
 *
 * i.e. manually queued tracks play right after the current one, then the
 * source continues where it left off. "Play next" inserts at the front of the
 * manual queue. Shuffle picks a random rank of the same order.
 */
object AudioQueueManager {
    private val queueDao get() = AppDatabase.instance.audioQueueDao()
    private val playlistDao get() = AppDatabase.instance.audioPlaylistDao()
    private val itemDao get() = AppDatabase.instance.audioPlaylistItemDao()
    private val historyDao get() = AppDatabase.instance.audioPlayHistoryDao()

    private const val HISTORY_KEEP = 200

    // ---------- current source ----------

    suspend fun source(): DAudioQueueSource = queueDao.getSource() ?: DAudioQueueSource()

    private suspend fun saveSource(source: DAudioQueueSource) = queueDao.putSource(source)

    /** Playlist id when the active playback source is a user playlist, else null. */
    suspend fun activePlaylistId(): String? {
        val src = source()
        return if (src.source == AudioPlaySource.PLAYLIST) src.playlistId else null
    }

    // ---------- legacy import ----------

    /** One-shot import of the old DataStore queue into the manual queue table. */
    suspend fun ensureMigrated() {
        if (AudioQueueMigratedPreference.getAsync()) return
        val legacy = AudioPlaylistPreference.getValueAsync()
        val rows = legacy.mapIndexed { i, a -> a.toQueueItem(i) }
        queueDao.insertAll(rows)
        saveSource(DAudioQueueSource(currentPath = AudioPlayingPreference.getValueAsync()))
        AudioQueueMigratedPreference.putAsync(true)
        AudioPlaylistPreference.putAsync(listOf())
        LogCat.d("AudioQueueManager: imported legacy queue, ${rows.size} items")
    }

    // ---------- set playback source ----------

    /** Play a user playlist: make it the source, clear the manual queue. Returns the track to start with. */
    suspend fun setPlaylistSource(playlistId: String, startPath: String?): DPlaylistAudio? {
        ensureMigrated()
        queueDao.deleteAll()
        val items = itemDao.getByPlaylist(playlistId)
        if (items.isEmpty()) {
            saveSource(DAudioQueueSource())
            return null
        }
        val start = items.find { it.audioPath == startPath } ?: items[0]
        saveSource(
            DAudioQueueSource(
                source = AudioPlaySource.PLAYLIST,
                playlistId = playlistId,
                currentPath = start.audioPath,
                currentIndex = start.position,
            )
        )
        recordHistory(start.audioPath, start.title, start.artist, start.duration)
        return start.toPlaylistAudio()
    }

    /** Play the whole library: make it the source, clear the manual queue. Returns the track to start with. */
    suspend fun setLibrarySource(startPath: String?, shuffle: Boolean): DPlaylistAudio? {
        ensureMigrated()
        queueDao.deleteAll()
        val size = countMedia(DataType.AUDIO, "")
        if (size == 0) {
            saveSource(DAudioQueueSource())
            return null
        }
        val sortBy = AudioSortByPreference.getValueAsync()
        var startIndex = 0
        val start = if (shuffle) {
            startIndex = Random.nextInt(size)
            libraryTrackAt(startIndex)
        } else if (startPath != null) {
            startIndex = locateInLibrary(startPath, size, sortBy)
            if (startIndex >= 0) libraryTrackAt(startIndex) else null
        } else {
            libraryTrackAt(0)
        }
        if (start == null) {
            saveSource(DAudioQueueSource())
            return null
        }
        saveSource(
            DAudioQueueSource(
                source = AudioPlaySource.LIBRARY,
                currentPath = start.path,
                currentIndex = startIndex,
                sortBy = sortBy.name,
            )
        )
        recordHistory(start.path, start.title, start.artist, start.duration)
        return start.toPlaylistAudio()
    }

    /** Reset the source and the manual queue. Stopping playback is the caller's job. */
    suspend fun clearQueue() {
        queueDao.deleteAll()
        saveSource(DAudioQueueSource())
    }

    // ---------- manual queue ----------

    /** Add tracks to the manual queue. [playNext] moves/inserts them at the front. */
    suspend fun enqueue(items: List<DPlaylistAudio>, playNext: Boolean = false) {
        if (items.isEmpty()) return
        ensureMigrated()
        if (playNext) {
            queueDao.shiftPositionsFrom(0, items.size)
            items.forEachIndexed { i, a ->
                queueDao.deleteByPath(a.path)
                queueDao.upsert(a.toQueueItem(i))
            }
        } else {
            var next = queueDao.maxPosition() + 1
            items.forEach { a ->
                queueDao.deleteByPath(a.path)
                queueDao.upsert(a.toQueueItem(next))
                next++
            }
        }
    }

    suspend fun removeQueued(path: String) {
        queueDao.deleteByPath(path)
    }

    /** Drag & drop reorder inside the manual queue. */
    suspend fun moveQueued(from: Int, to: Int) {
        val items = queueDao.allItems().toMutableList()
        if (from !in items.indices || to !in items.indices || from == to) return
        items.add(to, items.removeAt(from))
        items.forEachIndexed { i, item -> queueDao.updatePosition(item.path, i) }
    }

    /** Reorder the manual queue to match [paths]; unknown paths keep their order at the end. */
    suspend fun reorderQueued(paths: List<String>) {
        if (paths.isEmpty()) return
        val all = queueDao.allItems()
        if (all.isEmpty()) return
        val known = paths.toSet()
        val ordered = paths.mapNotNull { p -> all.firstOrNull { it.path == p } } +
            all.filter { it.path !in known }
        ordered.forEachIndexed { i, item -> queueDao.updatePosition(item.path, i) }
    }

    suspend fun queuedPaths(): Set<String> = queueDao.allItems().map { it.path }.toSet()

    /** Cascade cleanup when media files are deleted or trashed. */
    suspend fun removePaths(paths: Collection<String>) {
        if (paths.isEmpty()) return
        val list = paths.toList()
        queueDao.deleteByPaths(list)
        historyDao.deleteByPaths(list)
        itemDao.deleteByPaths(list)
        val src = source()
        if (src.currentPath in list) {
            saveSource(src.copy(currentPath = "", currentIndex = -1))
        }
    }

    // ---------- next / previous resolution ----------

    /**
     * Resolve the next/previous track in the playback order and advance the
     * current track. Returns null when there is nothing to play.
     */
    suspend fun resolveNext(isNext: Boolean, shuffle: Boolean): DPlaylistAudio? {
        ensureMigrated()
        val order = playbackOrder()
        if (order.total == 0) return null
        val superseded = supersededSourcePaths(order)
        val current = currentRank(order)
        val total = order.total - superseded.size
        var target = if (shuffle) {
            Random.nextInt(total)
        } else {
            val from = if (current < 0) (if (isNext) -1 else 0) else current
            if (isNext) (from + 1) % total else (from - 1 + total) % total
        }
        // Walk past source copies of manually queued tracks — they play from
        // their manual slot instead and must not repeat.
        var audio = trackAt(order, target)
        while (audio != null && audio.path in superseded) {
            target = if (isNext) (target + 1) % order.total else (target - 1 + order.total) % order.total
            val next = trackAt(order, target)
            if (next?.path == audio.path) break
            audio = next
        }
        if (audio == null || audio.path in superseded) return null
        saveCurrent(order, target)
        recordHistory(audio.path, audio.title, audio.artist, audio.duration)
        return audio
    }

    /**
     * Source copies of manually queued tracks are superseded: the manual slot
     * is the one that plays, so they are skipped in total/count, rendering and
     * sequential resolution. This is what keeps the queue free of duplicates.
     */
    private suspend fun supersededSourcePaths(order: Order): Set<String> {
        if (order.manualCount == 0 || order.source.source == AudioPlaySource.NONE) return emptySet()
        val queued = queueDao.allItems().map { it.path }
        return when (order.source.source) {
            AudioPlaySource.PLAYLIST ->
                itemDao.getByPlaylist(order.source.playlistId)
                    .filter { it.audioPath in queued.toSet() }
                    .map { it.audioPath }
                    .toSet()
            else -> {
                val found = searchMedia(DataType.AUDIO, "ids:" + queued.joinToString(","), queued.size, 0, librarySortOf(order.source))
                    .filterIsInstance<DAudio>()
                    .map { it.path }
                found.toSet()
            }
        }
    }

    /** Record that [path] started playing (manual jumps included). */
    suspend fun onPlaying(path: String, title: String, artist: String, duration: Long) {
        if (path.isEmpty()) return
        val src = source()
        // A manual jump breaks the cached library position; mark it unknown and
        // re-locate lazily on the next sequential skip.
        val newIndex = if (src.source == AudioPlaySource.LIBRARY && path != src.currentPath) -1 else src.currentIndex
        if (src.currentPath != path || newIndex != src.currentIndex) {
            saveSource(src.copy(currentPath = path, currentIndex = newIndex))
        }
        recordHistory(path, title, artist, duration)
    }

    private suspend fun recordHistory(path: String, title: String, artist: String, duration: Long) {
        val existing = historyDao.getByPath(path)
        historyDao.upsert(
            if (existing != null) {
                existing.copy(
                    playCount = existing.playCount + 1,
                    playedAt = TimeHelper.now(),
                    title = title,
                    artist = artist,
                    duration = duration,
                )
            } else {
                DAudioPlayHistory(path = path, title = title, artist = artist, duration = duration, playCount = 1)
            },
        )
        if (historyDao.count() > HISTORY_KEEP * 5 / 4) {
            historyDao.trim(HISTORY_KEEP)
        }
    }

    /** Total plays per artist, from the play history window. */
    suspend fun artistPlayCounts(): Map<String, Long> =
        historyDao.playCountsByArtist().associate { it.artist to it.count }

    // ---------- playback order ----------

    /**
     * Snapshot of the playback order:
     * `source[0..currentPos] -> manual[0..manualCount) -> source[currentPos+1..sourceSize)`.
     */
    private class Order(
        val source: DAudioQueueSource,
        val manualCount: Int,
        val sourceSize: Int,
        /** Position of the current track inside the source, -1 if not in it. */
        val currentPos: Int,
    ) {
        val total: Int get() = manualCount + sourceSize
    }

    private suspend fun playbackOrder(): Order {
        ensureMigrated()
        val src = source()
        val manualCount = queueDao.count()
        val sourceSize = sourceSizeOf(src)
        val currentPos = currentPosInSource(src, sourceSize)
        return Order(src, manualCount, sourceSize, currentPos)
    }

    private suspend fun sourceSizeOf(src: DAudioQueueSource): Int = when (src.source) {
        AudioPlaySource.PLAYLIST -> itemDao.countByPlaylist(src.playlistId)
        AudioPlaySource.LIBRARY -> countMedia(DataType.AUDIO, "")
        AudioPlaySource.NONE -> 0
    }

    private suspend fun currentPosInSource(src: DAudioQueueSource, sourceSize: Int): Int {
        val path = src.currentPath
        if (path.isEmpty() || sourceSize == 0) return -1
        return when (src.source) {
            AudioPlaySource.PLAYLIST -> itemDao.getByPath(src.playlistId, path)?.position ?: -1
            AudioPlaySource.LIBRARY -> {
                val sortBy = librarySortOf(src)
                if (src.currentIndex in 0 until sourceSize) {
                    val at = libraryTrackAt(src.currentIndex)
                    if (at?.path == path) return src.currentIndex
                }
                val index = locateInLibrary(path, sourceSize, sortBy)
                if (index >= 0) saveSource(src.copy(currentIndex = index))
                index
            }
            AudioPlaySource.NONE -> -1
        }
    }

    private suspend fun librarySortOf(src: DAudioQueueSource): FileSortBy =
        FileSortBy.entries.firstOrNull { it.name == src.sortBy } ?: AudioSortByPreference.getValueAsync()

    /** Rank of the current track in the playback order, -1 if unknown. */
    private suspend fun currentRank(order: Order): Int {
        val path = order.source.currentPath
        if (path.isEmpty()) return -1
        val queued = queueDao.itemByPath(path)
        if (queued != null) {
            // Queued items start right after the head: rank = currentPos+1 + rank in queue.
            return order.currentPos + 1 + queueDao.countBefore(queued.position)
        }
        // The current track is in the source and always closes the head segment.
        return order.currentPos
    }

    private suspend fun trackAt(order: Order, rank: Int): DPlaylistAudio? {
        if (rank < 0 || rank >= order.total) return null
        return when {
            rank <= order.currentPos -> sourceTrackAt(order.source, rank)
            rank <= order.currentPos + order.manualCount ->
                queueDao.itemAt(rank - order.currentPos - 1)?.toPlaylistAudio()
            else -> sourceTrackAt(order.source, rank - order.manualCount)
        }
    }

    private suspend fun saveCurrent(order: Order, rank: Int) {
        val path = trackAt(order, rank)?.path ?: return
        val sourcePos = when (order.source.source) {
            AudioPlaySource.PLAYLIST -> itemDao.getByPath(order.source.playlistId, path)?.position ?: -1
            AudioPlaySource.LIBRARY -> when {
                rank <= order.currentPos -> rank
                rank <= order.currentPos + order.manualCount -> -1
                else -> rank - order.manualCount
            }
            AudioPlaySource.NONE -> -1
        }
        saveSource(order.source.copy(currentPath = path, currentIndex = sourcePos))
    }

    /**
     * Text-filtered queue page: ranks are resolved lazily and items whose
     * title/artist/path do not contain [text] are skipped before pagination,
     * so offset/limit apply to the filtered sequence.
     */
    suspend fun queuePageFiltered(text: String, offset: Int, limit: Int): List<DPlaylistAudio> {
        if (text.isEmpty()) return queuePage(offset, limit)
        val order = playbackOrder()
        val superseded = supersededSourcePaths(order)
        val q = text.lowercase()
        val out = mutableListOf<DPlaylistAudio>()
        var matched = 0
        var rank = 0
        fun matches(a: DPlaylistAudio): Boolean =
            a.title.lowercase().contains(q) || a.artist.lowercase().contains(q) || a.path.lowercase().contains(q)
        while (rank < order.total && out.size < limit) {
            val item = trackAt(order, rank) ?: break
            rank++
            if (item.path in superseded) continue
            if (matches(item)) {
                if (matched >= offset) out.add(item)
                matched++
            }
        }
        return out
    }

    suspend fun queueTotal(): Int {
        val order = playbackOrder()
        return order.total - supersededSourcePaths(order).size
    }

    /** A page of the playback order — never materializes the whole queue. */
    suspend fun queuePage(offset: Int, limit: Int): List<DPlaylistAudio> {
        val order = playbackOrder()
        val superseded = supersededSourcePaths(order)
        val out = mutableListOf<DPlaylistAudio>()
        var rank = offset.coerceAtLeast(0)
        val end = minOf(offset + limit, order.total)
        while (rank < end) {
            when {
                rank <= order.currentPos -> { // head: source up to the current track
                    val segEnd = minOf(end, order.currentPos + 1)
                    out += sourcePage(order.source, rank, segEnd - rank).filter { it.path !in superseded }
                    rank = segEnd
                }
                rank <= order.currentPos + order.manualCount -> { // manual queue
                    val segEnd = minOf(end, order.currentPos + 1 + order.manualCount)
                    out += queueDao.itemsPage(segEnd - rank, rank - order.currentPos - 1).map { it.toPlaylistAudio() }
                    rank = segEnd
                }
                else -> { // tail: the rest of the source
                    out += sourcePage(order.source, rank - order.manualCount, end - rank).filter { it.path !in superseded }
                    rank = end
                }
            }
        }
        return out
    }

    private suspend fun sourceTrackAt(src: DAudioQueueSource, position: Int): DPlaylistAudio? {
        return when (src.source) {
            AudioPlaySource.PLAYLIST ->
                itemDao.pageByPlaylist(src.playlistId, 1, position).firstOrNull()?.toPlaylistAudio()
            AudioPlaySource.LIBRARY -> libraryTrackAt(position)?.toPlaylistAudio()
            AudioPlaySource.NONE -> null
        }
    }

    private suspend fun sourcePage(src: DAudioQueueSource, offset: Int, limit: Int): List<DPlaylistAudio> {
        if (limit <= 0) return listOf()
        return when (src.source) {
            AudioPlaySource.PLAYLIST ->
                itemDao.pageByPlaylist(src.playlistId, limit, offset).map { it.toPlaylistAudio() }
            AudioPlaySource.LIBRARY ->
                searchMedia(DataType.AUDIO, "", limit, offset, librarySortOf(src))
                    .filterIsInstance<DAudio>()
                    .map { it.toPlaylistAudio() }
            AudioPlaySource.NONE -> listOf()
        }
    }

    private suspend fun libraryTrackAt(position: Int): DAudio? {
        if (position < 0) return null
        return searchMedia(DataType.AUDIO, "", 1, position, AudioSortByPreference.getValueAsync())
            .filterIsInstance<DAudio>()
            .firstOrNull()
    }

    /** Index of [path] in the library under [sortBy], paged scan (cursor re-location only). */
    private suspend fun locateInLibrary(path: String, size: Int, sortBy: FileSortBy): Int {
        val pageSize = 500
        var offset = 0
        while (offset < size) {
            val page = searchMedia(DataType.AUDIO, "", pageSize, offset, sortBy).filterIsInstance<DAudio>()
            if (page.isEmpty()) break
            val hit = page.indexOfFirst { it.path == path }
            if (hit >= 0) return offset + hit
            offset += page.size
        }
        return -1
    }

    // ---------- user playlists ----------

    suspend fun playlist(id: String): DAudioPlaylist? = playlistDao.getById(id)

    suspend fun playlists(): List<Pair<DAudioPlaylist, Int>> {
        ensureMigrated()
        val all = playlistDao.getAll()
        val counts = playlistDao.itemCounts().associate { it.playlistId to it.cnt }
        return all.map { it to (counts[it.id] ?: 0) }
    }

    suspend fun createPlaylist(name: String): DAudioPlaylist {
        ensureMigrated()
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
        val src = source()
        if (src.source == AudioPlaySource.PLAYLIST && src.playlistId == id) {
            saveSource(src.copy(source = AudioPlaySource.NONE, playlistId = ""))
        }
    }

    suspend fun addPlaylistItems(playlistId: String, items: List<DPlaylistAudio>): Int {
        ensureMigrated()
        var added = 0
        var next = itemDao.maxPosition(playlistId) + 1
        items.forEach { a ->
            val row = DAudioPlaylistItem(
                id = StringHelper.shortUUID(),
                playlistId = playlistId,
                audioPath = a.path,
                title = a.title,
                artist = a.artist,
                duration = a.duration,
                position = next,
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

    suspend fun playlistItemsPage(playlistId: String, offset: Int, limit: Int): List<DAudioPlaylistItem> =
        itemDao.pageByPlaylist(playlistId, limit, offset)

    suspend fun playlistItemsPageFiltered(playlistId: String, text: String, offset: Int, limit: Int): List<DAudioPlaylistItem> =
        itemDao.pageByPlaylistText(playlistId, "%$text%", limit, offset)

    suspend fun playlistItemCount(playlistId: String): Int = itemDao.countByPlaylist(playlistId)

    // ---------- play history ----------

    suspend fun recentPage(limit: Int, offset: Int): List<DAudioPlayHistory> = historyDao.page(limit, offset)

    suspend fun recentPageFiltered(text: String, limit: Int, offset: Int): List<DAudioPlayHistory> =
        historyDao.pageText("%$text%", limit, offset)
}

fun DAudioPlaylistItem.toPlaylistAudio(): DPlaylistAudio =
    DPlaylistAudio(title = title, path = audioPath, artist = artist, duration = duration)

fun DAudioQueueItem.toPlaylistAudio(): DPlaylistAudio =
    DPlaylistAudio(title = title, path = path, artist = artist, duration = duration)

fun DPlaylistAudio.toQueueItem(position: Int): DAudioQueueItem =
    DAudioQueueItem(path = path, position = position, title = title, artist = artist, duration = duration)
