package com.ismartcoding.plain.ui.audio.playlists.components

import com.ismartcoding.plain.models.Audio as AudioTrack
import com.ismartcoding.plain.features.audio.AudioQueueClient
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.media.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel

internal suspend fun playPlaylistFrom(playlistId: String, startMediaId: String?, audioQueueVM: AudioQueueViewModel) {
    val start = withIO { run {
        audioQueueVM.source.queue.clearQueue()
        audioQueueVM.source.queue.add("playlist_id:$playlistId")
        val items = audioQueueVM.source.queue.queuePage(0, Int.MAX_VALUE)
        items.firstOrNull { it.mediaId == startMediaId } ?: items.firstOrNull()
    } }
    if (start != null) {
        audioJustPlayWithNotificationCheck(start, audioQueueVM.source)
        audioQueueVM.onStarted(start)
    }
}

internal suspend fun playPlaylistShuffled(playlistId: String, audioQueueVM: AudioQueueViewModel) {
    withIO { audioQueueVM.source.queue.clearQueue(); audioQueueVM.source.queue.add("playlist_id:$playlistId") }
    val next = withIO { audioQueueVM.source.queue.resolveNext(isNext = true, shuffle = true) }
    if (next != null) {
        audioJustPlayWithNotificationCheck(next, audioQueueVM.source)
        audioQueueVM.onStarted(next)
    }
}

internal fun AudioItem.toAudioTrack(): AudioTrack = AudioTrack(
    id = requireNotNull(mediaId),
    title = title,
    artist = artist,
    path = path,
    durationMs = durationMs,
    size = 0,
    bucketId = "",
    albumId = "",
    createdAt = TimeHelper.now(),
    updatedAt = TimeHelper.now(),
)
