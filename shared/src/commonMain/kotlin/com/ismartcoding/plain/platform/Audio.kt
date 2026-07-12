package com.ismartcoding.plain.platform

import kotlinx.coroutines.flow.StateFlow

/** Flow of audio playback playing state. */
expect fun audioIsPlayingFlow(): StateFlow<Boolean>

/** Current audio playback position in milliseconds. */
expect fun audioPlayerProgress(): Long

/** Seek the audio player to [progress] (milliseconds). */
expect fun audioSeekTo(progress: Long)

/** Pause audio playback. */
expect fun audioPause()

/** Resume audio playback. */
expect fun audioPlay()

/** Play an audio file from the given [path]. */
expect fun playAudioFromPath(path: String)

/**
 * Plays an audio file after ensuring notification permission is granted
 * (no-op if permission is denied).
 */
expect fun playAudioWithNotificationCheck(path: String)
