package com.ismartcoding.plain.ui.components.mediaviewer.video

import android.content.Context
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.ismartcoding.plain.lib.extensions.pathToUri
import java.util.UUID

/**
 * Android implementation of [VideoPlayerController] backed by ExoPlayer.
 */
@OptIn(UnstableApi::class)
class ExoPlayerVideoController(
    val exoPlayer: ExoPlayer,
    context: Context,
) : VideoPlayerController {

    private val focusManager = VideoAudioFocusManager(
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
    )

    private var eventListener: ((VideoPlayerEvent) -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val listener = eventListener ?: return
            listener(
                VideoPlayerEvent.StateChanged(
                    isPlaying = player.isPlaying,
                    duration = player.duration.coerceAtLeast(0L),
                ),
            )
            if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
                listener(VideoPlayerEvent.PositionDiscontinuity)
            }
            if (events.contains(Player.EVENT_RENDERED_FIRST_FRAME)) {
                listener(VideoPlayerEvent.FirstFrameRendered)
            }
        }
    }

    private var mediaSession: MediaSession? = null

    init {
        exoPlayer.addListener(playerListener)
        mediaSession = try {
            MediaSession.Builder(
                context.applicationContext,
                ForwardingPlayer(exoPlayer),
            ).setId("VideoPlayerMediaSession_" + UUID.randomUUID().toString().lowercase().split("-").first())
                .build()
        } catch (e: Throwable) {
            null
        }
    }

    override fun play() = exoPlayer.play()
    override fun pause() = exoPlayer.pause()
    override fun stop() = exoPlayer.stop()
    override fun prepare() = exoPlayer.prepare()
    override fun seekTo(positionMs: Long) = exoPlayer.seekTo(positionMs)
    override fun setPlaybackSpeed(speed: Float) = exoPlayer.setPlaybackSpeed(speed)
    override fun setMuted(muted: Boolean) {
        exoPlayer.volume = if (muted) 0f else 1f
    }

    override fun release() {
        mediaSession?.release()
        mediaSession = null
        focusManager.abandonFocus()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    override fun setMediaItem(path: String) {
        exoPlayer.setMediaItem(MediaItem.fromUri(path.pathToUri()))
    }

    override fun setEventListener(listener: (VideoPlayerEvent) -> Unit) {
        eventListener = listener
    }

    override fun requestAudioFocus() = focusManager.requestFocus(exoPlayer)
    override fun abandonAudioFocus() = focusManager.abandonFocus()

    override val duration: Long get() = exoPlayer.duration.coerceAtLeast(0L)
    override val currentPosition: Long get() = exoPlayer.currentPosition.coerceAtLeast(0L)
    override val bufferedPercentage: Int get() = exoPlayer.bufferedPercentage
    override val isPlaying: Boolean get() = exoPlayer.isPlaying
}
