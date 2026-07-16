package com.ismartcoding.plain.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
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

/**
 * Restart audio playback if it is currently playing. Used when a permission
 * change (e.g. POST_NOTIFICATIONS) requires restarting the foreground
 * playback service. No-op if audio is not playing or on platforms without
 * a media player service.
 */
expect fun restartAudioIfPlaying()

/** Play an audio file from the given [path]. */
expect fun playAudioFromPath(path: String)

/**
 * Plays an audio file after ensuring notification permission is granted
 * (no-op if permission is denied).
 */
expect fun playAudioWithNotificationCheck(path: String)

/**
 * Returns the title and artist for the audio file at [path].
 */
expect suspend fun getAudioMetadata(path: String): Pair<String, String>

/**
 * Builds a [DPlaylistAudio] from the audio file at [path] by extracting metadata
 * (title, artist, duration) via the platform media retriever.
 */
expect fun playlistAudioFromPath(path: String): DPlaylistAudio

/**
 * Start playback of [audio] immediately without altering the persisted playlist.
 * (Wraps `AudioPlayer.justPlay` on Android.)
 */
expect fun audioJustPlay(audio: DPlaylistAudio)

/**
 * Plays [audio] immediately without altering the persisted playlist, after ensuring
 * notification permission is granted (no-op if permission is denied).
 * (Wraps `Permissions.checkNotification` + `AudioPlayer.justPlay` on Android.)
 */
expect fun audioJustPlayWithNotificationCheck(audio: DPlaylistAudio)

/**
 * Stop playback and clear the player's media items. (Wraps `AudioPlayer.clear`.)
 */
expect fun audioClear()

/** Skip to the previous audio in the playlist. (Wraps `AudioPlayer.skipToPrevious`.) */
expect fun audioSkipToPrevious()

/** Skip to the next audio in the playlist. (Wraps `AudioPlayer.skipToNext`.) */
expect fun audioSkipToNext()

/**
 * Loads the embedded cover art for the audio file at [path] as an [ImageBitmap],
 * or null if the file has no embedded picture or loading fails.
 */
expect fun loadAudioCoverBitmap(path: String): ImageBitmap?

/**
 * Returns the file-id string used to retrieve the album art for [audio] via the
 * HTTP server. Empty string on platforms without a media-store album art provider.
 */
expect fun getAudioAlbumArtFileId(audio: DAudio): String
