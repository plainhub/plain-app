package com.ismartcoding.plain.ui.page.playlist.components

import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.db.DAudioPlaylistItem
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.ui.models.AudioQueueViewModel

/** Point the queue at this playlist starting from [startPath] (null = first). */
internal suspend fun playPlaylistFrom(playlistId: String, startPath: String?, audioQueueVM: AudioQueueViewModel) {
    val start = withIO { AudioQueueManager.setPlaylistSource(playlistId, startPath) }
    if (start != null) {
        audioJustPlayWithNotificationCheck(start)
        audioQueueVM.onStarted(start)
    }
}

/** Start the playlist from a shuffled pick instead of the top. */
internal suspend fun playPlaylistShuffled(playlistId: String, audioQueueVM: AudioQueueViewModel) {
    withIO { AudioQueueManager.setPlaylistSource(playlistId, null) }
    val next = withIO { AudioQueueManager.resolveNext(isNext = true, shuffle = true) }
    if (next != null) {
        audioJustPlayWithNotificationCheck(next)
        audioQueueVM.onStarted(next)
    }
}

/** Off-library fallback row value so selection records one id space. */
internal fun DAudioPlaylistItem.toDAudio(): DAudio = DAudio(
    id = audioPath,
    title = title,
    artist = artist,
    path = audioPath,
    duration = duration,
    size = 0,
    bucketId = "",
    albumId = "",
    createdAt = TimeHelper.now(),
    updatedAt = TimeHelper.now(),
)
