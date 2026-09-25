package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.db.DAudioPlayHistory
import com.ismartcoding.plain.helpers.escapeLike
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.platform.AppDatabase

/** Play history: per-track play count and recents, kept to a bounded window. */
object AudioPlayHistoryManager {
    private val historyDao get() = AppDatabase.instance.audioPlayHistoryDao()

    private const val HISTORY_KEEP = 200

    /** Record that [path] started playing (manual jumps included). */
    suspend fun recordHistory(path: String, title: String, artist: String, durationMs: Long) {
        val existing = historyDao.getByPath(path)
        historyDao.upsert(
            if (existing != null) {
                existing.copy(
                    playCount = existing.playCount + 1,
                    playedAt = TimeHelper.now(),
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                )
            } else {
                DAudioPlayHistory(path = path, title = title, artist = artist, durationMs = durationMs, playCount = 1)
            },
        )
        if (historyDao.count() > HISTORY_KEEP * 5 / 4) {
            historyDao.trim(HISTORY_KEEP)
        }
    }

    /** Cascade cleanup when media files are deleted or trashed. */
    suspend fun removePaths(paths: Collection<String>) {
        if (paths.isEmpty()) return
        historyDao.deleteByPaths(paths.toList())
    }

    /** Total plays per artist, from the play history window. */
    suspend fun artistPlayCounts(): Map<String, Long> =
        historyDao.playCountsByArtist().associate { it.artist to it.count }

    suspend fun recentPage(limit: Int, offset: Int): List<DAudioPlayHistory> = historyDao.page(limit, offset)

    suspend fun recentPageFiltered(text: String, limit: Int, offset: Int): List<DAudioPlayHistory> =
        historyDao.pageText("%${escapeLike(text)}%", limit, offset)
}
